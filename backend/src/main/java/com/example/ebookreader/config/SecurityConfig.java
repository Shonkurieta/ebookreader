package com.example.ebookreader.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowedOrigins(List.of("*"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(false);
                return config;
            }))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ✅ Публичные эндпоинты (доступны БЕЗ авторизации)
                .requestMatchers("/api/auth/**").permitAll()        // Регистрация, логин, refresh
                .requestMatchers("/api/books/**").permitAll()       // Книги доступны всем
                .requestMatchers("/api/genres/**").permitAll()      // Жанры доступны всем
                .requestMatchers("/api/test/**").permitAll()        // Тестовые эндпоинты
                
                // ✅ СТАТИЧЕСКИЕ ФАЙЛЫ - обложки книг (БЕЗ авторизации)
                .requestMatchers("/covers/**").permitAll()          // Обложки через /covers/
                .requestMatchers("/assets/**").permitAll()          // Обложки через /assets/
                .requestMatchers("/assets/covers/**").permitAll()   // Обложки через /assets/covers/
                
                // ✅ SWAGGER UI - документация API (БЕЗ авторизации)
                .requestMatchers("/swagger-ui/**").permitAll()      // Swagger UI интерфейс
                .requestMatchers("/v3/api-docs/**").permitAll()     // OpenAPI спецификация
                .requestMatchers("/swagger-ui.html").permitAll()    // Главная страница Swagger
                
                // ✅ Защищенные эндпоинты
                .requestMatchers("/api/admin/**").hasRole("ADMIN")  // Только админ
                .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN") // Профиль, закладки
                
                // Все остальное требует авторизации
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}