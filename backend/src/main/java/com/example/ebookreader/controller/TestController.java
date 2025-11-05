package com.example.ebookreader.controller;

import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "*")
public class TestController {

    @GetMapping("/public")
    public Map<String, Object> testPublic() {
        System.out.println("✅ TEST: Public endpoint accessed");
        return Map.of(
            "status", "success",
            "message", "Public endpoint works",
            "authenticated", SecurityContextHolder.getContext().getAuthentication() != null
        );
    }

    @GetMapping("/protected")
    public Map<String, Object> testProtected(@RequestHeader("Authorization") String authHeader) {
        System.out.println("═══════════════════════════════════════");
        System.out.println("✅ TEST: Protected endpoint accessed");
        System.out.println("Authorization header: " + authHeader);
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        if (auth != null) {
            System.out.println("✅ Authentication found:");
            System.out.println("   Principal: " + auth.getPrincipal());
            System.out.println("   Authorities: " + auth.getAuthorities());
            System.out.println("   Is authenticated: " + auth.isAuthenticated());
        } else {
            System.out.println("❌ NO Authentication in SecurityContext!");
        }
        System.out.println("═══════════════════════════════════════");
        
        return Map.of(
            "status", "success",
            "message", "Protected endpoint works",
            "authenticated", auth != null,
            "principal", auth != null ? auth.getPrincipal().toString() : "null",
            "authorities", auth != null ? auth.getAuthorities().toString() : "null"
        );
    }
}