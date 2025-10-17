package edu.trincoll.service;

import edu.trincoll.model.Book;
import edu.trincoll.model.BookStatus;
import edu.trincoll.model.Member;
import edu.trincoll.model.MembershipType;
import edu.trincoll.repository.BookRepository;
import edu.trincoll.repository.MemberRepository;
import edu.trincoll.service.fee.LateFeeCalculator;
import edu.trincoll.service.fee.LateFeeCalculatorFactory;
import edu.trincoll.service.fee.PremiumLateFeeCalculator;
import edu.trincoll.service.fee.RegularLateFeeCalculator;
import edu.trincoll.service.notification.NotificationService;
import edu.trincoll.service.policy.CheckoutPolicyFactory;
import edu.trincoll.service.policy.PremiumCheckoutPolicy;
import edu.trincoll.service.policy.RegularCheckoutPolicy;
import edu.trincoll.service.report.AvailabilityReportGenerator;
import edu.trincoll.service.report.OverdueReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Library Service Tests")
class LibraryServiceTest {

    @Mock private BookRepository bookRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private BookService bookService;
    @Mock private MemberService memberService;
    @Mock private CheckoutPolicyFactory checkoutPolicyFactory;
    @Mock private LateFeeCalculatorFactory lateFeeCalculatorFactory;
    @Mock private NotificationService notificationService;

    private LibraryService libraryService;

    private Book availableBook;
    private Member regularMember;
    private Member premiumMember;
    private Member studentMember;

    @BeforeEach
    void setUp() {
        libraryService = new LibraryService(
                bookService, memberService, checkoutPolicyFactory,
                bookRepository, memberRepository, lateFeeCalculatorFactory, notificationService);

        availableBook = new Book("978-0-123456-78-9", "Clean Code", "Robert Martin",
                LocalDate.of(2008, 8, 1));
        availableBook.setId(1L);
        availableBook.setStatus(BookStatus.AVAILABLE);

        regularMember = new Member("John Doe", "john@example.com");
        regularMember.setId(1L);
        regularMember.setMembershipType(MembershipType.REGULAR);
        regularMember.setBooksCheckedOut(0);

        premiumMember = new Member("Jane Smith", "jane@example.com");
        premiumMember.setId(2L);
        premiumMember.setMembershipType(MembershipType.PREMIUM);
        premiumMember.setBooksCheckedOut(0);

        studentMember = new Member("Bob Student", "bob@example.com");
        studentMember.setId(3L);
        studentMember.setMembershipType(MembershipType.STUDENT);
        studentMember.setBooksCheckedOut(0);
    }

    @Test
    @DisplayName("Should checkout book successfully for regular member")
    void shouldCheckoutBookForRegularMember() {
        doAnswer(invocation -> {
            Book b = invocation.getArgument(0);
            b.setDueDate(LocalDate.now().plusDays(14));
            return null;
        }).when(bookService).checkoutBook(any(Book.class), any(Member.class), anyInt());

        when(bookService.findByIsbn(availableBook.getIsbn())).thenReturn(availableBook);
        when(memberService.findByEmail(regularMember.getEmail())).thenReturn(regularMember);
        when(checkoutPolicyFactory.getPolicyFor(MembershipType.REGULAR)).thenReturn(new RegularCheckoutPolicy());

        String result = libraryService.checkoutBook(availableBook.getIsbn(), regularMember.getEmail());

        assertThat(result).contains("Book checked out successfully");
        assertThat(result).contains("Due date:");
        verify(bookService).checkoutBook(eq(availableBook), eq(regularMember), eq(14));
        verify(memberService).incrementCheckoutCount(eq(regularMember));
        verify(notificationService).sendCheckoutNotification(eq(regularMember), eq(availableBook), any(LocalDate.class));
    }

    @Test
    @DisplayName("Should apply correct loan period for premium member")
    void shouldApplyPremiumLoanPeriod() {
        when(bookService.findByIsbn(availableBook.getIsbn())).thenReturn(availableBook);
        when(memberService.findByEmail(premiumMember.getEmail())).thenReturn(premiumMember);
        when(checkoutPolicyFactory.getPolicyFor(MembershipType.PREMIUM)).thenReturn(new PremiumCheckoutPolicy());

        libraryService.checkoutBook(availableBook.getIsbn(), premiumMember.getEmail());

        verify(bookService).checkoutBook(any(Book.class), any(Member.class), eq(30));
    }

    @Test
    @DisplayName("Should enforce checkout limit for regular member")
    void shouldEnforceCheckoutLimitForRegularMember() {
        regularMember.setBooksCheckedOut(3); // At limit
        when(bookService.findByIsbn(availableBook.getIsbn())).thenReturn(availableBook);
        when(memberService.findByEmail(regularMember.getEmail())).thenReturn(regularMember);
        when(checkoutPolicyFactory.getPolicyFor(MembershipType.REGULAR)).thenReturn(new RegularCheckoutPolicy());

        String result = libraryService.checkoutBook(availableBook.getIsbn(), regularMember.getEmail());

        assertThat(result).isEqualTo("Member has reached checkout limit");
        verify(bookService, never()).checkoutBook(any(), any(), anyInt());
        verify(memberService, never()).incrementCheckoutCount(any());
    }

    @Test
    @DisplayName("Should not checkout unavailable book")
    void shouldNotCheckoutUnavailableBook() {
        availableBook.setStatus(BookStatus.CHECKED_OUT);
        when(bookService.findByIsbn(availableBook.getIsbn())).thenReturn(availableBook);
        when(memberService.findByEmail(regularMember.getEmail())).thenReturn(regularMember);

        String result = libraryService.checkoutBook(availableBook.getIsbn(), regularMember.getEmail());

        assertThat(result).isEqualTo("Book is not available");
        verify(bookService, never()).checkoutBook(any(), any(), anyInt());
        verify(memberService, never()).incrementCheckoutCount(any());
    }

    @Test
    @DisplayName("Should throw exception when book not found")
    void shouldThrowExceptionWhenBookNotFound() {
        when(bookService.findByIsbn(anyString())).thenThrow(new IllegalArgumentException("Book not found"));

        assertThatThrownBy(() -> libraryService.checkoutBook("invalid-isbn", regularMember.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Book not found");
    }

    @Test
    @DisplayName("Should return book successfully")
    void shouldReturnBookSuccessfully() {
        availableBook.setStatus(BookStatus.CHECKED_OUT);
        availableBook.setCheckedOutBy(regularMember.getEmail());
        availableBook.setDueDate(LocalDate.now().plusDays(7));

        when(bookService.findByIsbn(availableBook.getIsbn())).thenReturn(availableBook);
        when(memberService.findByEmail(regularMember.getEmail())).thenReturn(regularMember);

        regularMember.setBooksCheckedOut(1);

        String result = libraryService.returnBook(availableBook.getIsbn());

        assertThat(result).isEqualTo("Book returned successfully");
        verify(bookService).returnBook(eq(availableBook));
        verify(memberService).decrementCheckoutCount(eq(regularMember));
        verify(notificationService).sendReturnNotification(eq(regularMember), eq(availableBook), eq(0.0));
    }

    @Test
    @DisplayName("Should calculate late fee for regular member")
    void shouldCalculateLateFeeForRegularMember() {
        availableBook.setStatus(BookStatus.CHECKED_OUT);
        availableBook.setCheckedOutBy(regularMember.getEmail());
        availableBook.setDueDate(LocalDate.now().minusDays(5)); // 5 days late

        when(bookService.findByIsbn(availableBook.getIsbn())).thenReturn(availableBook);
        when(memberService.findByEmail(regularMember.getEmail())).thenReturn(regularMember);
        when(lateFeeCalculatorFactory.getCalculatorFor(MembershipType.REGULAR)).thenReturn(new RegularLateFeeCalculator());

        regularMember.setBooksCheckedOut(1);

        String result = libraryService.returnBook(availableBook.getIsbn());

        assertThat(result).contains("Late fee: $2.50"); // 5 days * $0.50
        verify(notificationService).sendReturnNotification(eq(regularMember), eq(availableBook), eq(2.5));
    }

    @Test
    @DisplayName("Should not charge late fee for premium member")
    void shouldNotChargeLateFeeForPremiumMember() {
        availableBook.setStatus(BookStatus.CHECKED_OUT);
        availableBook.setCheckedOutBy(premiumMember.getEmail());
        availableBook.setDueDate(LocalDate.now().minusDays(5)); // 5 days late

        when(bookService.findByIsbn(availableBook.getIsbn())).thenReturn(availableBook);
        when(memberService.findByEmail(premiumMember.getEmail())).thenReturn(premiumMember);
        when(lateFeeCalculatorFactory.getCalculatorFor(MembershipType.PREMIUM)).thenReturn(new PremiumLateFeeCalculator());

        premiumMember.setBooksCheckedOut(1);

        String result = libraryService.returnBook(availableBook.getIsbn());

        assertThat(result).isEqualTo("Book returned successfully");
        assertThat(result).doesNotContain("Late fee");

        verify(bookService).returnBook(eq(availableBook));
        verify(memberService).decrementCheckoutCount(eq(premiumMember));
    }

    @Test
    @DisplayName("Should search books by title")
    void shouldSearchBooksByTitle() {
        when(bookRepository.findByTitleContainingIgnoreCase("Clean"))
                .thenReturn(java.util.List.of(availableBook));

        var results = libraryService.searchBooks("Clean", "title");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("Should throw exception for invalid search type")
    void shouldThrowExceptionForInvalidSearchType() {
        assertThatThrownBy(() -> libraryService.searchBooks("test", "invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid search type");
    }

    @Test
    void searchByTitle_trimsAndDelegates() {
        var repo = mock(BookRepository.class);
        var svc  = new BookSearchService(repo);

        when(repo.findByTitleContainingIgnoreCase("dune")).thenReturn(List.of(new Book()));

        var result = svc.searchByTitle("  dune  ");
        assertThat(result).hasSize(1);
        verify(repo).findByTitleContainingIgnoreCase("dune");
    }

    @Test
    void searchByTitle_blankReturnsEmpty() {
        var svc = new BookSearchService(mock(BookRepository.class));
        assertThat(svc.searchByTitle("  ")).isEmpty();
    }

    @Test
    void searchByAuthor_trimsAndDelegates() {
        var repo = mock(BookRepository.class);
        var svc  = new BookSearchService(repo);

        when(repo.findByAuthor("Le Guin")).thenReturn(List.of(new Book()));

        var result = svc.searchByAuthor("  Le Guin ");
        assertThat(result).hasSize(1);
        verify(repo).findByAuthor("Le Guin");
    }

    @Test
    void searchByIsbn_trimsAndDelegates() {
        var repo = mock(BookRepository.class);
        var svc  = new BookSearchService(repo);

        when(repo.findByIsbn("9780143111580")).thenReturn(Optional.of(new Book()));

        var result = svc.searchByIsbn(" 9780143111580 ");
        assertThat(result).isPresent();
        verify(repo).findByIsbn("9780143111580");
    }

    @Test
    void searchByIsbn_blankReturnsEmptyOptional() {
        var svc = new BookSearchService(mock(BookRepository.class));
        assertThat(svc.searchByIsbn(" ")).isEmpty();
    }

    @Test
    void noAvailableBooks_returnsFriendlyMessage() {
        BookRepository repo = mock(BookRepository.class);
        when(repo.findAll()).thenReturn(List.of(
                mkBook("A", "Auth", "111", BookStatus.CHECKED_OUT, null, "x")
        ));

        AvailabilityReportGenerator gen = new AvailabilityReportGenerator(repo);
        String report = gen.generateReport();

        assertTrue(report.startsWith("Available Books Report"));
        assertTrue(report.contains("No books are currently available."));
        verify(repo, times(1)).findAll();
    }

    @Test
    void availableBooks_sortedCaseInsensitiveByTitle() {
        BookRepository repo = mock(BookRepository.class);
        when(repo.findAll()).thenReturn(List.of(
                mkBook("charlie", "C", "333", BookStatus.AVAILABLE, null, null),
                mkBook("Bravo", "B", "222", BookStatus.AVAILABLE, null, null),
                mkBook("alpha", "A", "111", BookStatus.AVAILABLE, null, null),
                mkBook("zzz", "Z", "999", BookStatus.CHECKED_OUT, null, "me") // filtered out
        ));

        AvailabilityReportGenerator gen = new AvailabilityReportGenerator(repo);
        String report = gen.generateReport();

        assertTrue(report.contains("Total: 3"));

        int iAlpha   = report.indexOf("- alpha by A (ISBN: 111)");
        int iBravo   = report.indexOf("- Bravo by B (ISBN: 222)");
        int iCharlie = report.indexOf("- charlie by C (ISBN: 333)");

        assertTrue(iAlpha >= 0 && iBravo >= 0 && iCharlie >= 0);
        assertTrue(iAlpha < iBravo && iBravo < iCharlie);

        verify(repo, times(1)).findAll();
    }

    private static Book mkBook(String title, String author, String isbn,
                               BookStatus status, java.time.LocalDate due, String who) {
        Book b = new Book();
        b.setTitle(title);
        b.setAuthor(author);
        b.setIsbn(isbn);
        b.setStatus(status);
        b.setDueDate(due);
        b.setCheckedOutBy(who);
        return b;
    }

    @Test
    void noOverdueBooks_messageIncludesToday() {
        BookRepository repo = mock(BookRepository.class);
        LocalDate today = LocalDate.now();

        when(repo.findAll()).thenReturn(List.of(
                mkBook("Avail", "A", "100", BookStatus.AVAILABLE, null, null),
                mkBook("DueToday", "B", "101", BookStatus.CHECKED_OUT, today, "x"),
                mkBook("Future", "C", "102", BookStatus.CHECKED_OUT, today.plusDays(3), "y")
        ));

        OverdueReportGenerator gen = new OverdueReportGenerator(repo);
        String report = gen.generateReport();

        assertTrue(report.startsWith("Overdue Books Report"));
        assertTrue(report.contains("No overdue books as of " + today));
        verify(repo, times(1)).findAll();
    }

    @Test
    void overdueBooks_sortedByOldestDueThenTitle_andShowsDaysLateAndBorrower() {
        BookRepository repo = mock(BookRepository.class);
        LocalDate today = LocalDate.now();

        var b1 = mkBook("Zoo",   "Z", "901", BookStatus.CHECKED_OUT, today.minusDays(10), "z@example.com");
        var b2 = mkBook("alpha", "A", "902", BookStatus.CHECKED_OUT, today.minusDays(10), "a@example.com");
        var b3 = mkBook("mid",   "M", "903", BookStatus.CHECKED_OUT, today.minusDays(5),  "m@example.com");
        var b4 = mkBook("new",   "N", "904", BookStatus.CHECKED_OUT, today.plusDays(1),   "n@example.com"); // not overdue

        when(repo.findAll()).thenReturn(List.of(b1, b2, b3, b4));

        OverdueReportGenerator gen = new OverdueReportGenerator(repo);
        String report = gen.generateReport();

        assertTrue(report.contains("Total Overdue: 3"));
        assertTrue(report.contains("alpha"));
        assertTrue(report.contains("Zoo"));
        assertTrue(report.contains("mid"));
        assertTrue(report.contains("10 days late"));
        assertTrue(report.contains("5 days late"));

        int iAlpha = report.indexOf("- alpha");
        int iZoo   = report.indexOf("- Zoo");
        int iMid   = report.indexOf("- mid");
        assertTrue(iAlpha < iZoo && iZoo < iMid);

        assertTrue(report.contains("Borrower: a@example.com"));
        assertTrue(report.contains("Borrower: z@example.com"));
        assertTrue(report.contains("Borrower: m@example.com"));

        verify(repo, times(1)).findAll();
    }

    @Test
    void generateAvailabilityReport_delegatesToAvailabilityGenerator() {
        var availability = mock(AvailabilityReportGenerator.class);
        var overdue      = mock(OverdueReportGenerator.class);

        when(availability.generateReport()).thenReturn("avail-report");

        var facade = new LibraryFacade(availability, overdue);
        String out = facade.generateAvailabilityReport();

        assertEquals("avail-report", out);
        verify(availability, times(1)).generateReport();
        verifyNoInteractions(overdue);
    }

    @Test
    void generateOverdueReport_delegatesToOverdueGenerator() {
        var availability = mock(AvailabilityReportGenerator.class);
        var overdue      = mock(OverdueReportGenerator.class);

        when(overdue.generateReport()).thenReturn("overdue-report");

        var facade = new LibraryFacade(availability, overdue);
        String out = facade.generateOverdueReport();

        assertEquals("overdue-report", out);
        verify(overdue, times(1)).generateReport();
        verifyNoInteractions(availability);
    }

    @Test
    void incrementCheckoutCount_increasesAndSaves() {
        MemberRepository repo = mock(MemberRepository.class);
        MemberService svc = new MemberService(repo);

        Member m = new Member("John", "john@example.com");
        m.setBooksCheckedOut(2);

        svc.incrementCheckoutCount(m);

        assertThat(m.getBooksCheckedOut()).isEqualTo(3);
        verify(repo).save(m);
    }


    @Test
    void findByEmail_returnsMember_whenPresent() {
        MemberRepository repo = mock(MemberRepository.class);
        MemberService svc = new MemberService(repo);

        Member m = new Member("Jane", "jane@example.com");
        when(repo.findByEmail("jane@example.com")).thenReturn(Optional.of(m));

        Member out = svc.findByEmail("jane@example.com");

        assertThat(out).isSameAs(m);
        verify(repo).findByEmail("jane@example.com");
    }

    @Test
    void regularCalculator_multipliesByHalfDollar() {
        LateFeeCalculator calc = new RegularLateFeeCalculator();
        assertThat(calc.calculateLateFee(0)).isEqualTo(0.0);
        assertThat(calc.calculateLateFee(1)).isEqualTo(0.50);
        assertThat(calc.calculateLateFee(5)).isEqualTo(2.50);
    }

    @Test
    void premiumCalculator_isAlwaysZero() {
        LateFeeCalculator calc = new PremiumLateFeeCalculator();
        assertThat(calc.calculateLateFee(0)).isEqualTo(0.0);
        assertThat(calc.calculateLateFee(7)).isEqualTo(0.0);
    }
}
