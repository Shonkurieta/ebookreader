package com.example.ebookreader.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    @Bean
    public StandardServletMultipartResolver multipartResolver() {
        return new StandardServletMultipartResolver();
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        System.out.println("🔧 [WebConfig] Configuring static resource handlers");
        
        // ✅ ИСПРАВЛЕНО: Убрали "backend/" из пути
        // Раздача статических файлов обложек
        registry.addResourceHandler("/covers/**")
                .addResourceLocations("file:assets/covers/")
                .setCachePeriod(3600);
        
        System.out.println("   ✅ Mapped /covers/** -> file:assets/covers/");
        
        // Дополнительно для всех assets
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("file:assets/")
                .setCachePeriod(3600);
        
        System.out.println("   ✅ Mapped /assets/** -> file:assets/");
    }
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);
    }
}