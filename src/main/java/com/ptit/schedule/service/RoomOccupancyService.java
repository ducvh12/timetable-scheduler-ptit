package com.ptit.schedule.service;

import com.ptit.schedule.dto.RoomOccupancyResponse;
import java.util.List;
import java.util.Map;

public interface RoomOccupancyService {

    /**
     * Lấy tất cả occupied slots của 1 phòng trong 1 semester
     */
    List<RoomOccupancyResponse> getOccupanciesByRoomAndSemester(Long roomId, Long semesterId);

    /**
     * Lấy tất cả occupied slots của 1 phòng (tất cả semester)
     */
    List<RoomOccupancyResponse> getOccupanciesByRoom(Long roomId);

    /**
     * Lấy tất cả occupied slots của 1 semester
     */
    List<RoomOccupancyResponse> getOccupanciesBySemester(Long semesterId);

    /**
     * Lấy thống kê sử dụng phòng theo semester
     */
    Map<String, Object> getRoomUsageStatistics(Long semesterId);

    /**
     * Check xem 1 slot có trống không
     */
    boolean isSlotAvailable(Long roomId, Long semesterId, Integer dayOfWeek, Integer period);

    /**
     * Xóa tất cả room occupancies của 1 semester
     */
    void deleteOccupanciesBySemester(Long semesterId);
}
