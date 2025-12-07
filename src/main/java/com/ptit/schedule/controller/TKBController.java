package com.ptit.schedule.controller;

import com.ptit.schedule.dto.*;
import com.ptit.schedule.entity.User;
import com.ptit.schedule.exception.InvalidDataException;
import com.ptit.schedule.service.TimetableSchedulingService;
import com.ptit.schedule.service.DataLoaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tkb")
@RequiredArgsConstructor
@Tag(name = "TKB Controller", description = "APIs for timetable generation")
public class TKBController {

    private final TimetableSchedulingService timetableSchedulingService;
    private final DataLoaderService dataLoaderService;

    @Operation(summary = "Generate TKB for batch subjects", description = "Tạo thời khóa biểu cho nhiều môn học")
    @PostMapping("/generate-batch")
    public ResponseEntity<TKBBatchResponse> generateTKBBatch(@RequestBody TKBBatchRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new InvalidDataException("Danh sách môn học không được rỗng");
        }
        
        // Lấy userId từ authentication nếu chưa có trong request
        if (request.getUserId() == null) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User) {
                User currentUser = (User) authentication.getPrincipal();
                request.setUserId(currentUser.getId());
            }
        }
        
        TKBBatchResponse response = timetableSchedulingService.simulateExcelFlowBatch(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Health check", description = "Kiểm tra trạng thái TKB controller")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("TKB Controller is OK");
    }

    @Operation(summary = "Test data loading", description = "Test load template data")
    @GetMapping("/test-data")
    public ResponseEntity<Map<String, Object>> testData() {
        var templateData = dataLoaderService.loadTemplateData();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("template_rows_count", templateData.size());
        response.put("message", "Data loaded successfully");

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reset TKB state", description = "Reset global scheduling state")
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetState() {
        timetableSchedulingService.resetState();
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "TKB state reset successfully");
        return ResponseEntity.ok(response);
    }



    @Operation(summary = "Debug common subject room assignment", description = "Test room assignment for common subjects")
    @PostMapping("/debug-common-subject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugCommonSubject() {
        TKBRequest commonSubjectRequest = TKBRequest.builder()
                .ma_mon("SKD1102")
                .ten_mon("Kỹ năng làm việc nhóm")
                .sotiet(30)
                .siso(100)
                .siso_mot_lop(50)
                .solop(2)
                .nganh("Chung")
                .subject_type("general")
                .student_year("2024")
                .he_dac_thu("")
                .build();

        TKBBatchRequest batchRequest = TKBBatchRequest.builder()
                .items(Collections.singletonList(commonSubjectRequest))
                .build();

        TKBBatchResponse response = timetableSchedulingService.simulateExcelFlowBatch(batchRequest);

        boolean hasRoomAssigned = response.getItems().stream()
                .flatMap(item -> item.getRows().stream())
                .anyMatch(row -> row.getPhong() != null);

        Map<String, Object> result = new HashMap<>();
        result.put("request", commonSubjectRequest);
        result.put("response", response);
        result.put("has_room_assigned", hasRoomAssigned);
        result.put("total_rows", response.getTotalRows());

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Debug common subject completed")
                .data(result)
                .build());
    }

    @Operation(summary = "Import data lịch mẫu", description = "Import file Excel chứa dữ liệu lịch mẫu và ghi đè vào real.json")
    @PostMapping("/import-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importDataTemplate(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        if (file.isEmpty()) {
            throw new InvalidDataException("File không được để trống");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new InvalidDataException("File phải có định dạng Excel (.xlsx hoặc .xls)");
        }

        dataLoaderService.importDataFromExcel(file);

        Map<String, Object> result = new HashMap<>();
        result.put("filename", filename);
        result.put("message", "Đã import dữ liệu và cập nhật real.json thành công");

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Import dữ liệu thành công")
                .data(result)
                .build());
    }

    @Operation(summary = "Save lastSlotIdx to Redis", description = "Lưu lastSlotIdx vào Redis với key (userId, academicYear, semester)")
    @PostMapping("/save-last-slot-idx")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveLastSlotIdxToRedis(
            @RequestParam Long userId,
            @RequestParam String academicYear,
            @RequestParam String semester) {
        
        timetableSchedulingService.commitSessionToRedis(userId, academicYear, semester);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("academicYear", academicYear);
        result.put("semester", semester);
        result.put("message", "Đã lưu lastSlotIdx vào Redis");

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Lưu lastSlotIdx thành công")
                .data(result)
                .build());
    }

    @Operation(summary = "Reset lastSlotIdx in Redis", description = "Xóa lastSlotIdx trong Redis cho user, năm học, học kỳ")
    @DeleteMapping("/reset-last-slot-idx-redis")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetLastSlotIdxRedis(
            @RequestParam Long userId,
            @RequestParam String academicYear,
            @RequestParam String semester) {
        
        timetableSchedulingService.resetOccupiedRoomsRedis(userId, academicYear, semester);

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("academicYear", academicYear);
        result.put("semester", semester);
        result.put("message", "Đã reset lastSlotIdx trong Redis");

        return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                .success(true)
                .message("Reset lastSlotIdx thành công")
                .data(result)
                .build());
    }
}
