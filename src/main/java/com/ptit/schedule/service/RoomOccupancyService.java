package com.ptit.schedule.service;

import com.ptit.schedule.dto.BulkCreateRoomOccupancyRequest;
import com.ptit.schedule.dto.RoomOccupancyResponse;
import com.ptit.schedule.dto.RoomWithOccupancyStatus;
import com.ptit.schedule.entity.OccupancyStatus;
import com.ptit.schedule.entity.RoomType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * Lấy tất cả occupied slots của 1 phòng (tất cả semester) (with pagination)
     */
    Page<RoomOccupancyResponse> getOccupanciesByRoom(Long roomId, Pageable pageable);

    /**
     * Lấy tất cả occupied slots của 1 phòng với search và filter (with pagination)
     */
    Page<RoomOccupancyResponse> getOccupanciesByRoomWithFilters(
            Long roomId,
            Long semesterId,
            Integer dayOfWeek,
            Integer period,
            String search,
            Pageable pageable);

    /**
     * Lấy tất cả occupied slots của 1 semester
     */
    List<RoomOccupancyResponse> getOccupanciesBySemester(Long semesterId);

    /**
     * Lấy tất cả occupied slots của 1 semester (with pagination)
     */
    Page<RoomOccupancyResponse> getOccupanciesBySemester(Long semesterId, Pageable pageable);

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

    /**
     * Tạo nhiều room occupancies từ TKB data
     */
    List<RoomOccupancyResponse> bulkCreateOccupancies(BulkCreateRoomOccupancyRequest request);

    /**
     * Lấy danh sách phòng với trạng thái chiếm dụng trong 1 semester
     */
    List<RoomWithOccupancyStatus> getRoomsWithOccupancyStatus(Long semesterId);

    /**
     * Lấy danh sách phòng với trạng thái chiếm dụng trong 1 semester (with
     * pagination)
     */
    Page<RoomWithOccupancyStatus> getRoomsWithOccupancyStatus(Long semesterId, Pageable pageable);

    /**
     * Lấy danh sách phòng với trạng thái chiếm dụng trong 1 semester với search và
     * filter (with pagination)
     */
    Page<RoomWithOccupancyStatus> getRoomsWithOccupancyStatusWithFilters(
            Long semesterId,
            String building,
            RoomType type,
            OccupancyStatus occupancyStatus,
            Integer minCapacity,
            Integer maxCapacity,
            String search,
            Pageable pageable);

    /**
     * Lấy trạng thái chiếm dụng của 1 phòng trong 1 semester
     */
    RoomWithOccupancyStatus getRoomOccupancyStatus(Long roomId, Long semesterId);

    /**
     * Lấy danh sách phòng trống theo thứ và tiết trong semester
     */
    List<RoomWithOccupancyStatus> getAvailableRoomsByDayAndPeriod(
            Long semesterId, Integer dayOfWeek, Integer period);

    /**
     * Lấy danh sách phòng trống theo thứ và tiết trong semester (with pagination)
     */
    Page<RoomWithOccupancyStatus> getAvailableRoomsByDayAndPeriod(
            Long semesterId, Integer dayOfWeek, Integer period, Pageable pageable);
}
