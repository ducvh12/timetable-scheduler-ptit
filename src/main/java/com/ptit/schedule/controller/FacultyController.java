package com.ptit.schedule.controller;

import com.ptit.schedule.dto.FacultyRequest;
import com.ptit.schedule.dto.FacultyResponse;
import com.ptit.schedule.service.FacultyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/faculties")
@RequiredArgsConstructor
@Tag(name = "Faculty Management", description = "API quản lý khoa")
public class FacultyController {
    
    private final FacultyService facultyService;
    
    @Operation(summary = "Health check", description = "Kiểm tra trạng thái Faculty Controller")
    @ApiResponse(responseCode = "200", description = "Faculty Controller hoạt động bình thường")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return new ResponseEntity<>("Faculty Controller is OK", HttpStatus.OK);
    }
    
    @Operation(summary = "Lấy tất cả khoa", description = "Trả về danh sách tất cả khoa")
    @ApiResponse(responseCode = "200", description = "Thành công")
    @GetMapping
    public ResponseEntity<List<FacultyResponse>> getAllFaculties() {
        List<FacultyResponse> faculties = facultyService.getAllFaculties();
        return ResponseEntity.ok(faculties);
    }
    
    @Operation(summary = "Lấy khoa theo ID", description = "Trả về thông tin khoa theo ID")
    @ApiResponse(responseCode = "200", description = "Thành công")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy khoa")
    @GetMapping("/{id}")
    public ResponseEntity<FacultyResponse> getFacultyById(@PathVariable String id) {
        FacultyResponse faculty = facultyService.getFacultyById(id);
        return ResponseEntity.ok(faculty);
    }
    
    @Operation(summary = "Tạo khoa mới", description = "Tạo khoa mới")
    @ApiResponse(responseCode = "201", description = "Tạo thành công")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @PostMapping
    public ResponseEntity<FacultyResponse> createFaculty(@Valid @RequestBody FacultyRequest request) {
        FacultyResponse faculty = facultyService.createFaculty(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(faculty);
    }
    
    @Operation(summary = "Cập nhật khoa", description = "Cập nhật thông tin khoa")
    @ApiResponse(responseCode = "200", description = "Cập nhật thành công")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy khoa")
    @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @PutMapping("/{id}")
    public ResponseEntity<FacultyResponse> updateFaculty(@PathVariable String id, 
                                                        @Valid @RequestBody FacultyRequest request) {
        FacultyResponse faculty = facultyService.updateFaculty(id, request);
        return ResponseEntity.ok(faculty);
    }
    
    @Operation(summary = "Xóa khoa", description = "Xóa khoa theo ID")
    @ApiResponse(responseCode = "204", description = "Xóa thành công")
    @ApiResponse(responseCode = "404", description = "Không tìm thấy khoa")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFaculty(@PathVariable String id) {
        facultyService.deleteFaculty(id);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(summary = "Test upload file", description = "Test upload file Excel")
    @PostMapping("/upload-test")
    public ResponseEntity<String> uploadTest(@RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("File không được để trống");
            }
            
            String fileName = file.getOriginalFilename();
            long fileSize = file.getSize();
            
            return ResponseEntity.ok("File uploaded: " + fileName + ", Size: " + fileSize + " bytes");
            
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Lỗi: " + e.getMessage());
        }
    }
}
