package com.ptit.schedule.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Table(name = "subjects")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subject_code")
    private String subjectCode;          // Mã môn học

    @Column(name = "subject_name")
    private String subjectName;

    @Column(name = "students_per_class")
    private Integer studentsPerClass;    // Sĩ số mỗi lớp học phần

    @Column(name = "number_of_classes")
    private Integer numberOfClasses;     // Số lớp học phần

    private Integer credits;            // Số tín chỉ

    @Column(name = "theory_hours")  // Số tiết lý thuyết
    private Integer theoryHours;

    @Column(name = "exercise_hours")  // Số tiết bài tập
    private Integer exerciseHours;

    @Column(name = "project_hours")  // Số tiết btl
    private Integer projectHours;

    @Column(name = "lab_hours") // Số tiết thực hành
    private Integer labHours;

    @Column(name = "self_study_hours")  // Số tiết tự học
    private Integer selfStudyHours;

    private String department; // Bộ môn

    @Column(name = "exam_format", nullable = true) // Hình thức thi
    private String examFormat;

    @Column(name = "program_type", nullable = true)
    private String programType; // Loại chương trình (Ví dụ: Chính quy, CLC,..)

    @ManyToOne
    @JoinColumn(name = "major_id")
    private Major major;

    @Column(name = "semester", nullable = true)
    private String semester;

}