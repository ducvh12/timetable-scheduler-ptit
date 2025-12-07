package com.ptit.schedule.controller;

import com.ptit.schedule.dto.RoomOccupancyResponse;
import com.ptit.schedule.service.RoomOccupancyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    @Operation(summary = "Get all occupancies for a room", description = "Retrieve all time slots when a specific room is occupied across all semesters")
    public ResponseEntity<List<RoomOccupancyResponse>> getOccupanciesByRoom(
            @Parameter(description = "Room ID") @PathVariable Long roomId) {
        log.info("GET /api/v1/room-occupancies/room/{}", roomId);

        List<RoomOccupancyResponse> occupancies = roomOccupancyService.getOccupanciesByRoom(roomId);

        return ResponseEntity.ok(occupancies);
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
    @Operation(summary = "Get all occupancies in a semester", description = "Retrieve all room occupancies in a specific semester")
    public ResponseEntity<List<RoomOccupancyResponse>> getOccupanciesBySemester(
            @Parameter(description = "Semester ID") @PathVariable Long semesterId) {
        log.info("GET /api/v1/room-occupancies/semester/{}", semesterId);

        List<RoomOccupancyResponse> occupancies = roomOccupancyService.getOccupanciesBySemester(semesterId);

        return ResponseEntity.ok(occupancies);
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
}
