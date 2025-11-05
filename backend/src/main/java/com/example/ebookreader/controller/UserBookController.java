package com.example.ebookreader.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ebookreader.config.JwtUtil;
import com.example.ebookreader.model.Book;
import com.example.ebookreader.model.User;
import com.example.ebookreader.model.UserBook;
import com.example.ebookreader.repository.BookRepository;
import com.example.ebookreader.repository.UserBookRepository;
import com.example.ebookreader.repository.UserRepository;

@RestController
@RequestMapping("/api/user/books")
@CrossOrigin(origins = "*")
public class UserBookController {

    @Autowired
    private UserBookRepository userBookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // Добавить в закладки
    @PostMapping("/{bookId}/bookmark")
    public ResponseEntity<?> addBookmark(
            @RequestHeader("Authorization") String token,
            @PathVariable Long bookId) {
        String username = jwtUtil.extractUsername(token.replace("Bearer ", ""));
        
        Optional<User> userOpt = userRepository.findByNickname(username);
        Optional<Book> bookOpt = bookRepository.findById(bookId);

        if (userOpt.isEmpty() || bookOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        Book book = bookOpt.get();

        Optional<UserBook> existing = userBookRepository.findByUserIdAndBookId(user.getId(), bookId);
        
        if (existing.isPresent()) {
            UserBook ub = existing.get();
            ub.setBookmarked(true);
            userBookRepository.save(ub);
        } else {
            UserBook ub = new UserBook();
            ub.setUser(user);
            ub.setBook(book);
            ub.setBookmarked(true);
            ub.setCurrentChapter(1);
            userBookRepository.save(ub);
        }

        return ResponseEntity.ok(Map.of("message", "Добавлено в закладки"));
    }

    // Удалить из закладок
    @DeleteMapping("/{bookId}/bookmark")
    public ResponseEntity<?> removeBookmark(
            @RequestHeader("Authorization") String token,
            @PathVariable Long bookId) {
        String username = jwtUtil.extractUsername(token.replace("Bearer ", ""));
        
        return userRepository.findByNickname(username)
                .flatMap(user -> userBookRepository.findByUserIdAndBookId(user.getId(), bookId))
                .map(ub -> {
                    ub.setBookmarked(false);
                    userBookRepository.save(ub);
                    return ResponseEntity.ok(Map.of("message", "Удалено из закладок"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Получить все закладки пользователя
    @GetMapping("/bookmarks")
    public ResponseEntity<?> getBookmarks(@RequestHeader("Authorization") String token) {
        String username = jwtUtil.extractUsername(token.replace("Bearer ", ""));
        
        return userRepository.findByNickname(username)
                .map(user -> {
                    List<UserBook> bookmarks = userBookRepository.findByUserIdAndBookmarkedTrue(user.getId());
                    List<Map<String, Object>> result = bookmarks.stream()
                            .map(ub -> {
                                Map<String, Object> item = new HashMap<>();
                                item.put("id", ub.getBook().getId());
                                item.put("title", ub.getBook().getTitle());
                                item.put("author", ub.getBook().getAuthor());
                                item.put("coverUrl", ub.getBook().getCoverUrl());
                                item.put("currentChapter", ub.getCurrentChapter());
                                return item;
                            })
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // Обновить прогресс чтения
    @PutMapping("/{bookId}/progress")
    public ResponseEntity<?> updateProgress(
            @RequestHeader("Authorization") String token,
            @PathVariable Long bookId,
            @RequestBody Map<String, Integer> request) {
        String username = jwtUtil.extractUsername(token.replace("Bearer ", ""));
        Integer chapter = request.get("chapter");

        if (chapter == null || chapter < 1) {
            return ResponseEntity.badRequest().body(Map.of("message", "Неверный номер главы"));
        }

        Optional<User> userOpt = userRepository.findByNickname(username);
        Optional<Book> bookOpt = bookRepository.findById(bookId);

        if (userOpt.isEmpty() || bookOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        Book book = bookOpt.get();

        Optional<UserBook> existing = userBookRepository.findByUserIdAndBookId(user.getId(), bookId);
        
        if (existing.isPresent()) {
            UserBook ub = existing.get();
            ub.setCurrentChapter(chapter);
            userBookRepository.save(ub);
        } else {
            UserBook ub = new UserBook();
            ub.setUser(user);
            ub.setBook(book);
            ub.setCurrentChapter(chapter);
            ub.setBookmarked(false);
            userBookRepository.save(ub);
        }

        return ResponseEntity.ok(Map.of("message", "Прогресс сохранён"));
    }

    // Получить прогресс чтения книги
    @GetMapping("/{bookId}/progress")
    public ResponseEntity<?> getProgress(
            @RequestHeader("Authorization") String token,
            @PathVariable Long bookId) {
        String username = jwtUtil.extractUsername(token.replace("Bearer ", ""));
        
        return userRepository.findByNickname(username)
                .flatMap(user -> userBookRepository.findByUserIdAndBookId(user.getId(), bookId))
                .map(ub -> ResponseEntity.ok(Map.of(
                    "currentChapter", ub.getCurrentChapter(),
                    "isBookmarked", ub.isBookmarked()
                )))
                .orElse(ResponseEntity.ok(Map.of(
                    "currentChapter", 1,
                    "isBookmarked", false
                )));
    }
}