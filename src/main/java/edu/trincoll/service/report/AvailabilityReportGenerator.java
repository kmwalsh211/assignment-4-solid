package edu.trincoll.service.report;

import edu.trincoll.model.Book;
import edu.trincoll.model.BookStatus;
import edu.trincoll.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Report listing books currently AVAILABLE.
 */
@Service
public class AvailabilityReportGenerator implements ReportGenerator {

    private final BookRepository bookRepository;

    public AvailabilityReportGenerator(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public String generateReport() {
        List<Book> available = bookRepository.findAll().stream()
                .filter(b -> b.getStatus() == BookStatus.AVAILABLE)
                .sorted(Comparator.comparing(Book::getTitle, String.CASE_INSENSITIVE_ORDER))
                .toList();

        if (available.isEmpty()) {
            return "Available Books Report\n-----------------------\nNo books are currently available.";
        }

        String rows = available.stream()
                .map(b -> "- " + safe(b.getTitle()) + " by " + safe(b.getAuthor()) +
                        " (ISBN: " + safe(b.getIsbn()) + ")")
                .collect(Collectors.joining("\n"));

        return "Available Books Report\n-----------------------\n" +
                "Total: " + available.size() + "\n\n" + rows + "\n";
    }

    private static String safe(String s) {
        return s == null ? "(unknown)" : s;
    }
}
