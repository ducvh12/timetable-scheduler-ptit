package com.ptit.schedule.controller;

import com.ptit.schedule.dto.*;
import com.ptit.schedule.service.TimetableSchedulingService;
import com.ptit.schedule.service.DataLoaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/tkb")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "TKB Controller", description = "APIs for timetable generation")
public class TKBController {

    private final TimetableSchedulingService timetableSchedulingService;
    private final DataLoaderService dataLoaderService;

    @Operation(summary = "Generate TKB for single subject", description = "Tạo thời khóa biểu cho một môn học")
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<TKBBatchResponse>> generateTKB(@RequestBody TKBRequest request) {
        try {
            log.info("Generating TKB for subject: {}", request.getSubjectName());
            // Convert single request to batch for unified processing
            TKBBatchRequest batchRequest = TKBBatchRequest.builder()
                    .items(Collections.singletonList(request))
                    .build();
            TKBBatchResponse response = timetableSchedulingService.simulateExcelFlowBatch(batchRequest);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Error generating TKB: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Lỗi tạo TKB: " + e.getMessage()));
        }
    }

    @Operation(summary = "Generate TKB for batch subjects", description = "Tạo thời khóa biểu cho nhiều môn học")
    @PostMapping("/generate-batch")
    public ResponseEntity<TKBBatchResponse> generateTKBBatch(@RequestBody TKBBatchRequest request) {
        try {
            log.info("Generating TKB batch with {} items", request.getItems().size());
            TKBBatchResponse response = timetableSchedulingService.simulateExcelFlowBatch(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error generating TKB batch: {}", e.getMessage(), e);
            TKBBatchResponse errorResponse = TKBBatchResponse.builder()
                    .items(Collections.emptyList())
                    .totalRows(0)
                    .totalClasses(0)
                    .lastSlotIdx(0)
                    .error("Lỗi tạo TKB batch: " + e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @Operation(summary = "Health check", description = "Kiểm tra trạng thái TKB controller")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("TKB Controller is OK");
    }

    @Operation(summary = "Test data loading", description = "Test load template data")
    @GetMapping("/test-data")
    public ResponseEntity<Map<String, Object>> testData() {
        try {
            log.info("Testing data loading...");
            var templateData = dataLoaderService.loadTemplateData();
            log.info("Loaded {} template rows", templateData.size());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("template_rows_count", templateData.size());
            response.put("message", "Data loaded successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error testing data loading: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @Operation(summary = "Reset TKB state", description = "Reset global scheduling state")
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetState() {
        try {
            timetableSchedulingService.resetState();
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "TKB state reset successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error resetting state: {}", e.getMessage(), e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @Operation(summary = "Reset lastSlotIdx", description = "Reset lastSlotIdx về -1 và lưu vào file")
    @PostMapping("/reset-last-slot-idx")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resetLastSlotIdx() {
        try {
            timetableSchedulingService.resetLastSlotIdx();

            Map<String, Object> result = new HashMap<>();
            result.put("lastSlotIdx", -1);
            result.put("message", "Đã reset lastSlotIdx về -1 và lưu vào file");

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Reset lastSlotIdx thành công")
                    .data(result)
                    .build());
        } catch (Exception e) {
            log.error("Error resetting lastSlotIdx: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Lỗi reset lastSlotIdx: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Debug common subject room assignment", description = "Test room assignment for common subjects")
    @PostMapping("/debug-common-subject")
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugCommonSubject() {
        try {
            log.info("=== DEBUG COMMON SUBJECT ROOM ASSIGNMENT ===");

            // Create a test TKBRequest for a common subject
            TKBRequest commonSubjectRequest = TKBRequest.builder()
                    .ma_mon("SKD1102")
                    .ten_mon("Kỹ năng làm việc nhóm")
                    .sotiet(30)
                    .siso(100)
                    .siso_mot_lop(50)
                    .solop(2)
                    .nganh("Chung")
                    .subject_type("general") // Test with general type for common subjects
                    .student_year("2024")
                    .he_dac_thu("") // empty for common subjects
                    .build();

            log.info("Testing with common subject request: {}", commonSubjectRequest);

            // Convert to batch request
            TKBBatchRequest batchRequest = TKBBatchRequest.builder()
                    .items(Collections.singletonList(commonSubjectRequest))
                    .build();

            // Generate TKB
            TKBBatchResponse response = timetableSchedulingService.simulateExcelFlowBatch(batchRequest);

            boolean hasRoomAssigned = response.getItems().stream()
                    .flatMap(item -> item.getRows().stream())
                    .anyMatch(row -> row.getPhong() != null);

            Map<String, Object> result = new HashMap<>();
            result.put("request", commonSubjectRequest);
            result.put("response", response);
            result.put("has_room_assigned", hasRoomAssigned);
            result.put("total_rows", response.getTotalRows());

            log.info("=== DEBUG COMMON SUBJECT RESULT ===");
            log.info("Total rows generated: {}", response.getTotalRows());
            log.info("Has room assigned: {}", hasRoomAssigned);

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Debug common subject completed")
                    .data(result)
                    .build());

        } catch (Exception e) {
            log.error("Error debugging common subject: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Lỗi debug common subject: " + e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Import data lịch mẫu", description = "Import file Excel chứa dữ liệu lịch mẫu và ghi đè vào real.json")
    @PostMapping("/import-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importDataTemplate(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            log.info("Importing data template file: {}", file.getOriginalFilename());

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Map<String, Object>>builder()
                                .success(false)
                                .message("File không được để trống")
                                .build());
            }

            String filename = file.getOriginalFilename();
            if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<Map<String, Object>>builder()
                                .success(false)
                                .message("File phải có định dạng Excel (.xlsx hoặc .xls)")
                                .build());
            }

            // Call service to process file and update real.json
            dataLoaderService.importDataFromExcel(file);

            Map<String, Object> result = new HashMap<>();
            result.put("filename", filename);
            result.put("message", "Đã import dữ liệu và cập nhật real.json thành công");

            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .success(true)
                    .message("Import dữ liệu thành công")
                    .data(result)
                    .build());
        } catch (Exception e) {
            log.error("Error importing data template: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("Lỗi import dữ liệu: " + e.getMessage())
                            .build());
        }
    }
}
