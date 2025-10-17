package edu.trincoll.service;

import edu.trincoll.model.Book;
import edu.trincoll.repository.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class BookSearchService {
    private final BookRepository bookRepository;

    public BookSearchService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    /**
     * Case-insensitive, contains-style title search.
     */
    public List<Book> searchByTitle(String title) {
        if (title == null || title.isBlank()) {
            return Collections.emptyList();
        }
        return bookRepository.findByTitleContainingIgnoreCase(title.trim());
    }

    /**
     * Exact author match (adjust to a contains/ignore-case variant if your repo provides one).
     */
    public List<Book> searchByAuthor(String author) {
        if (author == null || author.isBlank()) {
            return Collections.emptyList();
        }
        return bookRepository.findByAuthor(author.trim());
    }

    /**
     * ISBN lookup; returns at most one book.
     */
    public Optional<Book> searchByIsbn(String isbn) {
        if (isbn == null || isbn.isBlank()) {
            return Optional.empty();
        }
        return bookRepository.findByIsbn(isbn.trim());
    }
}
