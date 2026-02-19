package com.example.ebookreader.service;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.ebookreader.config.JwtUtil;
import com.example.ebookreader.controller.AuthController;
import com.example.ebookreader.dto.LoginRequest;
import com.example.ebookreader.model.User;
import com.example.ebookreader.repository.UserRepository;

class AuthControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLoginSuccess() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        User user = new User();
        user.setNickname("testuser");
        user.setPassword("hashed_password");
        user.setRole("USER");

        when(userRepository.findByNickname("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed_password")).thenReturn(true);
        when(jwtUtil.generateToken(any(), any())).thenReturn("mocked_token");

        // When
        ResponseEntity<?> response = authController.login(request);

        // Then
        assertEquals(200, response.getStatusCodeValue());
        verify(userRepository).findByNickname("testuser");
    }

    @Test
    void testLoginFailureInvalidPassword() {
        // Given
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("wrong_password");

        User user = new User();
        user.setNickname("testuser");
        user.setPassword("hashed_password");

        when(userRepository.findByNickname("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_password", "hashed_password")).thenReturn(false);

        // When
        ResponseEntity<?> response = authController.login(request);

        // Then
        assertEquals(401, response.getStatusCodeValue());
    }
}
