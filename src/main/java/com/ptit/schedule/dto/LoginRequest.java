package com.ptit.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for login request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    
    @NotBlank(message = "Username hoặc email không được để trống")
    private String username; // Có thể là username hoặc email
    
    @NotBlank(message = "Password không được để trống")
    private String password;
}
