package com.ptit.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleEntry {
    
    private String subjectCode;      // Mã môn học
    private String subjectName;      // Tên môn học  
    private String teacherId;        // Mã giảng viên
    private String teacherName;      // Tên giảng viên
    private String room;             // Phòng học
    private String building;
    private String classGroup;       // Nhóm lớp
    private int studentCount;        // Số sinh viên
    
    // Time slots - represented as day-period combinations where 'x' appears
    private List<TimeSlot> timeSlots;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlot {
        private String date;           // Tuần: "Tuần 1", "Tuần 2", etc.
        private String dayOfWeek;      // Tuần: "Tuần 1", "Tuần 2", etc. (đồng nhất)
        private String shift;          // Kíp: "1", "2", "3", etc.
        private String startPeriod;    // Tiết bắt đầu: "1", "2", "3", etc.
        private String numberOfPeriods; // Số tiết: "1", "2", "3", etc.
        
        public String getSlotKey() {
            // Key để detect conflict: tuần-thứ-kíp-tiết bắt đầu-số tiết
            return date + "-" + dayOfWeek + "-" + shift + "-" + startPeriod + "-" + numberOfPeriods;
        }
        
        public String getDisplayInfo() {
            return String.format("%s (%s) - Kíp %s - Tiết %s (%s tiết)", 
                date, dayOfWeek, shift, startPeriod, numberOfPeriods);
        }
    }
    
    public String getDisplayInfo() {
        return String.format("%s - %s (%s)", subjectCode, subjectName, teacherName);
    }
}