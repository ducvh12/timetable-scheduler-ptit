package com.ptit.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemesterRequest {
    
    @NotBlank(message = "Tên học kỳ không được để trống")
    private String semesterName;
    
    @NotBlank(message = "Năm học không được để trống")
    private String academicYear;
    
    private LocalDate startDate;
    
    private LocalDate endDate;
    
    private Boolean isActive;
    
    private String description;
}
