package edu.trincoll.service.report;

import edu.trincoll.model.Book;
import edu.trincoll.model.BookStatus;
import edu.trincoll.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Report listing books that are CHECKED_OUT and past due as of today.
 */
@Service
public class OverdueReportGenerator implements ReportGenerator {

    private final BookRepository bookRepository;

    public OverdueReportGenerator(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public String generateReport() {
        LocalDate today = LocalDate.now();

        List<Book> overdue = bookRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookStatus.CHECKED_OUT)
                .filter(b -> b.getDueDate() != null && b.getDueDate().isBefore(today))
                .sorted(Comparator.comparing(Book::getDueDate) // oldest due first
                        .thenComparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (overdue.isEmpty()) {
            return "Overdue Books Report\n---------------------\nNo overdue books as of " + today + ".";
        }

        String rows = overdue.stream()
                .map(b -> {
                    long daysLate = ChronoUnit.DAYS.between(b.getDueDate(), today);
                    String borrower = b.getCheckedOutBy() == null ? "(unknown borrower)" : b.getCheckedOutBy();
                    return "- " + safe(b.getTitle()) + " by " + safe(b.getAuthor()) +
                            " (ISBN: " + safe(b.getIsbn()) + ")\n" +
                            "  Due: " + b.getDueDate() + "  (" + daysLate + " days late)\n" +
                            "  Borrower: " + borrower;
                })
                .collect(Collectors.joining("\n\n"));

        return "Overdue Books Report\n---------------------\n" +
                "As of: " + today + "\n" +
                "Total Overdue: " + overdue.size() + "\n\n" + rows + "\n";
    }

    private static String safe(String s) {
        return s == null ? "(unknown)" : s;
    }
}
