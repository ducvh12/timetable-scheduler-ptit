package com.ptit.schedule.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Table(name = "semesters", 
    uniqueConstraints = @UniqueConstraint(columnNames = {"semester_name", "academic_year"}))
    // Không cần thêm index vì uniqueConstraint đã tự động tạo index
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Semester {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "semester_name", nullable = false)
    private String semesterName;  // Tên học kỳ (VD: Học kỳ 1)

    @Column(name = "academic_year", nullable = false)
    private String academicYear;  // Năm học (VD: 2024-2025)

    @Column(name = "start_date")
    private LocalDate startDate;  // Ngày bắt đầu

    @Column(name = "end_date")
    private LocalDate endDate;  // Ngày kết thúc

    @Column(name = "is_active")
    private Boolean isActive;  // Học kỳ hiện tại đang hoạt động

    @Column(name = "description")
    private String description;  // Mô tả

    // Quan hệ 1-nhiều với Subject
    @OneToMany(mappedBy = "semester", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subject> subjects;
}
