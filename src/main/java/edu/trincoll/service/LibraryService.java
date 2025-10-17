package edu.trincoll.service;

import edu.trincoll.model.Book;
import edu.trincoll.model.BookStatus;
import edu.trincoll.model.Member;
import edu.trincoll.model.MembershipType;
import edu.trincoll.repository.BookRepository;
import edu.trincoll.repository.MemberRepository;
import edu.trincoll.service.policy.CheckoutPolicy;
import edu.trincoll.service.fee.LateFeeCalculator;
import edu.trincoll.service.fee.LateFeeCalculatorFactory;
import edu.trincoll.service.policy.CheckoutPolicyFactory;
import edu.trincoll.service.notification.NotificationService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * SOLID VIOLATIONS TO FIX:
 *
 * This service violates multiple SOLID principles. Your task is to refactor it
 * following the TODOs below. Each TODO is worth points based on which SOLID
 * principle(s) it addresses.
 *
 * Current violations:
 * - SRP: This class has too many responsibilities
 * - OCP: Adding new membership types requires modifying existing code
 * - DIP: Direct database access and business logic are mixed
 * - ISP: Would create fat interfaces if extracted
 */
@Service
public class LibraryService {

    private final BookService bookService;
    private final MemberService memberService;
    private final CheckoutPolicyFactory checkoutPolicyFactory;
    private final LateFeeCalculatorFactory lateFeeCalculatorFactory;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    public LibraryService(BookService bookService, MemberService memberService, CheckoutPolicyFactory checkoutPolicyFactory, BookRepository bookRepository, MemberRepository memberRepository, LateFeeCalculatorFactory lateFeeCalculatorFactory, NotificationService notificationService) {
        this.bookService = bookService;
        this.memberService = memberService;
        this.checkoutPolicyFactory = checkoutPolicyFactory;
        this.bookRepository = bookRepository;
        this.memberRepository = memberRepository;
        this.lateFeeCalculatorFactory = lateFeeCalculatorFactory;
        this.notificationService = notificationService;
    }

    // TODO 1 (15 points): SRP Violation - This method has multiple responsibilities - KAYLA: DONE
    // Extract book-specific operations to a separate BookService
    // Move member-specific operations to a separate MemberService
    public String checkoutBook(String isbn, String memberEmail) {
        // Find book
        Book book = bookService.findByIsbn(isbn);

        // Find member
        Member member = memberService.findByEmail(memberEmail);

        // Check if book is available
        if (book.getStatus() != BookStatus.AVAILABLE) {
            return "Book is not available";
        }


        // TODO 2 (15 points): OCP Violation - This checkout limit logic violates Open-Closed Principle - KAYLA: DONE
        // Create a CheckoutPolicy interface with different implementations for each membership type
        // Use Strategy pattern instead of if-else statements
        CheckoutPolicy policy = checkoutPolicyFactory.getPolicyFor(member.getMembershipType());

        if (!policy.canCheckout(member)) {
            return "Member has reached checkout limit";
        }

        // Update book status
        bookService.checkoutBook(book, member, policy.getLoanPeriodDays());

        // Update member
        memberService.incrementCheckoutCount(member);

        // TODO 3 (10 points): SRP Violation - Notification logic should be separate - KAYLA: DONE
        // Create a NotificationService interface with email implementation
        // This demonstrates DIP (depend on abstraction, not concrete email sending)
        notificationService.sendCheckoutNotification(member, book, book.getDueDate());

        return "Book checked out successfully. Due date: " + book.getDueDate();
    }

    // TODO 4 (15 points): SRP Violation - Return book logic should be in BookService - KAYLA: DONE
    // Also contains duplicated notification logic (DRY violation)
    public String returnBook(String isbn) {
        Book book = bookService.findByIsbn(isbn);

        if (book.getStatus() != BookStatus.CHECKED_OUT) {
            return "Book is not checked out";
        }

        String memberEmail = book.getCheckedOutBy();
        Member member = memberService.findByEmail(memberEmail);

        // TODO 5 (10 points): OCP & SRP Violation - Late fee calculation - AJ: DONE
        // Create a LateFeeCalculator interface with strategy implementations
        // Different membership types might have different fee structures
        double lateFee = 0.0;
        if (book.getDueDate().isBefore(LocalDate.now())) {
            long daysLate = LocalDate.now().toEpochDay() - book.getDueDate().toEpochDay();
            LateFeeCalculator calculator = lateFeeCalculatorFactory.getCalculatorFor(member.getMembershipType());
            lateFee = calculator.calculateLateFee(daysLate);
        }

        // Update book
        bookService.returnBook(book);

        // Update member
        memberService.decrementCheckoutCount(member);

        // Duplicated notification code (should use NotificationService)
        notificationService.sendReturnNotification(member, book, lateFee);

        if (lateFee > 0) {
            return "Book returned. Late fee: $" + String.format("%.2f", lateFee);
        }

        return "Book returned successfully";
    }

    // TODO 6 (10 points): SRP Violation - Search/query operations
    // Create a BookSearchService with different search strategies
    // This also demonstrates ISP - clients shouldn't depend on unused search methods
    public List<Book> searchBooks(String searchTerm, String searchType) {
        if ("title".equalsIgnoreCase(searchType)) {
            return bookRepository.findByTitleContainingIgnoreCase(searchTerm);
        } else if ("author".equalsIgnoreCase(searchType)) {
            return bookRepository.findByAuthor(searchTerm);
        } else if ("isbn".equalsIgnoreCase(searchType)) {
            return bookRepository.findByIsbn(searchTerm)
                    .map(List::of)
                    .orElse(List.of());
        } else {
            throw new IllegalArgumentException("Invalid search type");
        }
    }

    // TODO 7 (10 points): LSP & OCP Violation - Report generation
    // Create a ReportGenerator interface with different format implementations
    // This allows adding new report formats without modifying existing code
    public String generateReport(String reportType) {
        if ("overdue".equalsIgnoreCase(reportType)) {
            List<Book> overdueBooks = bookRepository.findByDueDateBefore(LocalDate.now());
            StringBuilder report = new StringBuilder("OVERDUE BOOKS REPORT\n");
            report.append("====================\n");
            for (Book book : overdueBooks) {
                report.append(String.format("%s by %s - Due: %s - Checked out by: %s\n",
                        book.getTitle(), book.getAuthor(), book.getDueDate(), book.getCheckedOutBy()));
            }
            return report.toString();
        } else if ("available".equalsIgnoreCase(reportType)) {
            long availableCount = bookRepository.countByStatus(BookStatus.AVAILABLE);
            return "Available books: " + availableCount;
        } else if ("members".equalsIgnoreCase(reportType)) {
            long totalMembers = memberRepository.count();
            return "Total members: " + totalMembers;
        } else {
            throw new IllegalArgumentException("Invalid report type");
        }
    }

    // TODO 8 (15 points): BONUS - Create a complete refactoring
    // After implementing all TODOs above, demonstrate the refactored architecture:
    // 1. Draw a class diagram showing all services and their dependencies
    // 2. Write integration tests that prove the refactored code works
    // 3. Document which SOLID principles each new class/interface demonstrates
    // 4. Show how the refactoring makes the code more testable with mocks
}
