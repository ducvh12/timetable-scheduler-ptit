package com.ptit.schedule.service;

import com.ptit.schedule.dto.AuthResponse;
import com.ptit.schedule.dto.LoginRequest;
import com.ptit.schedule.dto.RegisterRequest;
import com.ptit.schedule.dto.UserResponse;

/**
 * Service interface for authentication operations
 */
public interface AuthService {
    
    /**
     * Register a new user
     */
    AuthResponse register(RegisterRequest request);
    
    /**
     * Login user and return JWT token
     */
    AuthResponse login(LoginRequest request);
    
    /**
     * Get current authenticated user
     */
    UserResponse getCurrentUser();
}
