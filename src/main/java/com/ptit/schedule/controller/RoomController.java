package com.ptit.schedule.controller;

import com.ptit.schedule.dto.ApiResponse;
import com.ptit.schedule.dto.RoomRequest;
import com.ptit.schedule.dto.RoomResponse;
import com.ptit.schedule.service.RoomService;
import com.ptit.schedule.service.TimetableSchedulingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Tag(name = "Room Management", description = "API quản lý phòng học")
public class RoomController {
    
    private final RoomService roomService;
    private final TimetableSchedulingService timetableSchedulingService;
    
    @Operation(summary = "Tạo phòng học mới", description = "Tạo phòng học mới trong hệ thống")
    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(@RequestBody RoomRequest request) {
        try {
            RoomResponse response = roomService.createRoom(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, "Tạo phòng học thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Lỗi tạo phòng học: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Lấy danh sách phòng học", description = "Lấy danh sách tất cả phòng học")
    @GetMapping
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAllRooms() {
        try {
            List<RoomResponse> rooms = roomService.getAllRooms();
            return ResponseEntity.ok(ApiResponse.success(rooms, "Lấy danh sách phòng học thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Lỗi lấy danh sách phòng học: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Lấy phòng học theo ID", description = "Lấy thông tin phòng học theo ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getRoomById(@PathVariable String id) {
        try {
            RoomResponse room = roomService.getRoomById(id);
            return ResponseEntity.ok(ApiResponse.success(room, "Lấy thông tin phòng học thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Lỗi lấy thông tin phòng học: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Cập nhật phòng học", description = "Cập nhật thông tin phòng học")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> updateRoom(@PathVariable String id, @RequestBody RoomRequest request) {
        try {
            RoomResponse response = roomService.updateRoom(id, request);
            return ResponseEntity.ok(ApiResponse.success(response, "Cập nhật phòng học thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Lỗi cập nhật phòng học: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Xóa phòng học", description = "Xóa phòng học khỏi hệ thống")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteRoom(@PathVariable String id) {
        try {
            roomService.deleteRoom(id);
            return ResponseEntity.ok(ApiResponse.success("Phòng học đã được xóa thành công!", "Xóa phòng học thành công!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Lỗi xóa phòng học: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Health check", description = "Kiểm tra trạng thái controller")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Room Controller is OK");
    }
    
    @Operation(summary = "Confirm and save current session results", 
               description = "API được gọi khi user bấm nút 'Thêm vào kết quả' để commit session occupied rooms vào global")
    @PostMapping("/save-results")
    public ResponseEntity<Map<String, Object>> saveResults() {
        try {
            // Commit session occupied rooms to global (permanent storage)
            timetableSchedulingService.commitSessionToGlobal();
            
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
    
    @Operation(summary = "Reset occupied rooms", 
               description = "Reset tất cả phòng đã sử dụng (cả session và global) - Cho phép sử dụng lại tất cả phòng")
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetOccupiedRooms() {
        try {
            // Get info before reset
            Map<String, Integer> beforeInfo = timetableSchedulingService.getOccupiedRoomsInfo();
            
            // Reset both session and global occupied rooms
            timetableSchedulingService.resetOccupiedRooms();
            
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
    
    @Operation(summary = "Get occupied rooms info", 
               description = "Lấy thông tin về số phòng đã sử dụng (session và global)")
    @GetMapping("/occupied-info")
    public ResponseEntity<Map<String, Object>> getOccupiedRoomsInfo() {
        try {
            Map<String, Integer> info = timetableSchedulingService.getOccupiedRoomsInfo();
            
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
}
