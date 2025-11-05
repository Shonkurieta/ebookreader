    package com.example.ebookreader.config;

    import java.io.IOException;
    import java.util.Arrays;
    import java.util.List;
    import java.util.stream.Collectors;

    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
    import org.springframework.security.core.authority.SimpleGrantedAuthority;
    import org.springframework.security.core.context.SecurityContextHolder;
    import org.springframework.security.core.userdetails.UserDetails;
    import org.springframework.security.core.userdetails.UserDetailsService;
    import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
    import org.springframework.stereotype.Component;
    import org.springframework.web.filter.OncePerRequestFilter;

    import jakarta.servlet.FilterChain;
    import jakarta.servlet.ServletException;
    import jakarta.servlet.http.HttpServletRequest;
    import jakarta.servlet.http.HttpServletResponse;

    @Component
    public class JwtAuthenticationFilter extends OncePerRequestFilter {

        @Autowired
        private JwtUtil jwtUtil;

        @Autowired
        private UserDetailsService userDetailsService;

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain) throws ServletException, IOException {
            
            try {
                final String authHeader = request.getHeader("Authorization");
                final String jwt;
                final String username;

                System.out.println("🔒 [JwtAuthenticationFilter] Processing request:");
                System.out.println("   URI: " + request.getRequestURI());
                System.out.println("   Method: " + request.getMethod());

                // Если нет заголовка Authorization или не Bearer
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    System.out.println("   ℹ️ No JWT token found, continuing chain");
                    filterChain.doFilter(request, response);
                    return;
                }

                // Извлекаем токен
                jwt = authHeader.substring(7);
                System.out.println("   Token (first 30 chars): " + jwt.substring(0, Math.min(30, jwt.length())) + "...");

                // Извлекаем username из токена
                username = jwtUtil.extractUsername(jwt);
                System.out.println("   Username from token: " + username);

                // Если username есть и пользователь еще не аутентифицирован
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    
                    // Загружаем UserDetails
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    System.out.println("   UserDetails loaded for: " + username);
                    System.out.println("   UserDetails authorities: " + userDetails.getAuthorities());

                    // Проверяем токен
                    if (jwtUtil.isTokenValid(jwt, userDetails)) {
                        System.out.println("   ✅ Token is valid");

                        // ВАЖНО: Извлекаем authorities из токена
                        String authoritiesString = jwtUtil.extractAuthorities(jwt);
                        List<SimpleGrantedAuthority> authorities;
                        
                        if (authoritiesString != null && !authoritiesString.isEmpty()) {
                            authorities = Arrays.stream(authoritiesString.split(","))
                                    .map(SimpleGrantedAuthority::new)
                                    .collect(Collectors.toList());
                            System.out.println("   🔑 Authorities from token: " + authorities);
                        } else {
                            // Fallback на authorities из UserDetails
                            authorities = userDetails.getAuthorities().stream()
                                    .map(auth -> new SimpleGrantedAuthority(auth.getAuthority()))
                                    .collect(Collectors.toList());
                            System.out.println("   🔑 Authorities from UserDetails: " + authorities);
                        }

                        // Создаем authentication с authorities из токена
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                authorities // ← Используем authorities из токена
                        );

                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        System.out.println("   ✅ Authentication set in SecurityContext");
                        System.out.println("   Final authorities: " + authToken.getAuthorities());
                    } else {
                        System.out.println("   ❌ Token is invalid");
                    }
                }

                filterChain.doFilter(request, response);
                
            } catch (Exception e) {
                System.err.println("❌ [JwtAuthenticationFilter] Error: " + e.getMessage());
                e.printStackTrace();
                filterChain.doFilter(request, response);
            }
        }
    }