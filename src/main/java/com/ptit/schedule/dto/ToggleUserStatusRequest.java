package com.ptit.schedule.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for toggling user status (active/deactive)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToggleUserStatusRequest {
    
    @NotNull(message = "Enabled status không được để trống")
    private Boolean enabled;
}
