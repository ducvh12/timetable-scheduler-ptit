package com.ptit.schedule.service;

import com.ptit.schedule.dto.UserResponse;

import java.util.List;

/**
 * Service interface for user management operations
 */
public interface UserService {
    
    /**
     * Get all users
     */
    List<UserResponse> getAllUsers();
    
    /**
     * Get user by ID
     */
    UserResponse getUserById(Long id);
    
    /**
     * Active/Deactive user
     */
    UserResponse toggleUserStatus(Long id, Boolean enabled);
    
    /**
     * Delete user
     */
    void deleteUser(Long id);
}
