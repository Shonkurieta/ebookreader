package com.example.ebookreader.service;

import java.util.List;
import java.util.Optional;

import com.example.ebookreader.dto.ChapterDTO;
import com.example.ebookreader.model.Book;

public interface BookService {
    List<Book> getAllBooks();
    List<Book> searchBooks(String query);
    Optional<Book> getBookById(Long id);
    List<ChapterDTO> getBookChapters(Long bookId);
    Optional<ChapterDTO> getChapter(Long bookId, int chapterOrder);
}