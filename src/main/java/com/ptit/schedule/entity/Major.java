package com.ptit.schedule.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "majors", indexes = {
    // Composite index cho majorCode + classYear (unique constraint tự nhiên, thường filter cùng lúc)
    @Index(name = "idx_major_class_year", columnList = "major_code, class_year")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Major {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    
    @Column(name = "major_code", nullable = false)
    @NotBlank(message = "Major code is required")
    @Size(max = 50, message = "Major code must not exceed 50 characters")
    private String majorCode;
    
    @Column(name = "class_year", nullable = false)
    @NotBlank(message = "Class year is required")
    @Size(max = 10, message = "Class year must not exceed 10 characters")
    private String classYear;
    
    @Column(name = "major_name")
    @Size(max = 255, message = "Major name must not exceed 255 characters")
    private String majorName;

    @Column(name = "number_of_students")
    @NotNull(message = "Number of students is required")
    @Min(value = 1, message = "Number of students must be at least 1")
    @Max(value = 1000, message = "Number of students must not exceed 1000")
    private Integer numberOfStudents;  // Sĩ số sinh viên

    @ManyToOne
    @JoinColumn(name = "faculty_id")
    @NotNull(message = "Faculty is required")
    private Faculty faculty;

    @OneToMany(mappedBy = "major", cascade = CascadeType.ALL)
    private List<Subject> subjects;
}