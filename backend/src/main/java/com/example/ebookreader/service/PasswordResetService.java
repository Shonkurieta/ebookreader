package com.example.ebookreader.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ebookreader.exception.BadRequestException;
import com.example.ebookreader.model.PasswordResetCode;
import com.example.ebookreader.model.User;
import com.example.ebookreader.repository.PasswordResetCodeRepository;
import com.example.ebookreader.repository.UserRepository;

@Service
public class PasswordResetService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetCodeRepository resetCodeRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final String mailFrom;
    private final int codeTtlMinutes;
    private final int maxAttempts;

    public PasswordResetService(
            UserRepository userRepository,
            PasswordResetCodeRepository resetCodeRepository,
            JavaMailSender mailSender,
            PasswordEncoder passwordEncoder,
            @Value("${ebookreader.mail.from}") String mailFrom,
            @Value("${ebookreader.password-reset.code-ttl-minutes:15}") int codeTtlMinutes,
            @Value("${ebookreader.password-reset.max-attempts:5}") int maxAttempts) {
        this.userRepository = userRepository;
        this.resetCodeRepository = resetCodeRepository;
        this.mailSender = mailSender;
        this.passwordEncoder = passwordEncoder;
        this.mailFrom = mailFrom;
        this.codeTtlMinutes = codeTtlMinutes;
        this.maxAttempts = maxAttempts;
    }

    @Transactional
    public void requestReset(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            return;
        }
        if ("GOOGLE".equalsIgnoreCase(user.getAuthProvider())) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));

        resetCodeRepository.markUnusedCodesAsUsed(email, now);

        PasswordResetCode resetCode = new PasswordResetCode();
        resetCode.setEmail(email);
        resetCode.setCodeHash(passwordEncoder.encode(code));
        resetCode.setExpiresAt(now.plusMinutes(codeTtlMinutes));
        resetCodeRepository.save(resetCode);

        sendResetEmail(email, code);
    }

    @Transactional
    public void confirmReset(String rawEmail, String code, String newPassword) {
        String email = normalizeEmail(rawEmail);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadRequestException("Неверный или просроченный код"));

        if ("GOOGLE".equalsIgnoreCase(user.getAuthProvider())) {
            throw new BadRequestException("Для Google аккаунта используйте вход через Google");
        }

        PasswordResetCode resetCode = resetCodeRepository
                .findFirstByEmailAndUsedAtIsNullOrderByCreatedAtDesc(email)
                .orElseThrow(() -> new BadRequestException("Неверный или просроченный код"));

        LocalDateTime now = LocalDateTime.now();
        if (resetCode.getExpiresAt().isBefore(now) || resetCode.getAttemptCount() >= maxAttempts) {
            resetCode.setUsedAt(now);
            resetCodeRepository.save(resetCode);
            throw new BadRequestException("Неверный или просроченный код");
        }

        if (!passwordEncoder.matches(code, resetCode.getCodeHash())) {
            resetCode.setAttemptCount(resetCode.getAttemptCount() + 1);
            resetCodeRepository.save(resetCode);
            throw new BadRequestException("Неверный или просроченный код");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        resetCode.setUsedAt(now);
        userRepository.save(user);
        resetCodeRepository.save(resetCode);
    }

    private void sendResetEmail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("Код восстановления пароля EBook Reader");
        message.setText("Ваш код восстановления пароля: " + code
                + "\n\nКод действует " + codeTtlMinutes + " минут."
                + "\nЕсли вы не запрашивали восстановление, просто проигнорируйте это письмо.");
        mailSender.send(message);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
