package com.ptit.schedule.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
    private String id;

    @Column(name = "class_number", nullable = false)
    @NotNull(message = "Class number is required")
    @Min(value = 1, message = "Class number must be at least 1")
    private Integer classNumber;  // lop

    @Column(name = "subject_id", nullable = false)
    @NotBlank(message = "Subject ID is required")
    @Size(max = 50, message = "Subject ID must not exceed 50 characters")
    private String subjectId;  // ma_mon

    @Column(name = "subject_name", nullable = false)
    @NotBlank(message = "Subject name is required")
    @Size(max = 255, message = "Subject name must not exceed 255 characters")
    private String subjectName;  // ten_mon

    @Column(name = "session_number")
    private Integer sessionNumber;  // kip

    @Column(name = "day_of_week")
    @Min(value = 1, message = "Day of week must be between 1-7")
    @Max(value = 7, message = "Day of week must be between 1-7")
    private Integer dayOfWeek;  // thu

    @Column(name = "start_period")
    @Min(value = 1, message = "Start period must be at least 1")
    @Max(value = 12, message = "Start period must not exceed 12")
    private Integer startPeriod;  // tiet_bd

    @Column(name = "period_length")
    @Min(value = 1, message = "Period length must be at least 1")
    @Max(value = 6, message = "Period length must not exceed 6")
    private Integer periodLength;  // L

    @Column(name = "room_number")
    @Size(max = 50, message = "Room number must not exceed 50 characters")
    private String roomNumber;  // phong

    @Column(name = "student_year")
    @Size(max = 10, message = "Student year must not exceed 10 characters")
    private String studentYear;  // student_year

    @Column(name = "special_system")
    @Size(max = 20, message = "Special system must not exceed 20 characters")
    private String specialSystem;  // he_dac_thu

    @Column(name = "major")
    @Size(max = 50, message = "Major must not exceed 50 characters")
    private String major;  // nganh

    @Column(name = "note")
    @Size(max = 500, message = "Note must not exceed 500 characters")
    private String note;  // N

    // Additional fields for TKB generation
    @Column(name = "total_periods")
    private Integer totalPeriods;  // sotiet - Total periods for this subject

    @Column(name = "remaining_periods")
    private Integer remainingPeriods;  // ai - Remaining periods to schedule

    @Column(name = "periods_used")
    private Integer periodsUsed;  // ah - Periods used in this session

    @Column(name = "week_schedule", length = 1000)
    private String weekSchedule;  // JSON string of weeks 1-18 (O_to_AG)

    @Column(name = "session_type")
    private String sessionType;  // 'sang' or 'chieu'

    @Column(name = "slot_index")
    private Integer slotIndex;  // Index in rotating slots

    // TODO: Uncomment when Room entity is ready
    // @ManyToOne
    // @JoinColumn(name = "room_id")
    // private Room room;

    @ManyToOne
    @JoinColumn(name = "subject_entity_id")
    private Subject subject;

    @ManyToOne
    @JoinColumn(name = "major_entity_id")
    private Major majorEntity;
}
