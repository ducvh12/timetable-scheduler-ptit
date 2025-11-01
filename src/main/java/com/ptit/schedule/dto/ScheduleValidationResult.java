package com.ptit.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO cho kết quả phân tích xung đột thời khóa biểu
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleValidationResult {
    private ConflictResult conflictResult;
    private List<ScheduleEntry> scheduleEntries;
    private String fileName;
    private int totalEntries;
    private long fileSize;
    
    // Computed properties for easier frontend handling
    public boolean hasConflicts() {
        return conflictResult != null && conflictResult.getTotalConflicts() > 0;
    }
    
    public int getRoomConflictCount() {
        return conflictResult != null && conflictResult.getRoomConflicts() != null 
            ? conflictResult.getRoomConflicts().size() : 0;
    }
    
    public int getTeacherConflictCount() {
        return conflictResult != null && conflictResult.getTeacherConflicts() != null 
            ? conflictResult.getTeacherConflicts().size() : 0;
    }
    
    public String getFormattedFileSize() {
        if (fileSize == 0) return "0 Bytes";
        int k = 1024;
        String[] sizes = {"Bytes", "KB", "MB", "GB"};
        int i = (int) Math.floor(Math.log(fileSize) / Math.log(k));
        return String.format("%.2f %s", fileSize / Math.pow(k, i), sizes[i]);
    }
}