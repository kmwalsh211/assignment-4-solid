package edu.trincoll.service;

import edu.trincoll.model.Book;
import edu.trincoll.model.BookStatus;
import edu.trincoll.model.Member;
import edu.trincoll.model.MembershipType;
import edu.trincoll.repository.BookRepository;
import edu.trincoll.repository.MemberRepository;
import edu.trincoll.service.fee.LateFeeCalculatorFactory;
import edu.trincoll.service.fee.PremiumLateFeeCalculator;
import edu.trincoll.service.fee.RegularLateFeeCalculator;
import edu.trincoll.service.policy.CheckoutPolicyFactory;
import edu.trincoll.service.policy.PremiumCheckoutPolicy;
import edu.trincoll.service.policy.RegularCheckoutPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Library Service Tests")
class LibraryServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private BookService bookService;

    @Mock
    private MemberService memberService;

    @Mock
    private CheckoutPolicyFactory checkoutPolicyFactory;

    @Mock
    private LateFeeCalculatorFactory lateFeeCalculatorFactory;

    private LibraryService libraryService;

    private Book availableBook;
    private Member regularMember;
    private Member premiumMember;
    private Member studentMember;

    @BeforeEach
    void setUp() {
        libraryService = new LibraryService(bookService, memberService, checkoutPolicyFactory, bookRepository, memberRepository, lateFeeCalculatorFactory);

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
        when(bookService.findByIsbn(availableBook.getIsbn()))
                .thenReturn(availableBook);
        when(memberService.findByEmail(regularMember.getEmail()))
                .thenReturn(regularMember);
        when(checkoutPolicyFactory.getPolicyFor(MembershipType.REGULAR))
                .thenReturn(new RegularCheckoutPolicy());

        // Act
        String result = libraryService.checkoutBook(availableBook.getIsbn(), regularMember.getEmail());

        // Assert
        assertThat(result).contains("Book checked out successfully");
        assertThat(result).contains("Due date:");

        // Verify that the service methods were called with the correct arguments
        verify(bookService).checkoutBook(eq(availableBook), eq(regularMember), eq(14));
        verify(memberService).incrementCheckoutCount(eq(regularMember));
    }

    @Test
    @DisplayName("Should apply correct loan period for premium member")
    void shouldApplyPremiumLoanPeriod() {
        // Arrange
        when(bookService.findByIsbn(availableBook.getIsbn()))
                .thenReturn(availableBook);
        when(memberService.findByEmail(premiumMember.getEmail()))
                .thenReturn(premiumMember);
        when(checkoutPolicyFactory.getPolicyFor(MembershipType.PREMIUM))
                .thenReturn(new PremiumCheckoutPolicy());

        // Act
        libraryService.checkoutBook(availableBook.getIsbn(), premiumMember.getEmail());

        // Assert
        verify(bookService).checkoutBook(any(Book.class), any(Member.class), eq(30));
    }

    @Test
    @DisplayName("Should enforce checkout limit for regular member")
    void shouldEnforceCheckoutLimitForRegularMember() {
        // Arrange
        regularMember.setBooksCheckedOut(3); // At limit
        when(bookService.findByIsbn(availableBook.getIsbn()))
                .thenReturn(availableBook);
        when(memberService.findByEmail(regularMember.getEmail()))
                .thenReturn(regularMember);
        when(checkoutPolicyFactory.getPolicyFor(MembershipType.REGULAR))
                .thenReturn(new RegularCheckoutPolicy());

        // Act
        String result = libraryService.checkoutBook(availableBook.getIsbn(), regularMember.getEmail());

        // Assert
        assertThat(result).isEqualTo("Member has reached checkout limit");
        verify(bookService, never()).checkoutBook(any(), any(), anyInt());
        verify(memberService, never()).incrementCheckoutCount(any());
    }

    @Test
    @DisplayName("Should not checkout unavailable book")
    void shouldNotCheckoutUnavailableBook() {
        // Arrange
        availableBook.setStatus(BookStatus.CHECKED_OUT);
        when(bookService.findByIsbn(availableBook.getIsbn()))
                .thenReturn(availableBook);
        when(memberService.findByEmail(regularMember.getEmail()))
                .thenReturn(regularMember);

        // Act
        String result = libraryService.checkoutBook(availableBook.getIsbn(), regularMember.getEmail());

        // Assert
        assertThat(result).isEqualTo("Book is not available");
        verify(bookService, never()).checkoutBook(any(), any(), anyInt());
        verify(memberService, never()).incrementCheckoutCount(any());
    }

    @Test
    @DisplayName("Should throw exception when book not found")
    void shouldThrowExceptionWhenBookNotFound() {
        // Arrange
        when(bookService.findByIsbn(anyString()))
                .thenThrow(new IllegalArgumentException("Book not found"));

        // Act & Assert
        assertThatThrownBy(() ->
                libraryService.checkoutBook("invalid-isbn", regularMember.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Book not found");
    }

    @Test
    @DisplayName("Should return book successfully")
    void shouldReturnBookSuccessfully() {
        // Arrange
        availableBook.setStatus(BookStatus.CHECKED_OUT);
        availableBook.setCheckedOutBy(regularMember.getEmail());
        availableBook.setDueDate(LocalDate.now().plusDays(7));

        when(bookService.findByIsbn(availableBook.getIsbn()))
                .thenReturn(availableBook);
        when(memberService.findByEmail(regularMember.getEmail()))
                .thenReturn(regularMember);

        regularMember.setBooksCheckedOut(1);

        // Act
        String result = libraryService.returnBook(availableBook.getIsbn());

        // Assert
        assertThat(result).isEqualTo("Book returned successfully");
        verify(bookService).returnBook(eq(availableBook));
        verify(memberService).decrementCheckoutCount(eq(regularMember));
    }

    @Test
    @DisplayName("Should calculate late fee for regular member")
    void shouldCalculateLateFeeForRegularMember() {
        // Arrange
        availableBook.setStatus(BookStatus.CHECKED_OUT);
        availableBook.setCheckedOutBy(regularMember.getEmail());
        availableBook.setDueDate(LocalDate.now().minusDays(5)); // 5 days late

        when(bookService.findByIsbn(availableBook.getIsbn()))
                .thenReturn(availableBook);
        when(memberService.findByEmail(regularMember.getEmail()))
                .thenReturn(regularMember);
        when(lateFeeCalculatorFactory.getCalculatorFor(MembershipType.REGULAR))
                .thenReturn(new RegularLateFeeCalculator());

        regularMember.setBooksCheckedOut(1);

        // Act
        String result = libraryService.returnBook(availableBook.getIsbn());

        // Assert
        assertThat(result).contains("Late fee: $2.50"); // 5 days * $0.50
    }

    @Test
    @DisplayName("Should not charge late fee for premium member")
    void shouldNotChargeLateFeeForPremiumMember() {
        // Arrange
        availableBook.setStatus(BookStatus.CHECKED_OUT);
        availableBook.setCheckedOutBy(premiumMember.getEmail());
        availableBook.setDueDate(LocalDate.now().minusDays(5)); // 5 days late

        when(bookService.findByIsbn(availableBook.getIsbn()))
                .thenReturn(availableBook);
        when(memberService.findByEmail(premiumMember.getEmail()))
                .thenReturn(premiumMember);
        when(lateFeeCalculatorFactory.getCalculatorFor(MembershipType.PREMIUM))
                .thenReturn(new PremiumLateFeeCalculator());

        premiumMember.setBooksCheckedOut(1);

        // Act
        String result = libraryService.returnBook(availableBook.getIsbn());

        // Assert
        assertThat(result).isEqualTo("Book returned successfully");
        assertThat(result).doesNotContain("Late fee");

        verify(bookService).returnBook(eq(availableBook));
        verify(memberService).decrementCheckoutCount(eq(premiumMember));
    }

    @Test
    @DisplayName("Should search books by title")
    void shouldSearchBooksByTitle() {
        // Arrange
        when(bookRepository.findByTitleContainingIgnoreCase("Clean"))
                .thenReturn(java.util.List.of(availableBook));

        // Act
        var results = libraryService.searchBooks("Clean", "title");

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).isEqualTo("Clean Code");
    }

    @Test
    @DisplayName("Should throw exception for invalid search type")
    void shouldThrowExceptionForInvalidSearchType() {
        // Act & Assert
        assertThatThrownBy(() ->
                libraryService.searchBooks("test", "invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid search type");
    }
}