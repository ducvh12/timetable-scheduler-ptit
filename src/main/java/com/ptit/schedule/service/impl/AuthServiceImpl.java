package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.AuthResponse;
import com.ptit.schedule.dto.LoginRequest;
import com.ptit.schedule.dto.RegisterRequest;
import com.ptit.schedule.dto.UserResponse;
import com.ptit.schedule.entity.User;
import com.ptit.schedule.exception.DuplicateResourceException;
import com.ptit.schedule.exception.InvalidDataException;
import com.ptit.schedule.exception.ResourceNotFoundException;
import com.ptit.schedule.repository.UserRepository;
import com.ptit.schedule.security.JwtTokenProvider;
import com.ptit.schedule.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for authentication operations
 */
@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    
    @Override
    public AuthResponse register(RegisterRequest request) {
        // Check if username already exists - 409 Conflict
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username đã tồn tại");
        }
        
        // Check if email already exists - 409 Conflict
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email đã tồn tại");
        }
        
        // Create new user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .role(request.getRole())
                .enabled(false)
                .build();
        
        User savedUser = userRepository.save(user);
        
        // Generate JWT token using email and role
        String token = jwtTokenProvider.generateToken(savedUser.getEmail(), savedUser.getRole());
        
        return new AuthResponse(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole()
        );
    }
    
    @Override
    public AuthResponse login(LoginRequest request) {
        // Tìm user theo username hoặc email
        User user = userRepository.findByUsername(request.getUsername())
                .or(() -> userRepository.findByEmail(request.getUsername()))
                .orElseThrow(() -> new ResourceNotFoundException("Tài khoản không tồn tại"));
        if(!user.getEnabled()){
            throw new InvalidDataException("Tài khoản chưa đươc kích hoạt");
        }
        // Authenticate user với email (vì UserDetailsService load theo email)
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(), // Dùng email để authenticate
                        request.getPassword()
                )
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Generate JWT token using email and role
        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole());
        
        return new AuthResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }
    
    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResourceNotFoundException("User chưa đăng nhập");
        }
        
        // authentication.getName() sẽ trả về email (vì token lưu email)
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại"));
        
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .enabled(user.getEnabled())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
