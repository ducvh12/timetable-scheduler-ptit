package com.ptit.schedule.controller;

import com.ptit.schedule.dto.ApiResponse;
import com.ptit.schedule.service.ExcelParseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("api/simple-excel")
@Tag(name = "Excel Upload", description = "API upload file Excel")
@RequiredArgsConstructor
public class SimpleExcelController {
    
    private final ExcelParseService excelParseService;
    
    @Operation(summary = "Upload Excel file", 
               description = "Upload file Excel để test. Chọn file từ máy tính.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Upload thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "File không hợp lệ")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<String> uploadExcel(
            @Parameter(description = "File Excel (.xlsx, .xls)", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("File không được để trống");
            }
            
            String fileName = file.getOriginalFilename();
            long fileSize = file.getSize();
            String contentType = file.getContentType();
            
            return ResponseEntity.ok(String.format(
                "✅ File uploaded thành công!\n" +
                "📁 Tên file: %s\n" +
                "📏 Kích thước: %d bytes\n" +
                "📋 Content Type: %s", 
                fileName, fileSize, contentType));
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("❌ Lỗi: " + e.getMessage());
        }
    }
    
    @Operation(summary = "Import Excel và tạo dữ liệu", 
               description = "Upload file Excel và tự động tạo Faculty, Major, Subject từ dữ liệu.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Import thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "File không hợp lệ")
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importExcel(
            @Parameter(description = "File Excel (.xlsx, .xls)", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("File không được để trống"));
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx") && 
                !file.getOriginalFilename().toLowerCase().endsWith(".xls")) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.badRequest("Chỉ hỗ trợ file Excel (.xlsx, .xls)"));
            }
            
            Map<String, Object> result = excelParseService.parseExcelAndCreateData(file);
            
            if (result.containsKey("error")) {
                return ResponseEntity.status(500)
                    .body(ApiResponse.badRequest(result.get("error").toString()));
            }
            
            return ResponseEntity.ok(ApiResponse.success(result, "Import dữ liệu thành công!"));
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(ApiResponse.badRequest("Lỗi khi xử lý file: " + e.getMessage()));
        }
    }
    
    @Operation(summary = "Health check", description = "Kiểm tra trạng thái controller")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("✅ Simple Excel Controller is OK");
    }
}
