package com.ptit.schedule.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "schedules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "class_number")
    private Integer classNumber; // Lớp

    @Column(name = "subject_id")
    private String subjectId; // Mã môn

    @Column(name = "subject_name")
    private String subjectName; // Tên môn

    @Column(name = "student_year")
    private String studentYear; // Khóa

    @Column(name = "major")
    private String major; // Ngành

    @Column(name = "special_system")
    private String specialSystem; // Hệ đặc thù

    @Column(name = "day_of_week")
    private Integer dayOfWeek; // Thứ

    @Column(name = "session_number")
    private Integer sessionNumber; // Kíp

    @Column(name = "start_period")
    private Integer startPeriod; // Tiết BD

    @Column(name = "period_length")
    private Integer periodLength; // L

    @Column(name = "room_number")
    private String roomNumber; // Mã phòng

    @Column(name = "academic_year")
    private String academicYear; // Năm học (VD: "2023-2024")

    @Column(name = "semester")
    private String semester; // Học kỳ (VD: "1", "2", "hè")

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // User tạo schedule này

    // Tuần 1 đến tuần 18: mỗi tuần là một cột kiểu String (lưu "x" hoặc "")
    @Column(name = "week_1")
    private String week1;
    @Column(name = "week_2")
    private String week2;
    @Column(name = "week_3")
    private String week3;
    @Column(name = "week_4")
    private String week4;
    @Column(name = "week_5")
    private String week5;
    @Column(name = "week_6")
    private String week6;
    @Column(name = "week_7")
    private String week7;
    @Column(name = "week_8")
    private String week8;
    @Column(name = "week_9")
    private String week9;
    @Column(name = "week_10")
    private String week10;
    @Column(name = "week_11")
    private String week11;
    @Column(name = "week_12")
    private String week12;
    @Column(name = "week_13")
    private String week13;
    @Column(name = "week_14")
    private String week14;
    @Column(name = "week_15")
    private String week15;
    @Column(name = "week_16")
    private String week16;
    @Column(name = "week_17")
    private String week17;
    @Column(name = "week_18")
    private String week18;
}