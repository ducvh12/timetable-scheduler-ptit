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
}

