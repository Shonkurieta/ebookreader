package com.example.ebookreader.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.ebookreader.model.PasswordResetCode;

@Repository
public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {
    Optional<PasswordResetCode> findFirstByEmailAndUsedAtIsNullOrderByCreatedAtDesc(String email);

    @Modifying
    @Query("update PasswordResetCode c set c.usedAt = :usedAt where c.email = :email and c.usedAt is null")
    void markUnusedCodesAsUsed(String email, LocalDateTime usedAt);
}
