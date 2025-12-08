package com.ptit.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomOccupancyResponse {
    private Long id;
    private Long roomId;
    private String roomName; // "404"
    private String building; // "A2"
    private Long semesterId;
    private String semesterName; // "Học kỳ 1"
    private String academicYear; // "2024-2025"
    private Integer dayOfWeek; // 2-7
    private String dayOfWeekName; // "Thứ 2", "Thứ 3"...
    private Integer period; // 1-6
    private String periodName; // "Ca 1", "Ca 2"...
    private String uniqueKey; // "404-A2|5|1"
    private String note;
}
