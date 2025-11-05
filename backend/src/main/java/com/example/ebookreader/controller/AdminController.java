package com.example.ebookreader.controller;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.ebookreader.dto.ChapterDTO;
import com.example.ebookreader.model.Book;
import com.example.ebookreader.model.Chapter;
import com.example.ebookreader.model.User;
import com.example.ebookreader.repository.BookRepository;
import com.example.ebookreader.repository.ChapterRepository;
import com.example.ebookreader.repository.UserRepository;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private UserRepository userRepository;

    // === УПРАВЛЕНИЕ КНИГАМИ ===

    @GetMapping("/books")
    public ResponseEntity<List<Book>> getAllBooks() {
        System.out.println("📚 [AdminController] GET /api/admin/books");
        return ResponseEntity.ok(bookRepository.findAll());
    }

    @GetMapping("/books/{id}")
    public ResponseEntity<Book> getBook(@PathVariable Long id) {
        System.out.println("📖 [AdminController] GET /api/admin/books/" + id);
        return bookRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/books", consumes = "multipart/form-data")
    public ResponseEntity<?> createBook(
            @RequestPart("title") String title,
            @RequestPart("author") String author,
            @RequestPart(value = "description", required = false) String description,
            @RequestPart(value = "cover", required = false) MultipartFile cover
    ) {
        System.out.println("➕ [AdminController] POST /api/admin/books");
        System.out.println("   Title: " + title);
        System.out.println("   Author: " + author);
        System.out.println("   Cover: " + (cover != null ? cover.getOriginalFilename() : "null"));

        try {
            if (title == null || title.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createError("Название книги обязательно"));
            }

            if (author == null || author.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createError("Автор книги обязателен"));
            }

            Book newBook = new Book();
            newBook.setTitle(title);
            newBook.setAuthor(author);
            newBook.setDescription(description != null ? description : "");

            if (cover != null && !cover.isEmpty()) {
                try {
                    Path uploadPath = Paths.get("assets/covers");
                    if (!Files.exists(uploadPath)) {
                        Files.createDirectories(uploadPath);
                        System.out.println("   📁 Created directory: " + uploadPath.toAbsolutePath());
                    }

                    String fileName = System.currentTimeMillis() + "_" + cover.getOriginalFilename();
                    Path filePath = uploadPath.resolve(fileName);

                    Files.copy(cover.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

                    String coverUrl = "assets/covers/" + fileName;
                    newBook.setCoverUrl(coverUrl);

                    System.out.println("   🖼 Cover saved: " + filePath.toAbsolutePath());
                    System.out.println("   📝 Cover URL in DB: " + coverUrl);

                } catch (IOException e) {
                    System.err.println("   ❌ Error saving cover: " + e.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(createError("Ошибка сохранения обложки: " + e.getMessage()));
                }
            }

            Book savedBook = bookRepository.save(newBook);
            System.out.println("   ✅ Book created with ID: " + savedBook.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(savedBook);

        } catch (Exception e) {
            System.err.println("   ❌ Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createError("Ошибка при создании книги: " + e.getMessage()));
        }
    }

    @PutMapping(value = "/books/{id}")
    public ResponseEntity<?> updateBook(@PathVariable Long id, @RequestBody Book bookDetails) {
        System.out.println("✏️ [AdminController] PUT /api/admin/books/" + id);

        try {
            Book existingBook = bookRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Книга не найдена"));

            existingBook.setTitle(bookDetails.getTitle());
            existingBook.setAuthor(bookDetails.getAuthor());
            existingBook.setDescription(bookDetails.getDescription());
            if (bookDetails.getCoverUrl() != null) {
                existingBook.setCoverUrl(bookDetails.getCoverUrl());
            }

            Book updatedBook = bookRepository.save(existingBook);
            System.out.println("   ✅ Book updated: " + updatedBook.getId());

            return ResponseEntity.ok(updatedBook);

        } catch (Exception e) {
            System.err.println("   ❌ Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createError("Ошибка при обновлении книги: " + e.getMessage()));
        }
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<?> deleteBook(@PathVariable Long id) {
        System.out.println("🗑 [AdminController] DELETE /api/admin/books/" + id);

        return bookRepository.findById(id)
                .map(book -> {
                    chapterRepository.deleteAll(chapterRepository.findByBookIdOrderByChapterOrderAsc(id));

                    if (book.getCoverUrl() != null && !book.getCoverUrl().isEmpty()) {
                        try {
                            Path coverPath = Paths.get(book.getCoverUrl());
                            Files.deleteIfExists(coverPath);
                            System.out.println("   🗑️ Cover file deleted: " + coverPath);
                        } catch (IOException e) {
                            System.err.println("   ⚠️ Could not delete cover file: " + e.getMessage());
                        }
                    }

                    bookRepository.delete(book);
                    System.out.println("   ✅ Book deleted: " + id);
                    return ResponseEntity.ok(createSuccess("Книга удалена"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/covers/{filename}")
    public ResponseEntity<Resource> getCover(@PathVariable String filename) {
        System.out.println("🖼 [AdminController] GET /api/admin/covers/" + filename);

        try {
            Path filePath = Paths.get("assets/covers").resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = Files.probeContentType(filePath);
                if (contentType == null) contentType = "image/jpeg";

                System.out.println("   ✅ Cover found: " + filePath);
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .body(resource);
            } else {
                System.out.println("   ❌ Cover not found: " + filePath);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            System.err.println("   ❌ Error loading cover: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // === ГЛАВЫ ===

    @GetMapping("/books/{bookId}/chapters")
    public ResponseEntity<List<Chapter>> getChapters(@PathVariable Long bookId) {
        System.out.println("📑 [AdminController] GET /api/admin/books/" + bookId + "/chapters");
        return ResponseEntity.ok(chapterRepository.findByBookIdOrderByChapterOrderAsc(bookId));
    }

    @PostMapping("/books/{bookId}/chapters")
    public ResponseEntity<?> createChapter(@PathVariable Long bookId, @RequestBody ChapterDTO dto) {
        System.out.println("➕ [AdminController] POST /api/admin/books/" + bookId + "/chapters");
        return bookRepository.findById(bookId)
                .map(book -> {
                    Chapter chapter = new Chapter();
                    chapter.setBook(book);
                    chapter.setchapterOrder(dto.getChapterOrder());
                    chapter.setTitle(dto.getTitle());
                    chapter.setContent(dto.getContent());
                    Chapter saved = chapterRepository.save(chapter);
                    System.out.println("   ✅ Chapter created: " + saved.getId());
                    return ResponseEntity.ok(saved);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/books/{bookId}/chapters/{chapterId}")
    public ResponseEntity<?> updateChapter(
            @PathVariable Long bookId,
            @PathVariable Long chapterId,
            @RequestBody ChapterDTO dto) {
        System.out.println("✏️ [AdminController] PUT /api/admin/books/" + bookId + "/chapters/" + chapterId);
        return chapterRepository.findById(chapterId)
                .map(chapter -> {
                    if (dto.getChapterOrder() != null) chapter.setchapterOrder(dto.getChapterOrder());
                    if (dto.getTitle() != null) chapter.setTitle(dto.getTitle());
                    if (dto.getContent() != null) chapter.setContent(dto.getContent());
                    Chapter updated = chapterRepository.save(chapter);
                    System.out.println("   ✅ Chapter updated: " + updated.getId());
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/books/{bookId}/chapters/{chapterId}")
    public ResponseEntity<?> deleteChapter(@PathVariable Long bookId, @PathVariable Long chapterId) {
        System.out.println("🗑 [AdminController] DELETE /api/admin/books/" + bookId + "/chapters/" + chapterId);
        return chapterRepository.findById(chapterId)
                .map(chapter -> {
                    chapterRepository.delete(chapter);
                    System.out.println("   ✅ Chapter deleted: " + chapterId);
                    return ResponseEntity.ok(createSuccess("Глава удалена"));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // === ПОЛЬЗОВАТЕЛИ ===

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        System.out.println("👥 [AdminController] GET /api/admin/users");
        return ResponseEntity.ok(userRepository.findAll());
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        System.out.println("🗑 [AdminController] DELETE /api/admin/users/" + id);

        return userRepository.findById(id)
                .map(user -> {
                    System.out.println("   🔎 User role: " + user.getRole());

                    if ("ADMIN".equals(user.getRole())) {
                        long adminCount = userRepository.findAll().stream()
                                .filter(u -> "ADMIN".equals(u.getRole()))
                                .count();

                        if (adminCount <= 1) {
                            System.out.println("   ❌ Нельзя удалить последнего администратора");
                            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                    .body(createError("Нельзя удалить последнего администратора"));
                        }
                    }

                    userRepository.delete(user);
                    System.out.println("   ✅ User deleted: " + id);
                    return ResponseEntity.ok(createSuccess("Пользователь удалён"));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createError("Пользователь не найден")));
    }

    // === HELPER ===

    private Map<String, String> createError(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("error", message);
        return map;
    }

    private Map<String, String> createSuccess(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        return map;
    }
}
