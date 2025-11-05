package com.example.ebookreader.config;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "FangSparrow33344@1$_SecretKey_ForJWT2025";
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 10; // 10 часов
    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    // === Генерация токена ===
    public String generateToken(Long userId, UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        
        claims.put("userId", userId);
        
        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        
        claims.put("authorities", authorities);
        
        System.out.println("🔹 Generating JWT token");
        System.out.println("   User ID: " + userId);
        System.out.println("   Username (nickname): " + userDetails.getUsername());
        System.out.println("   Authorities: " + authorities);
        
        // ✅ В subject храним nickname, а userId в claims
        String token = createToken(claims, userDetails.getUsername());
        System.out.println("   Token created (first 30 chars): " + token.substring(0, Math.min(30, token.length())) + "...");
        
        return token;
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date(System.currentTimeMillis());
        Date expiration = new Date(System.currentTimeMillis() + EXPIRATION_TIME);
        
        System.out.println("   Issued at: " + now);
        System.out.println("   Expires at: " + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // === Извлечение userId из claims ===
    public Long extractUserId(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object userIdObj = claims.get("userId");
            Long userId = null;
            
            if (userIdObj instanceof Integer) {
                userId = ((Integer) userIdObj).longValue();
            } else if (userIdObj instanceof Long) {
                userId = (Long) userIdObj;
            }
            
            System.out.println("🔹 Extracted userId from token: " + userId);
            return userId;
        } catch (Exception e) {
            System.err.println("❌ Error extracting userId: " + e.getMessage());
            throw e;
        }
    }

    // ✅ Извлечение nickname из subject
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    // Извлечение authorities из токена
    public String extractAuthorities(String token) {
        try {
            Claims claims = extractAllClaims(token);
            String authorities = claims.get("authorities", String.class);
            System.out.println("🔹 Extracted authorities from token: " + authorities);
            return authorities;
        } catch (Exception e) {
            System.err.println("❌ Error extracting authorities: " + e.getMessage());
            return null;
        }
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            System.err.println("❌ Error parsing token: " + e.getClass().getName());
            System.err.println("   Message: " + e.getMessage());
            throw e;
        }
    }

    // === Проверка токена ===
    private boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            Date now = new Date();
            boolean expired = expiration.before(now);
            
            System.out.println("🔹 Token expiration check:");
            System.out.println("   Expires at: " + expiration);
            System.out.println("   Current time: " + now);
            System.out.println("   Is expired: " + expired);
            
            return expired;
        } catch (Exception e) {
            System.err.println("❌ Error checking expiration: " + e.getMessage());
            return true;
        }
    }

    // Валидация по userId
    public boolean isTokenValid(String token, Long userId) {
        try {
            System.out.println("🔹 Validating token:");
            
            final Long tokenUserId = extractUserId(token);
            
            System.out.println("   Token userId: " + tokenUserId);
            System.out.println("   Expected userId: " + userId);
            
            boolean userIdMatches = tokenUserId.equals(userId);
            System.out.println("   UserId matches: " + userIdMatches);
            
            boolean expired = isTokenExpired(token);
            System.out.println("   Token expired: " + expired);
            
            boolean valid = userIdMatches && !expired;
            System.out.println("   Final result: " + (valid ? "✅ VALID" : "❌ INVALID"));
            
            return valid;
        } catch (Exception e) {
            System.err.println("❌ Token validation error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // Валидация по UserDetails
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            System.err.println("❌ Token validation error: " + e.getMessage());
            return false;
        }
    }
}