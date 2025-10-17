package edu.trincoll.service;

import edu.trincoll.model.Book;
import edu.trincoll.model.BookStatus;
import edu.trincoll.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test: loads the Spring context, uses real H2 DB and real beans.
 * Verifies service coordination via the Facade and data flowing from JPA layer.
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.ANY) // use in-memory H2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class LibraryFacadeSpringIT {

    @Autowired private LibraryFacade facade;
    @Autowired private BookRepository bookRepository;

    @BeforeEach
    void seedData() {
        bookRepository.deleteAll();

        // Available books
        bookRepository.save(new Book("111", "alpha", "A", LocalDate.of(2000,1,1)));
        bookRepository.save(new Book("222", "Bravo", "B", LocalDate.of(2001,1,1)));

        // Overdue (checked out, due in the past)
        Book overdueOldest = new Book("901", "Zoo", "Z", LocalDate.of(2002,1,1));
        overdueOldest.setStatus(BookStatus.CHECKED_OUT);
        overdueOldest.setCheckedOutBy("z@example.com");
        overdueOldest.setDueDate(LocalDate.now().minusDays(10));
        bookRepository.save(overdueOldest);

        Book overdueTie = new Book("902", "alpha", "A", LocalDate.of(2002,1,1));
        overdueTie.setStatus(BookStatus.CHECKED_OUT);
        overdueTie.setCheckedOutBy("a@example.com");
        overdueTie.setDueDate(LocalDate.now().minusDays(10));
        bookRepository.save(overdueTie);

        // Not overdue (future due)
        Book notOverdue = new Book("903", "mid", "M", LocalDate.of(2002,1,1));
        notOverdue.setStatus(BookStatus.CHECKED_OUT);
        notOverdue.setCheckedOutBy("m@example.com");
        notOverdue.setDueDate(LocalDate.now().plusDays(3));
        bookRepository.save(notOverdue);
    }

    @Test
    void availabilityReport_includesOnlyAvailable_sortedByTitle() {
        String report = facade.generateAvailabilityReport();

        assertThat(report).startsWith("Available Books Report");
        assertThat(report).contains("Total: 2");
        int iAlpha = report.indexOf("- alpha by A (ISBN: 111)");
        int iBravo = report.indexOf("- Bravo by B (ISBN: 222)");
        assertThat(iAlpha).isGreaterThanOrEqualTo(0);
        assertThat(iBravo).isGreaterThanOrEqualTo(0);
        assertThat(iAlpha).isLessThan(iBravo); // alpha before Bravo
    }

    @Test
    void overdueReport_includesOnlyOverdue_sortedByDueDateThenTitle() {
        String report = facade.generateOverdueReport();

        assertThat(report).startsWith("Overdue Books Report");
        assertThat(report).contains("Total Overdue: 2"); // only the past-due ones

        int iAlpha = report.indexOf("- alpha by A (ISBN: 902)");
        int iZoo   = report.indexOf("- Zoo by Z (ISBN: 901)");

        // Same oldest due date -> alpha comes before Zoo (tie-break on title)
        assertThat(iAlpha).isGreaterThanOrEqualTo(0);
        assertThat(iZoo).isGreaterThanOrEqualTo(0);
        assertThat(iAlpha).isLessThan(iZoo);

        // Contains days-late and borrower fields
        assertThat(report).contains("10 days late");
        assertThat(report).contains("Borrower: a@example.com");
        assertThat(report).contains("Borrower: z@example.com");
    }
}
