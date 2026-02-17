package com.example.ebookreader.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ebookreader.config.JwtUtil;
import com.example.ebookreader.dto.LoginRequest;
import com.example.ebookreader.dto.RegisterRequest;
import com.example.ebookreader.exception.BadRequestException;
import com.example.ebookreader.exception.UnauthorizedException;
import com.example.ebookreader.model.User;
import com.example.ebookreader.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
@Tag(name = "Аутентификация", description = "API для регистрации и входа пользователей")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Operation(summary = "Регистрация нового пользователя")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            String username = request.getUsername().trim();
            String email = request.getEmail().trim();

            if (userRepository.findByNickname(username).isPresent()) {
                throw new BadRequestException("Пользователь с таким именем уже существует");
            }

            if (userRepository.findByEmail(email).isPresent()) {
                throw new BadRequestException("Email уже используется");
            }

            User user = new User();
            user.setNickname(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole("USER");

            User savedUser = userRepository.save(user);

            // Создаем UserDetails напрямую для генерации токена
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    savedUser.getNickname(),
                    savedUser.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + savedUser.getRole()))
            );

            String token = jwtUtil.generateToken(savedUser.getId(), userDetails);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("username", savedUser.getNickname());
            response.put("email", savedUser.getEmail());
            response.put("role", savedUser.getRole());

            return ResponseEntity.ok(response);
            
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Ошибка регистрации: " + e.getMessage()));
        }
    }

    @Operation(summary = "Вход пользователя в систему")
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            String loginIdentifier = request.getUsername().trim();
            String password = request.getPassword();

            // 1. Ищем пользователя по нику или почте
            User user = userRepository.findByNickname(loginIdentifier)
                    .orElseGet(() -> userRepository.findByEmail(loginIdentifier)
                    .orElseThrow(() -> new UnauthorizedException("Неверное имя пользователя или пароль")));

            // 2. Проверяем пароль через BCrypt
            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new UnauthorizedException("Неверное имя пользователя или пароль");
            }

            // 3. Создаем UserDetails вручную (без повторного запроса к БД)
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    user.getNickname(),
                    user.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
            );

            // 4. Генерируем токен
            String token = jwtUtil.generateToken(user.getId(), userDetails);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getNickname());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());

            return ResponseEntity.ok(response);
            
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(401).body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Ошибка входа: " + e.getMessage()));
        }
    }

    @Operation(summary = "Обновление токена")
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String identifier = jwtUtil.extractUsername(token);
            
            User user = userRepository.findByNickname(identifier)
                    .orElseGet(() -> userRepository.findByEmail(identifier)
                    .orElseThrow(() -> new UnauthorizedException("Пользователь не найден")));
            
            UserDetails userDetails = new org.springframework.security.core.userdetails.User(
                    user.getNickname(),
                    user.getPassword(),
                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
            );

            String newToken = jwtUtil.generateToken(user.getId(), userDetails);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", newToken);
            response.put("username", user.getNickname());
            response.put("role", user.getRole());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Ошибка обновления токена: " + e.getMessage()));
        }
    }

    @Operation(summary = "Тестовый эндпоинт")
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of("message", "Auth controller is working"));
    }
}
