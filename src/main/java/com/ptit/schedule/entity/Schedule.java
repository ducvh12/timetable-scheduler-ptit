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

    // Quan hệ với Subject - lấy thông tin môn học, ngành, học kỳ từ đây
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(name = "class_number")
    private Integer classNumber; // Lớp thứ mấy (1, 2, 3...)

    @Column(name = "student_year")
    private String studentYear; // Khóa (VD: "D21CQCN01-B")

    @Column(name = "major")
    private String major; // Mã ngành (VD: "CNTT", "TT") - lấy từ FE

    @Column(name = "special_system")
    private String specialSystem; // Hệ đặc thù (VD: "Chính quy", "CLC")

    @Column(name = "si_so_mot_lop")
    private Integer siSoMotLop; // Sĩ số một lớp - lấy từ FE

    @Column(name = "room_number")
    private String roomNumber; // Mã phòng

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // User tạo schedule này

    @ManyToOne(fetch = FetchType.EAGER) // EAGER để luôn load template khi query schedule
    @JoinColumn(name = "template_id", nullable = false)
    private TKBTemplate tkbTemplate; // Template được sử dụng để tạo schedule này
    
    // Helper methods để lấy thông tin từ subject
    public String getSubjectCode() {
        return subject != null ? subject.getSubjectCode() : null;
    }
    
    public String getSubjectName() {
        return subject != null ? subject.getSubjectName() : null;
    }
    
    public Semester getSemester() {
        return subject != null ? subject.getSemester() : null;
    }
    
    public String getMajorCode() {
        return subject != null && subject.getMajor() != null 
            ? subject.getMajor().getMajorCode() : null;
    }
}