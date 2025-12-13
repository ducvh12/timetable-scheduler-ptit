package com.ptit.schedule.dto;

import com.ptit.schedule.entity.OccupancyStatus;
import com.ptit.schedule.entity.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomWithOccupancyStatus {
    private Long id;
    private String name;
    private Integer capacity;
    private String building;
    private RoomType type;
    private String typeDisplayName;

    // Occupancy status for specific semester
    private Long semesterId;
    private String semesterName;
    private String academicYear;

    // Occupancy statistics
    private Integer totalOccupiedSlots; // Tổng số slot bị chiếm
    private Integer totalAvailableSlots; // Tổng số slot trống (6 days * 6 periods = 36 - occupied)
    private Double occupancyRate; // Tỷ lệ % sử dụng
    private OccupancyStatus occupancyStatus; // AVAILABLE, UNAVAILABLE, USED

    // Chi tiết các slot bị chiếm
    private List<OccupiedSlot> occupiedSlots;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OccupiedSlot {
        private Integer dayOfWeek; // 2-7
        private String dayName; // "Thứ 2"
        private Integer period; // 1-6
        private String periodName; // "Ca 1"
        private String note; // Môn học chiếm slot này
    }
}
