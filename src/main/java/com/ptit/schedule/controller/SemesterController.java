package com.ptit.schedule.controller;

import com.ptit.schedule.dto.ApiResponse;
import com.ptit.schedule.dto.SemesterRequest;
import com.ptit.schedule.dto.SemesterResponse;
import com.ptit.schedule.service.SemesterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/semesters")
@RequiredArgsConstructor
public class SemesterController {
    
    private final SemesterService semesterService;
    
    /**
     * Lấy tất cả semesters
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<SemesterResponse>>> getAllSemesters() {
        try {
            List<SemesterResponse> semesters = semesterService.getAllSemesters();
            return ResponseEntity.ok(ApiResponse.success(semesters));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy danh sách học kỳ: " + e.getMessage(), 500));
        }
    }
    
    /**
     * Lấy semester theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SemesterResponse>> getSemesterById(@PathVariable Long id) {
        try {
            SemesterResponse semester = semesterService.getSemesterById(id);
            return ResponseEntity.ok(ApiResponse.success(semester));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy thông tin học kỳ: " + e.getMessage(), 500));
        }
    }
    
    /**
     * Lấy semester theo tên học kỳ
     */
    @GetMapping("/name/{semesterName}")
    public ResponseEntity<ApiResponse<SemesterResponse>> getSemesterByName(@PathVariable String semesterName) {
        try {
            SemesterResponse semester = semesterService.getSemesterByName(semesterName);
            return ResponseEntity.ok(ApiResponse.success(semester));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy thông tin học kỳ: " + e.getMessage(), 500));
        }
    }
    
    /**
     * Lấy semester đang hoạt động
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<SemesterResponse>> getActiveSemester() {
        try {
            SemesterResponse semester = semesterService.getActiveSemester();
            return ResponseEntity.ok(ApiResponse.success(semester));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy học kỳ đang hoạt động: " + e.getMessage(), 500));
        }
    }
    
    /**
     * Lấy danh sách tên học kỳ (distinct)
     */
    @GetMapping("/names")
    public ResponseEntity<ApiResponse<List<String>>> getAllSemesterNames() {
        try {
            List<String> names = semesterService.getAllSemesterNames();
            return ResponseEntity.ok(ApiResponse.success(names));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi lấy danh sách tên học kỳ: " + e.getMessage(), 500));
        }
    }
    
    /**
     * Tạo semester mới
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SemesterResponse>> createSemester(@Valid @RequestBody SemesterRequest request) {
        try {
            SemesterResponse semester = semesterService.createSemester(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(semester, "Tạo học kỳ thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.badRequest(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi tạo học kỳ: " + e.getMessage(), 500));
        }
    }
    
    /**
     * Cập nhật semester
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SemesterResponse>> updateSemester(
            @PathVariable Long id,
            @Valid @RequestBody SemesterRequest request) {
        try {
            SemesterResponse semester = semesterService.updateSemester(id, request);
            return ResponseEntity.ok(ApiResponse.success(semester, "Cập nhật học kỳ thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi cập nhật học kỳ: " + e.getMessage(), 500));
        }
    }
    
    /**
     * Xóa semester
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSemester(@PathVariable Long id) {
        try {
            semesterService.deleteSemester(id);
            return ResponseEntity.ok(ApiResponse.success(null, "Xóa học kỳ thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xóa học kỳ: " + e.getMessage(), 500));
        }
    }
    
    /**
     * Kích hoạt semester
     */
    @PatchMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<SemesterResponse>> activateSemester(@PathVariable Long id) {
        try {
            SemesterResponse semester = semesterService.activateSemester(id);
            return ResponseEntity.ok(ApiResponse.success(semester, "Kích hoạt học kỳ thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.notFound(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi kích hoạt học kỳ: " + e.getMessage(), 500));
        }
    }
}
