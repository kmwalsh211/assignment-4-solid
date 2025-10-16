package edu.trincoll.service;

import edu.trincoll.model.Book;
import edu.trincoll.model.BookStatus;
import edu.trincoll.model.Member;
import edu.trincoll.repository.BookRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class BookService {
    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Book findByIsbn(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .orElseThrow(() -> new IllegalArgumentException("Book not found with ISBN: " + isbn));
    }

    public void checkoutBook(Book book, Member member, int loanPeriodDays) {
        book.setStatus(BookStatus.CHECKED_OUT);
        book.setCheckedOutBy(member.getEmail());
        book.setDueDate(LocalDate.now().plusDays(loanPeriodDays));
        bookRepository.save(book);
    }

    public void returnBook(Book book) {
        book.setStatus(BookStatus.AVAILABLE);
        book.setCheckedOutBy(null);
        book.setDueDate(null);
        bookRepository.save(book);
    }
}
