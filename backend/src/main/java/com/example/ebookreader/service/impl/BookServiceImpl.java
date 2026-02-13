package com.example.ebookreader.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ebookreader.dto.ChapterDTO;
import com.example.ebookreader.model.Book;
import com.example.ebookreader.model.Chapter;
import com.example.ebookreader.repository.BookRepository;
import com.example.ebookreader.repository.ChapterRepository;
import com.example.ebookreader.service.BookService;

@Service
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final ChapterRepository chapterRepository;

    @Autowired
    public BookServiceImpl(BookRepository bookRepository, ChapterRepository chapterRepository) {
        this.bookRepository = bookRepository;
        this.chapterRepository = chapterRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Book> searchBooks(String query) {
        if (query == null || query.trim().isEmpty()) {
            return bookRepository.findAll();
        }
        return bookRepository.findAll().stream()
                .filter(book -> 
                    book.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                    book.getAuthor().toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Book> getBookById(Long id) {
        return bookRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChapterDTO> getBookChapters(Long bookId) {
        List<Chapter> chapters = chapterRepository.findByBookIdOrderByChapterOrderAsc(bookId);
        return chapters.stream()
                .map(ch -> new ChapterDTO(
                        ch.getId(),
                        ch.getChapterOrder(),
                        ch.getTitle(),
                        null // Не отдаём контент в списке глав
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ChapterDTO> getChapter(Long bookId, int chapterOrder) {
        return chapterRepository.findByBookIdAndChapterOrder(bookId, chapterOrder)
                .map(ch -> new ChapterDTO(
                        ch.getId(),
                        ch.getChapterOrder(),
                        ch.getTitle(),
                        ch.getContent()
                ));
    }
}
