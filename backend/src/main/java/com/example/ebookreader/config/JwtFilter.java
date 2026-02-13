package com.example.ebookreader.config;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.ebookreader.service.CustomUserDetailsService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    public JwtFilter(JwtUtil jwtUtil, CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        System.out.println("\n═══════════════════════════════════════");
        System.out.println("🔹 JWT FILTER - REQUEST");
        System.out.println("═══════════════════════════════════════");
        System.out.println("URI: " + path);
        System.out.println("Method: " + request.getMethod());
        
        // 🔹 Пропускаем JWT фильтр для публичных эндпоинтов, статических файлов и GraphQL
        if (path.startsWith("/api/auth/") || 
            path.startsWith("/api/books") || 
            path.startsWith("/api/genres") ||
            path.startsWith("/api/test/") ||
            path.startsWith("/covers/") ||
            path.startsWith("/assets/") ||
            path.startsWith("/graphql") ||   // ← ДОБАВЛЕНО
            path.startsWith("/graphiql")) {  // ← ДОБАВЛЕНО
            
            System.out.println("✅ Публичный ресурс - пропуск JWT фильтра");
            System.out.println("═══════════════════════════════════════\n");
            filterChain.doFilter(request, response);
            return;
        }
        
        final String authHeader = request.getHeader("Authorization");
        System.out.println("Authorization header: " + (authHeader != null ? authHeader.substring(0, Math.min(30, authHeader.length())) + "..." : "NULL"));
        
        // 🔹 Если нет заголовка Authorization
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("⚠️ Нет валидного Authorization заголовка");
            System.out.println("═══════════════════════════════════════\n");
            filterChain.doFilter(request, response);
            return;
        }
        
        try {
            final String jwtToken = authHeader.substring(7);
            System.out.println("Token extracted (first 20 chars): " + jwtToken.substring(0, Math.min(20, jwtToken.length())) + "...");
            
            final Long userId = jwtUtil.extractUserId(jwtToken);
            System.out.println("User ID from token: " + userId);
            
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                System.out.println("🔍 Loading user details for ID: " + userId);
                
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                System.out.println("✅ User details loaded");
                
                System.out.println("🔍 Validating token...");
                if (jwtUtil.isTokenValid(jwtToken, userId)) {
                    System.out.println("✅ Token is VALID");
                    
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    System.out.println("✅ Authentication set in SecurityContext");
                } else {
                    System.out.println("❌ Token is INVALID");
                }
            }
        } catch (Exception e) {
            System.err.println("❌ ERROR in JWT Filter: " + e.getClass().getName());
            System.err.println("   Message: " + e.getMessage());
        }
        
        System.out.println("═══════════════════════════════════════\n");
        filterChain.doFilter(request, response);
    }
}
