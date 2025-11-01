package com.ptit.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConflictResult {
    
    private List<RoomConflict> roomConflicts;
    private List<TeacherConflict> teacherConflicts;
    private int totalConflicts;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomConflict {
        private String room;
        private ScheduleEntry.TimeSlot timeSlot;  // Đối tượng TimeSlot đầy đủ
        private List<ScheduleEntry> conflictingSchedules;
        private List<String> conflictWeeks; // Danh sách các tuần bị xung đột
        
        public String getConflictDescription() {
            if (conflictWeeks != null && !conflictWeeks.isEmpty()) {
                String weeksStr = String.join(", ", conflictWeeks);
                return String.format("Phòng %s bị trùng vào %s (%s) - Kíp %s - Tiết %s (%s tiết) tại các tuần: %s", 
                    room, 
                    timeSlot.getDayOfWeek(),
                    timeSlot.getDayOfWeek(),
                    timeSlot.getShift(), 
                    timeSlot.getStartPeriod(), 
                    timeSlot.getNumberOfPeriods(),
                    weeksStr);
            }
            return String.format("Phòng %s bị trùng vào %s", room, timeSlot.getDisplayInfo());
        }
        
        public String getConflictKey() {
            // Key để group conflicts: room-dayOfWeek-shift-startPeriod-numberOfPeriods
            return String.format("%s-%s-%s-%s-%s", 
                room, timeSlot.getDayOfWeek(), timeSlot.getShift(), 
                timeSlot.getStartPeriod(), timeSlot.getNumberOfPeriods());
        }
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherConflict {
        private String teacherId;
        private String teacherName;
        private ScheduleEntry.TimeSlot timeSlot;  // Đối tượng TimeSlot đầy đủ
        private List<ScheduleEntry> conflictingSchedules;
        private List<String> conflictWeeks; // Danh sách các tuần bị xung đột
        
        public String getConflictDescription() {
            if (conflictWeeks != null && !conflictWeeks.isEmpty()) {
                String weeksStr = String.join(", ", conflictWeeks);
                return String.format("Giảng viên %s (%s) bị trùng lịch vào %s (%s) - Kíp %s - Tiết %s (%s tiết) tại các tuần: %s", 
                    teacherName, teacherId,
                    timeSlot.getDayOfWeek(),
                    timeSlot.getDayOfWeek(),
                    timeSlot.getShift(), 
                    timeSlot.getStartPeriod(), 
                    timeSlot.getNumberOfPeriods(),
                    weeksStr);
            }
            return String.format("Giảng viên %s (%s) bị trùng lịch vào %s", 
                    teacherName, teacherId, timeSlot.getDisplayInfo());
        }
        
        public String getConflictKey() {
            // Key để group conflicts: teacherId-dayOfWeek-shift-startPeriod-numberOfPeriods
            return String.format("%s-%s-%s-%s-%s", 
                teacherId, timeSlot.getDayOfWeek(), timeSlot.getShift(), 
                timeSlot.getStartPeriod(), timeSlot.getNumberOfPeriods());
        }
    }
    
    public int getTotalConflicts() {
        return (roomConflicts != null ? roomConflicts.size() : 0) + 
               (teacherConflicts != null ? teacherConflicts.size() : 0);
    }
}