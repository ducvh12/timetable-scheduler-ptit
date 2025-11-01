package com.ptit.schedule.controller;

import com.ptit.schedule.dto.ApiResponse;
import com.ptit.schedule.dto.ConflictResult;
import com.ptit.schedule.dto.ScheduleEntry;
import com.ptit.schedule.dto.ScheduleValidationResult;
import com.ptit.schedule.service.ScheduleConflictDetectionService;
import com.ptit.schedule.service.ScheduleExcelReaderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("api/schedule-validation")
@RequiredArgsConstructor
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:3001", "http://localhost:8080",
        "http://localhost:5173", "http://localhost:4173" })
public class ScheduleValidationController {

    private final ScheduleExcelReaderService excelReaderService;
    private final ScheduleConflictDetectionService conflictDetectionService;

    /**
     * API endpoint để validate file Excel format
     */
    @PostMapping(value = "/validate-format", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Boolean> validateExcelFormat(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ApiResponse.badRequest("Vui lòng chọn file Excel để upload");
            }

            boolean isValid = excelReaderService.validateScheduleExcelFormat(file);
            
            if (!isValid) {
                return ApiResponse.badRequest("File không đúng định dạng thời khóa biểu. Vui lòng kiểm tra lại file Excel.");
            }

            return ApiResponse.success(true, "File hợp lệ");

        } catch (Exception e) {
            log.error("Error validating Excel format", e);
            return ApiResponse.error("Lỗi khi kiểm tra định dạng file: " + e.getMessage(), 400);
        }
    }

    /**
     * API endpoint để upload và phân tích xung đột
     */
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ScheduleValidationResult> analyzeSchedule(@RequestParam("file") MultipartFile file) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return ApiResponse.badRequest("Vui lòng chọn file Excel để upload");
            }

            if (!excelReaderService.validateScheduleExcelFormat(file)) {
                return ApiResponse.badRequest("File không đúng định dạng thời khóa biểu. Vui lòng kiểm tra lại file Excel.");
            }

            // Read schedule data
            List<ScheduleEntry> scheduleEntries = excelReaderService.readScheduleFromExcel(file);
            
            if (scheduleEntries.isEmpty()) {
                return ApiResponse.badRequest("Không tìm thấy dữ liệu thời khóa biểu trong file. Vui lòng kiểm tra lại.");
            }

            // Detect conflicts
            ConflictResult conflictResult = conflictDetectionService.detectConflicts(scheduleEntries);

            // Prepare result
            ScheduleValidationResult result = ScheduleValidationResult.builder()
                    .conflictResult(conflictResult)
                    .scheduleEntries(scheduleEntries)
                    .fileName(file.getOriginalFilename())
                    .totalEntries(scheduleEntries.size())
                    .fileSize(file.getSize())
                    .build();

            return ApiResponse.success(result, "Phân tích thành công");

        } catch (Exception e) {
            log.error("Error processing schedule file", e);
            return ApiResponse.error("Lỗi khi xử lý file: " + e.getMessage(), 400);
        }
    }

    /**
     * API endpoint để lấy chi tiết xung đột cụ thể
     */
    @GetMapping("/conflicts/{type}")
    public ApiResponse<Object> getConflictDetails(@PathVariable String type,
                                                  @RequestParam(required = false) String room,
                                                  @RequestParam(required = false) String teacherId) {
        try {
            // This endpoint could be extended to provide detailed analysis
            // for specific conflicts if needed
            return ApiResponse.success(null, "Endpoint for future detailed conflict analysis");
        } catch (Exception e) {
            log.error("Error getting conflict details", e);
            return ApiResponse.error("Lỗi khi lấy chi tiết xung đột: " + e.getMessage(), 400);
        }
    }
}