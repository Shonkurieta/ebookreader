package com.example.ebookreader.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.ebookreader.model.User;
import com.example.ebookreader.repository.UserRepository;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    
    @Autowired
    private UserRepository userRepository;
    
    // Оригинальный метод для загрузки по nickname (используется при логине)
    @Override
    public UserDetails loadUserByUsername(String nickname) throws UsernameNotFoundException {
        User user = userRepository.findByNickname(nickname)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + nickname));
        
        System.out.println("✅ Loading user by nickname: " + nickname);
        System.out.println("   User ID: " + user.getId());
        System.out.println("   User role: " + user.getRole());
        
        return new org.springframework.security.core.userdetails.User(
                user.getNickname(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
    
    // Новый метод для загрузки по ID (используется в JWT фильтре)
    public UserDetails loadUserById(Long userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
        
        System.out.println("✅ Loading user by ID: " + userId);
        System.out.println("   User nickname: " + user.getNickname());
        System.out.println("   User role: " + user.getRole());
        
        return new org.springframework.security.core.userdetails.User(
                user.getNickname(),  // nickname для UserDetails
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole()))
        );
    }
    
    // Получить объект User по ID (для контроллеров)
    public User getUserById(Long userId) throws UsernameNotFoundException {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + userId));
    }
}