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
}
