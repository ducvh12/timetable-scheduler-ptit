package com.ptit.schedule.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TKBRequest {
    
    @NotBlank(message = "Subject ID is required")
    private String ma_mon;  // ma_mon
    
    @NotBlank(message = "Subject name is required")
    private String ten_mon;  // ten_mon
    
    @NotNull(message = "Total periods is required")
    @Min(value = 1, message = "Total periods must be at least 1")
    private Integer sotiet;  // sotiet
    
    @NotNull(message = "Number of students is required")
    @Min(value = 1, message = "Number of students must be at least 1")
    private Integer siso;  // siso
    
    @NotNull(message = "Students per class is required")
    @Min(value = 1, message = "Students per class must be at least 1")
    private Integer siso_mot_lop;  // siso_mot_lop
    
    @NotNull(message = "Number of classes is required")
    @Min(value = 1, message = "Number of classes must be at least 1")
    private Integer solop;  // solop
    
    private String nganh;  // nganh
    
    private String subject_type;  // 'english', 'general'
    
    private String student_year;  // 'nt', '2024', 'general'
    
    private String he_dac_thu;  // he_dac_thu - "CLC", "CTTT", etc.
    
    // Getters for backward compatibility
    public String getSubjectId() { return ma_mon; }
    public String getSubjectName() { return ten_mon; }
    public Integer getTotalPeriods() { return sotiet; }
    public Integer getNumberOfStudents() { return siso; }
    public Integer getStudentsPerClass() { return siso_mot_lop; }
    public Integer getNumberOfClasses() { return solop; }
    public String getMajor() { return nganh; }
    public String getSubjectType() { return subject_type; }
    public String getStudentYear() { return student_year; }
    public String getSpecialSystem() { return he_dac_thu; }
}

