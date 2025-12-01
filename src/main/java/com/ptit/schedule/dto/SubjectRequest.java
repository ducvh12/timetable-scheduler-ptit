package com.ptit.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request tạo/cập nhật môn học")
public class SubjectRequest {
    
    @Schema(description = "Mã môn học", example = "INT14149", required = true)
    @NotBlank(message = "Subject code is required")
    @Size(max = 50, message = "Subject code must not exceed 50 characters")
    private String subjectCode;

    @Schema(description = "Tên môn học", example = "IoT và ứng dụng", required = true)
    @NotBlank(message = "Subject name is required")
    @Size(max = 255, message = "Subject name must not exceed 255 characters")
    private String subjectName;
    
//    @NotNull(message = "Students per class is required")
    @Min(value = 0, message = "Students per class must be at least 1")
    @Max(value = 200, message = "Students per class must not exceed 200")
    private Integer studentsPerClass;
    
//    @NotNull(message = "Number of classes is required")
    @Min(value = 1, message = "Number of classes must be at least 1")
    @Max(value = 50, message = "Number of classes must not exceed 50")
    private Integer numberOfClasses;
    
    @NotNull(message = "Credits is required")
    @Min(value = 1, message = "Credits must be at least 1")
    @Max(value = 10, message = "Credits must not exceed 10")
    private Integer credits;
    
//    @NotNull(message = "Theory hours is required")
    @Min(value = 0, message = "Theory hours must not be negative")
    private Integer theoryHours;
    
//    @NotNull(message = "Exercise hours is required")
    @Min(value = 0, message = "Exercise hours must not be negative")
    private Integer exerciseHours;
    
//    @NotNull(message = "Project hours is required")
    @Min(value = 0, message = "Project hours must not be negative")
    private Integer projectHours;
    
//    @NotNull(message = "Lab hours is required")
    @Min(value = 0, message = "Lab hours must not be negative")
    private Integer labHours;
    
//    @NotNull(message = "Self study hours is required")
    @Min(value = 0, message = "Self study hours must not be negative")
    private Integer selfStudyHours;
    
//    @NotBlank(message = "Department is required")
    @Size(max = 100, message = "Department must not exceed 100 characters")
    private String department;
    
//    @NotBlank(message = "Exam format is required")
    @Size(max = 50, message = "Exam format must not exceed 50 characters")
    private String examFormat;

    @NotNull(message = "Class year is required")
    private String classYear;

    @NotNull(message = "Faculty ID is required")
    private String facultyId;

    @NotBlank(message = "Major ID is required")
    private String majorId;
    
    // Các trường cho major nếu cần tạo mới (optional)
    private String majorName;

    private String programType;

    @NotNull(message = "Number of students is required")
    private Integer numberOfStudents;

    @NotBlank(message = "Semester name is required")
    private String semesterName;

    @NotBlank(message = "Academic year is required")
    private String academicYear;

    private Boolean isCommon;
}
