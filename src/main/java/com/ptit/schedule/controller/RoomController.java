package com.ptit.schedule.controller;

import com.ptit.schedule.dto.ApiResponse;
import com.ptit.schedule.dto.RoomRequest;
import com.ptit.schedule.dto.RoomResponse;
import com.ptit.schedule.dto.RoomStatusUpdateRequest;
import com.ptit.schedule.dto.RoomBulkStatusUpdateRequest;
import com.ptit.schedule.dto.TKBBatchResponse;
import com.ptit.schedule.entity.RoomStatus;
import com.ptit.schedule.entity.RoomType;
import com.ptit.schedule.service.RoomService;
import com.ptit.schedule.service.ScheduleService;
import com.ptit.schedule.service.SubjectRoomMappingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final ScheduleService scheduleService;
    private final SubjectRoomMappingService subjectRoomMappingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAllRooms() {
        List<RoomResponse> rooms = roomService.getAllRooms();
        return ResponseEntity.ok(ApiResponse.<List<RoomResponse>>builder()
                .success(true)
                .message("Lấy danh sách phòng thành công")
                .data(rooms)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable Long id) {
        RoomResponse room = roomService.getRoomById(id);
        return ResponseEntity.ok(ApiResponse.<RoomResponse>builder()
                .success(true)
                .message("Lấy thông tin phòng thành công")
                .data(room)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(@Valid @RequestBody RoomRequest roomRequest) {
        RoomResponse room = roomService.createRoom(roomRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<RoomResponse>builder()
                        .success(true)
                        .message("Tạo phòng mới thành công")
                        .data(room)
                        .build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest roomRequest) {
        RoomResponse room = roomService.updateRoom(id, roomRequest);
        return ResponseEntity.ok(ApiResponse.<RoomResponse>builder()
                .success(true)
                .message("Cập nhật thông tin phòng thành công")
                .data(room)
                .build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa phòng thành công")
                .build());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoomStatus(
            @PathVariable Long id,
            @Valid @RequestBody RoomStatusUpdateRequest statusRequest) {
        RoomResponse room = roomService.updateRoomStatus(id, statusRequest);
        return ResponseEntity.ok(ApiResponse.<RoomResponse>builder()
                .success(true)
                .message("Cập nhật trạng thái phòng thành công")
                .data(room)
                .build());
    }

    @PatchMapping("/bulk-status")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> bulkUpdateRoomStatus(
            @Valid @RequestBody RoomBulkStatusUpdateRequest request) {
        List<RoomResponse> updatedRooms = roomService.bulkUpdateRoomStatus(request);
        return ResponseEntity.ok(ApiResponse.<List<RoomResponse>>builder()
                .success(true)
                .message("Cập nhật trạng thái " + updatedRooms.size() + " phòng thành công")
                .data(updatedRooms)
                .build());
    }

    @GetMapping("/building/{building}")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByBuilding(@PathVariable String building) {
        List<RoomResponse> rooms = roomService.getRoomsByBuilding(building);
        return ResponseEntity.ok(ApiResponse.<List<RoomResponse>>builder()
                .success(true)
                .message("Lấy danh sách phòng theo tòa nhà thành công")
                .data(rooms)
                .build());
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByStatus(@PathVariable RoomStatus status) {
        List<RoomResponse> rooms = roomService.getRoomsByStatus(status);
        return ResponseEntity.ok(ApiResponse.<List<RoomResponse>>builder()
                .success(true)
                .message("Lấy danh sách phòng theo trạng thái thành công")
                .data(rooms)
                .build());
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByType(@PathVariable RoomType type) {
        List<RoomResponse> rooms = roomService.getRoomsByType(type);
        return ResponseEntity.ok(ApiResponse.<List<RoomResponse>>builder()
                .success(true)
                .message("Lấy danh sách phòng theo loại thành công")
                .data(rooms)
                .build());
    }

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAvailableRoomsWithCapacity(
            @RequestParam Integer capacity) {
        List<RoomResponse> rooms = roomService.getAvailableRoomsWithCapacity(capacity);
        return ResponseEntity.ok(ApiResponse.<List<RoomResponse>>builder()
                .success(true)
                .message("Lấy danh sách phòng trống có đủ sức chứa thành công")
                .data(rooms)
                .build());
    }

    @GetMapping("/building/{building}/status/{status}")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByBuildingAndStatus(
            @PathVariable String building,
            @PathVariable RoomStatus status) {
        List<RoomResponse> rooms = roomService.getRoomsByBuildingAndStatus(building, status);
        return ResponseEntity.ok(ApiResponse.<List<RoomResponse>>builder()
                .success(true)
                .message("Lấy danh sách phòng theo tòa nhà và trạng thái thành công")
                .data(rooms)
                .build());
    }

    @GetMapping("/type/{type}/status/{status}")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getRoomsByTypeAndStatus(
            @PathVariable RoomType type,
            @PathVariable RoomStatus status) {
        List<RoomResponse> rooms = roomService.getRoomsByTypeAndStatus(type, status);
        return ResponseEntity.ok(ApiResponse.<List<RoomResponse>>builder()
                .success(true)
                .message("Lấy danh sách phòng theo loại và trạng thái thành công")
                .data(rooms)
                .build());
    }

    @PostMapping("/save-results")
    public ResponseEntity<Map<String, Object>> saveResults(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String academicYear,
            @RequestParam(required = false) String semester) {
        try {
            scheduleService.commitSessionToRedis(userId, academicYear, semester);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đã lưu kết quả TKB vào hệ thống!");
            response.put("status", "success");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Lỗi khi lưu kết quả: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @GetMapping("/occupied-info")
    public ResponseEntity<Map<String, Object>> getOccupiedRoomsInfo() {
        try {
            Map<String, Integer> info = scheduleService.getOccupiedRoomsInfo();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("session", info.get("session"));
            response.put("global", info.get("global"));
            response.put("total", info.get("total"));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Lỗi khi lấy thông tin phòng: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetOccupiedRooms() {
        try {
            Map<String, Integer> beforeInfo = scheduleService.getOccupiedRoomsInfo();

            scheduleService.resetOccupiedRooms();

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Đã reset phòng đã sử dụng! Tất cả phòng có thể sử dụng lại.");
            response.put("status", "success");
            response.put("clearedRooms", beforeInfo.get("total"));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("message", "Lỗi khi reset phòng: " + e.getMessage());
            errorResponse.put("status", "error");
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    // Subject-Room Mapping endpoints
    @GetMapping("/subject-room-mappings")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSubjectRoomMappings() {
        Map<String, String> mappings = subjectRoomMappingService.getAllMappings();
        return ResponseEntity.ok(ApiResponse.<Map<String, String>>builder()
                .success(true)
                .message("Lấy danh sách mapping môn-phòng thành công")
                .data(mappings)
                .build());
    }

    @DeleteMapping("/subject-room-mappings")
    public ResponseEntity<ApiResponse<Void>> clearSubjectRoomMappings() {
        subjectRoomMappingService.clearMappings();
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa tất cả mapping môn-phòng thành công")
                .build());
    }

    @DeleteMapping("/subject-room-mappings/{maMon}")
    public ResponseEntity<ApiResponse<Void>> clearSubjectRoomMapping(@PathVariable String maMon) {
        subjectRoomMappingService.clearSubject(maMon);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Xóa mapping cho môn " + maMon + " thành công")
                .build());
    }

    // Room Assignment for TKB
    @PostMapping("/assign-rooms")
    public ResponseEntity<ApiResponse<TKBBatchResponse>> assignRoomsToSchedule(
            @RequestBody TKBBatchResponse scheduleWithoutRooms,
            @RequestParam String academicYear,
            @RequestParam String semester) {

        if (scheduleWithoutRooms == null || scheduleWithoutRooms.getItems() == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<TKBBatchResponse>builder()
                            .success(false)
                            .message("Dữ liệu thời khóa biểu không hợp lệ")
                            .build());
        }

        if (academicYear == null || semester == null) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<TKBBatchResponse>builder()
                            .success(false)
                            .message("Năm học và học kỳ không được rỗng")
                            .build());
        }

        TKBBatchResponse response = roomService.assignRoomsToSchedule(
                scheduleWithoutRooms, academicYear, semester);

        return ResponseEntity.ok(ApiResponse.<TKBBatchResponse>builder()
                .success(true)
                .message("Gán phòng học cho thời khóa biểu thành công")
                .data(response)
                .build());
    }
}
