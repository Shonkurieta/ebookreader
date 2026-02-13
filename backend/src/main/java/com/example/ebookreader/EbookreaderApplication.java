package com.example.ebookreader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry; // Импортируем

@SpringBootApplication
@EnableRetry // Включаем поддержку Spring Retry
public class EbookreaderApplication {
    public static void main(String[] args) {
        SpringApplication.run(EbookreaderApplication.class, args);
    }
}