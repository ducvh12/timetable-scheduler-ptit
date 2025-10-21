package com.ptit.schedule.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class MajorRequest {
    
    @NotBlank(message = "Major ID is required")
    @Size(max = 50, message = "Major ID must not exceed 50 characters")
    private String majorId;
    
    @NotBlank(message = "Major name is required")
    @Size(max = 255, message = "Major name must not exceed 255 characters")
    private String majorName;
    
    @NotNull(message = "Number of students is required")
    @Min(value = 1, message = "Number of students must be at least 1")
    @Max(value = 1000, message = "Number of students must not exceed 1000")
    private Integer numberOfStudents;
    
    @NotBlank(message = "Class year is required")
    @Size(max = 10, message = "Class year must not exceed 10 characters")
    private String classYear;
    
    @NotBlank(message = "Faculty ID is required")
    private String facultyId;
}
