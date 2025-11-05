package com.example.ebookreader.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ebookreader.config.JwtUtil;
import com.example.ebookreader.model.User;
import com.example.ebookreader.repository.UserRepository;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String nickname = request.get("username");
            String email = request.get("email");
            String password = request.get("password");

            if (nickname == null || nickname.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Имя пользователя обязательно"));
            }

            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Email обязателен"));
            }

            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Пароль обязателен"));
            }

            if (userRepository.findByNickname(nickname).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Пользователь с таким именем уже существует"));
            }

            if (userRepository.findByEmail(email).isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Email уже используется"));
            }

            User user = new User();
            user.setNickname(nickname);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setRole("USER");

            User savedUser = userRepository.save(user);

            UserDetails userDetails = userDetailsService.loadUserByUsername(nickname);
            String token = jwtUtil.generateToken(savedUser.getId(), userDetails);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getNickname());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Ошибка регистрации: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String login = request.get("username");
            String password = request.get("password");

            if (login == null || login.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Логин обязателен"));
            }

            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Пароль обязателен"));
            }

            Optional<User> userOpt = userRepository.findByNickname(login);
            
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByEmail(login);
            }
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401)
                        .body(Map.of("message", "Неверное имя пользователя или пароль"));
            }

            User user = userOpt.get();

            if (!passwordEncoder.matches(password, user.getPassword())) {
                return ResponseEntity.status(401)
                        .body(Map.of("message", "Неверное имя пользователя или пароль"));
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getNickname());
            String token = jwtUtil.generateToken(user.getId(), userDetails);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getNickname());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());

            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Ошибка входа: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            String token = authHeader.replace("Bearer ", "");
            String identifier = jwtUtil.extractUsername(token);
            
            Optional<User> userOpt = userRepository.findByNickname(identifier);
            
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByEmail(identifier);
            }
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Пользователь не найден"));
            }
            
            User user = userOpt.get();
            
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getNickname());
            String newToken = jwtUtil.generateToken(user.getId(), userDetails);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", newToken);
            response.put("username", user.getNickname());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Ошибка обновления токена: " + e.getMessage()));
        }
    }

    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of("message", "Auth controller is working"));
    }
}