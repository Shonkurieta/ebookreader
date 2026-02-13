package com.example.ebookreader.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid; // Импортируем @Valid

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
import com.example.ebookreader.dto.LoginRequest; // Импортируем DTO
import com.example.ebookreader.dto.RegisterRequest; // Импортируем DTO
import com.example.ebookreader.exception.BadRequestException; // Импортируем наше исключение
import com.example.ebookreader.exception.UnauthorizedException; // Импортируем наше исключение

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

    @Autowired
    private UserDetailsService userDetailsService;

    @Operation(summary = "Регистрация нового пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешная регистрация",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Неверные входные данные",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) { // Используем @Valid и DTO
        try {
            if (userRepository.findByNickname(request.getUsername()).isPresent()) {
                throw new BadRequestException("Пользователь с таким именем уже существует");
            }

            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new BadRequestException("Email уже используется");
            }

            User user = new User();
            user.setNickname(request.getUsername());
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setRole("USER");

            User savedUser = userRepository.save(user);

            UserDetails userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            String token = jwtUtil.generateToken(savedUser.getId(), userDetails);

            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("username", user.getNickname());
            response.put("email", user.getEmail());
            response.put("role", user.getRole());

            return ResponseEntity.ok(response);
            
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Ошибка регистрации: " + e.getMessage()));
        }
    }

    @Operation(summary = "Вход пользователя в систему")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Успешный вход",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Неверные учетные данные",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) { // Используем @Valid и DTO
        try {
            String login = request.getUsername();
            String password = request.getPassword();

            Optional<User> userOpt = userRepository.findByNickname(login);
            
            if (userOpt.isEmpty()) {
                userOpt = userRepository.findByEmail(login);
            }
            
            if (userOpt.isEmpty()) {
                throw new UnauthorizedException("Неверное имя пользователя или пароль");
            }

            User user = userOpt.get();

            if (!passwordEncoder.matches(password, user.getPassword())) {
                throw new UnauthorizedException("Неверное имя пользователя или пароль");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getNickname());
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Токен успешно обновлен",
                    content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                    content = @Content(schema = @Schema(implementation = Map.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        // ... существующая логика ...
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

    @Operation(summary = "Тестовый эндпоинт")
    @ApiResponse(responseCode = "200", description = "Успешный ответ")
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(Map.of("message", "Auth controller is working"));
    }
}
