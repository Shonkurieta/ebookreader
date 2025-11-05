package com.example.ebookreader;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHasher {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "KleinYuangDao$@*!245";
        String hashedPassword = encoder.encode(password);
        
        System.out.println("Original password: " + password);
        System.out.println("Hashed password: " + hashedPassword);
        
        // Проверка что хэш работает
        boolean matches = encoder.matches(password, hashedPassword);
        System.out.println("Password matches: " + matches);
    }
}

// ===================================
// КАК ИСПОЛЬЗОВАТЬ:
// ===================================
// 1. Создайте этот файл в вашем проекте
// 2. Запустите его как обычное Java приложение
// 3. Скопируйте захэшированный пароль из консоли
// 4. Вставьте его в SQL INSERT для создания админа

// ===================================
// АЛЬТЕРНАТИВА - Онлайн генератор:
// ===================================
// Можете использовать онлайн: https://bcrypt-generator.com/
// Rounds: 10 (по умолчанию для Spring Security)
// Password: admIn1