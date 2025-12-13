package com.ptit.schedule.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateRoomOccupancyRequest {

    @NotNull(message = "Semester ID không được để trống")
    private Long semesterId;

    @NotEmpty(message = "Danh sách occupancy items không được để trống")
    private List<OccupancyItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OccupancyItem {
        @NotNull(message = "Room ID không được để trống")
        private Long roomId;

        @NotNull(message = "Day of week không được để trống")
        private Integer dayOfWeek; // 2-7 (Thứ 2 đến Thứ 7)

        @NotNull(message = "Kip không được để trống")
        private Integer kip; // 1-4 (Kíp 1-4)

        private String subjectCode; // Mã môn học (optional, để ghi chú)
        private String subjectName; // Tên môn học (optional)
    }
}
