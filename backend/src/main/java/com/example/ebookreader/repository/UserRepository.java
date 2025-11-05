package com.example.ebookreader.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ebookreader.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Поиск по nickname
    Optional<User> findByNickname(String nickname);
    
    // Поиск по email
    Optional<User> findByEmail(String email);
    
    // Поиск по ID (наследуется от JpaRepository, но можно указать явно)
    Optional<User> findById(Long id);
}