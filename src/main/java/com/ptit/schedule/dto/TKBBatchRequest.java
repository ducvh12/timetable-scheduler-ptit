package com.ptit.schedule.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TKBBatchRequest {
    
    @NotNull(message = "Items list is required")
    @NotEmpty(message = "Items list cannot be empty")
    private List<TKBRequest> items;
    
    private Long userId;  // User ID (có thể lấy từ auth context)
    
    private String academicYear;  // Năm học (VD: "2023-2024")
    
    private String semester;  // Học kỳ (VD: "1", "2", "hè")
}

