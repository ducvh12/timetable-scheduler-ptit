package com.ptit.schedule.controller;

import com.ptit.schedule.dto.BulkCreateRoomOccupancyRequest;
import com.ptit.schedule.dto.PageResponse;
import com.ptit.schedule.dto.RoomOccupancyResponse;
import com.ptit.schedule.dto.RoomWithOccupancyStatus;
import com.ptit.schedule.entity.OccupancyStatus;
import com.ptit.schedule.entity.RoomType;
import com.ptit.schedule.service.RoomOccupancyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/room-occupancies")
@RequiredArgsConstructor
@Tag(name = "Room Occupancy", description = "APIs for viewing room occupancy schedules")
public class RoomOccupancyController {

    private final RoomOccupancyService roomOccupancyService;

    @GetMapping("/room/{roomId}")
    @Operation(summary = "Get all occupancies for a room", description = "Retrieve all time slots when a specific room is occupied across all semesters with pagination, search, and filters")
    public ResponseEntity<PageResponse<RoomOccupancyResponse>> getOccupanciesByRoom(
            @Parameter(description = "Room ID") @PathVariable Long roomId,
            @Parameter(description = "Filter by Semester ID") @RequestParam(required = false) Long semesterId,
            @Parameter(description = "Filter by Day of Week (2-7)") @RequestParam(required = false) Integer dayOfWeek,
            @Parameter(description = "Filter by Period (1-6)") @RequestParam(required = false) Integer period,
            @Parameter(description = "Search in semester name, academic year, or note") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "semesterId") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "desc") String direction) {
        log.info("GET /api/v1/room-occupancies/room/{}?semesterId={}&dayOfWeek={}&period={}&search={}&page={}&size={}",
                roomId, semesterId, dayOfWeek, period, search, page, size);

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<RoomOccupancyResponse> pageData;
        // Use filtered method if any filter is applied
        if (semesterId != null || dayOfWeek != null || period != null || (search != null && !search.trim().isEmpty())) {
            pageData = roomOccupancyService.getOccupanciesByRoomWithFilters(
                    roomId, semesterId, dayOfWeek, period, search, pageable);
        } else {
            pageData = roomOccupancyService.getOccupanciesByRoom(roomId, pageable);
        }

        PageResponse<RoomOccupancyResponse> response = PageResponse.<RoomOccupancyResponse>builder()
                .content(pageData.getContent())
                .pageNum(pageData.getNumber())
                .pageSize(pageData.getSize())
                .total(pageData.getTotalElements())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/room/{roomId}/semester/{semesterId}")
    @Operation(summary = "Get room occupancies in specific semester", description = "Retrieve all time slots when a specific room is occupied in a specific semester")
    public ResponseEntity<List<RoomOccupancyResponse>> getOccupanciesByRoomAndSemester(
            @Parameter(description = "Room ID") @PathVariable Long roomId,
            @Parameter(description = "Semester ID") @PathVariable Long semesterId) {
        log.info("GET /api/v1/room-occupancies/room/{}/semester/{}", roomId, semesterId);

        List<RoomOccupancyResponse> occupancies = roomOccupancyService.getOccupanciesByRoomAndSemester(roomId,
                semesterId);

        return ResponseEntity.ok(occupancies);
    }

    @GetMapping("/semester/{semesterId}")
    @Operation(summary = "Get all occupancies in a semester", description = "Retrieve all room occupancies in a specific semester with pagination")
    public ResponseEntity<PageResponse<RoomOccupancyResponse>> getOccupanciesBySemester(
            @Parameter(description = "Semester ID") @PathVariable Long semesterId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {
        log.info("GET /api/v1/room-occupancies/semester/{}?page={}&size={}", semesterId, page, size);

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<RoomOccupancyResponse> pageData = roomOccupancyService.getOccupanciesBySemester(semesterId, pageable);

        PageResponse<RoomOccupancyResponse> response = PageResponse.<RoomOccupancyResponse>builder()
                .content(pageData.getContent())
                .pageNum(pageData.getNumber())
                .pageSize(pageData.getSize())
                .total(pageData.getTotalElements())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/semester/{semesterId}/statistics")
    @Operation(summary = "Get room usage statistics", description = "Get statistics about room usage in a semester including total rooms used, occupancy by day/period, and top rooms")
    public ResponseEntity<Map<String, Object>> getRoomUsageStatistics(
            @Parameter(description = "Semester ID") @PathVariable Long semesterId) {
        log.info("GET /api/v1/room-occupancies/semester/{}/statistics", semesterId);

        Map<String, Object> statistics = roomOccupancyService.getRoomUsageStatistics(semesterId);

        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/check-availability")
    @Operation(summary = "Check if a time slot is available", description = "Check if a specific room is available at a specific time in a semester")
    public ResponseEntity<Map<String, Object>> checkSlotAvailability(
            @Parameter(description = "Room ID") @RequestParam Long roomId,
            @Parameter(description = "Semester ID") @RequestParam Long semesterId,
            @Parameter(description = "Day of week (2=Monday, 7=Saturday)") @RequestParam Integer dayOfWeek,
            @Parameter(description = "Period (1-6)") @RequestParam Integer period) {
        log.info("GET /api/v1/room-occupancies/check-availability?roomId={}&semesterId={}&dayOfWeek={}&period={}",
                roomId, semesterId, dayOfWeek, period);

        boolean isAvailable = roomOccupancyService.isSlotAvailable(roomId, semesterId, dayOfWeek, period);

        return ResponseEntity.ok(Map.of(
                "roomId", roomId,
                "semesterId", semesterId,
                "dayOfWeek", dayOfWeek,
                "period", period,
                "available", isAvailable,
                "status", isAvailable ? "Phòng trống" : "Phòng đã được sử dụng"));
    }

    @DeleteMapping("/semester/{semesterId}")
    @Operation(summary = "Delete all room occupancies in a semester", description = "Delete all room occupancy records for a specific semester")
    public ResponseEntity<Map<String, Object>> deleteOccupanciesBySemester(
            @Parameter(description = "Semester ID") @PathVariable Long semesterId) {
        log.info("DELETE /api/v1/room-occupancies/semester/{}", semesterId);

        roomOccupancyService.deleteOccupanciesBySemester(semesterId);

        return ResponseEntity.ok(Map.of(
                "message", "Đã xóa thành công tất cả room occupancies của học kỳ",
                "semesterId", semesterId,
                "status", "success"));
    }

    @PostMapping("/bulk-create")
    @Operation(summary = "Bulk create room occupancies from TKB", description = "Create multiple room occupancy records for a semester")
    public ResponseEntity<Map<String, Object>> bulkCreateOccupancies(
            @Valid @RequestBody BulkCreateRoomOccupancyRequest request) {
        log.info("POST /api/v1/room-occupancies/bulk-create for semesterId={} with {} items",
                request.getSemesterId(), request.getItems().size());

        List<RoomOccupancyResponse> created = roomOccupancyService.bulkCreateOccupancies(request);

        return ResponseEntity.ok(Map.of(
                "message", "Đã tạo " + created.size() + " room occupancies thành công",
                "semesterId", request.getSemesterId(),
                "totalRequested", request.getItems().size(),
                "totalCreated", created.size(),
                "totalSkipped", request.getItems().size() - created.size(),
                "data", created,
                "status", "success"));
    }

    @GetMapping("/rooms-status/semester/{semesterId}")
    @Operation(summary = "Get all rooms with occupancy status", description = "Get occupancy status of all rooms in a specific semester with pagination, search, and filters")
    public ResponseEntity<PageResponse<RoomWithOccupancyStatus>> getRoomsWithOccupancyStatus(
            @Parameter(description = "Semester ID") @PathVariable Long semesterId,
            @Parameter(description = "Filter by building (e.g., A1, A2, D3, NT)") @RequestParam(required = false) String building,
            @Parameter(description = "Filter by room type") @RequestParam(required = false) RoomType type,
            @Parameter(description = "Filter by occupancy status (AVAILABLE, UNAVAILABLE, USED)") @RequestParam(required = false) OccupancyStatus occupancyStatus,
            @Parameter(description = "Filter by minimum capacity") @RequestParam(required = false) Integer minCapacity,
            @Parameter(description = "Filter by maximum capacity") @RequestParam(required = false) Integer maxCapacity,
            @Parameter(description = "Search in room name or building") @RequestParam(required = false) String search,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "building") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {
        log.info(
                "GET /api/v1/room-occupancies/rooms-status/semester/{}?building={}&type={}&occupancyStatus={}&search={}&page={}&size={}",
                semesterId, building, type, occupancyStatus, search, page, size);

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<RoomWithOccupancyStatus> pageData;
        // Use filtered method if any filter is applied
        if (building != null || type != null || occupancyStatus != null || minCapacity != null || maxCapacity != null
                || (search != null && !search.trim().isEmpty())) {
            pageData = roomOccupancyService.getRoomsWithOccupancyStatusWithFilters(
                    semesterId, building, type, occupancyStatus, minCapacity, maxCapacity, search, pageable);
        } else {
            pageData = roomOccupancyService.getRoomsWithOccupancyStatus(semesterId, pageable);
        }

        PageResponse<RoomWithOccupancyStatus> response = PageResponse.<RoomWithOccupancyStatus>builder()
                .content(pageData.getContent())
                .pageNum(pageData.getNumber())
                .pageSize(pageData.getSize())
                .total(pageData.getTotalElements())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/room/{roomId}/status/semester/{semesterId}")
    @Operation(summary = "Get room occupancy status", description = "Get detailed occupancy status of a specific room in a semester")
    public ResponseEntity<RoomWithOccupancyStatus> getRoomOccupancyStatus(
            @Parameter(description = "Room ID") @PathVariable Long roomId,
            @Parameter(description = "Semester ID") @PathVariable Long semesterId) {
        log.info("GET /api/v1/room-occupancies/room/{}/status/semester/{}", roomId, semesterId);

        RoomWithOccupancyStatus status = roomOccupancyService.getRoomOccupancyStatus(roomId, semesterId);

        return ResponseEntity.ok(status);
    }

    @GetMapping("/available-rooms")
    @Operation(summary = "Get available rooms at specific time", description = "Get all available rooms at a specific day and period in a semester with pagination")
    public ResponseEntity<PageResponse<RoomWithOccupancyStatus>> getAvailableRoomsByDayAndPeriod(
            @Parameter(description = "Semester ID") @RequestParam Long semesterId,
            @Parameter(description = "Day of week (2-7)") @RequestParam Integer dayOfWeek,
            @Parameter(description = "Period (1-6)") @RequestParam Integer period,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort by field") @RequestParam(defaultValue = "building") String sortBy,
            @Parameter(description = "Sort direction (asc/desc)") @RequestParam(defaultValue = "asc") String direction) {
        log.info("GET /api/v1/room-occupancies/available-rooms?semesterId={}&dayOfWeek={}&period={}&page={}&size={}",
                semesterId, dayOfWeek, period, page, size);

        Sort.Direction sortDirection = direction.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));
        Page<RoomWithOccupancyStatus> pageData = roomOccupancyService
                .getAvailableRoomsByDayAndPeriod(semesterId, dayOfWeek, period, pageable);

        PageResponse<RoomWithOccupancyStatus> response = PageResponse.<RoomWithOccupancyStatus>builder()
                .content(pageData.getContent())
                .pageNum(pageData.getNumber())
                .pageSize(pageData.getSize())
                .total(pageData.getTotalElements())
                .build();

        return ResponseEntity.ok(response);
    }
}
