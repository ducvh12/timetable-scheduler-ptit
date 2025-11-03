package com.ptit.schedule.controller;

import com.ptit.schedule.dto.*;
import com.ptit.schedule.service.ExcelReaderService;
import com.ptit.schedule.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Nullable;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@RestController
@RequestMapping("api/subjects")
@RequiredArgsConstructor
@Tag(name = "Subject Management", description = "API quản lý môn học")
public class SubjectController {
    
    private final SubjectService subjectService;
    private final ExcelReaderService excelReaderService;

    @Operation(summary = "Health check", description = "Kiểm tra trạng thái server")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Server hoạt động bình thường")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        ApiResponse<String> response = ApiResponse.success("Server is OK");
        return ResponseEntity.ok(response);
    }



    
    @Operation(summary = "Lấy tất cả môn học với phân trang", description = "Trả về danh sách môn học với phân trang và sắp xếp")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công")
    @GetMapping("")
    public ResponseEntity<ApiResponse<PagedResponse<SubjectFullDTO>>> getAllSubjectsPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        try {
            // Validate parameters
            if (page < 0) page = 0;
            if (size <= 0 || size > 100) size = 10; // Limit max size to 100

            Page<SubjectFullDTO> result = subjectService.getAllSubjectsWithPagination(page, size, sortBy, sortDir);
            PagedResponse<SubjectFullDTO> pagedResponse =  PagedResponse.<SubjectFullDTO>builder()
                    .page(result.getNumber())
                    .items(result.getContent().stream().toList())
                    .totalPages(result.getTotalPages())
                    .size(result.getSize())
                    .totalElements(result.getTotalElements())
                    .build();
            return ResponseEntity.ok(
                ApiResponse.success(pagedResponse, "Lấy danh sách môn học với phân trang thành công")
            );
        } catch (RuntimeException e) {
            ApiResponse<PagedResponse<SubjectFullDTO>> response = ApiResponse.badRequest(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }


    
    @Operation(summary = "Lấy môn học theo ngành", description = "Trả về danh sách môn học theo major ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công")
    @GetMapping("/major/{majorId}")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> getSubjectsByMajorId(@PathVariable Integer majorId) {
        List<SubjectResponse> subjects = subjectService.getSubjectsByMajorId(majorId);
        ApiResponse<List<SubjectResponse>> response = ApiResponse.success(subjects, "Lấy danh sách môn học theo ngành thành công");
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Tạo môn học mới", description = "Tạo môn học mới với logic tự động tạo major nếu cần")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Tạo thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @PostMapping
    public ResponseEntity<ApiResponse<SubjectResponse>> createSubject(@Valid @RequestBody SubjectRequest request) {
        try {
            System.out.println("Received Subject Request: " + request);
            SubjectResponse subject = subjectService.createSubject(request);
            ApiResponse<SubjectResponse> response = ApiResponse.created(subject, "Tạo môn học thành công");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            ApiResponse<SubjectResponse> response = ApiResponse.badRequest(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @Operation(summary = "Cập nhật môn học", description = "Cập nhật thông tin môn học")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cập nhật thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy môn học")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SubjectResponse>> updateSubject(@PathVariable Long id,
                                                        @Valid @RequestBody SubjectRequest request) {
        try {
            SubjectResponse subject = subjectService.updateSubject(id, request);
            ApiResponse<SubjectResponse> response = ApiResponse.success(subject, "Cập nhật môn học thành công");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("not found")) {
                ApiResponse<SubjectResponse> response = ApiResponse.notFound(e.getMessage());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            } else {
                ApiResponse<SubjectResponse> response = ApiResponse.badRequest(e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }
        }
    }
    
    @Operation(summary = "Xóa môn học", description = "Xóa môn học theo ID")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Xóa thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Không tìm thấy môn học")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@PathVariable Long id) {
        try {
            subjectService.deleteSubject(id);
            ApiResponse<Void> response = ApiResponse.success(null, "Xóa môn học thành công");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<Void> response = ApiResponse.notFound(e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @Operation(summary = "Upload Excel và tạo nhiều môn học", description = "Upload file Excel để tạo nhiều môn học cùng lúc")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Upload thành công")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "File không hợp lệ hoặc dữ liệu lỗi")
    @PostMapping("/upload-excel")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> uploadExcelSubjects(@RequestParam("file") MultipartFile file,
                                                                                  @RequestParam("semester") String semester) {
        try {
            // Kiểm tra file
            if (file.isEmpty()) {
                ApiResponse<List<SubjectResponse>> response = ApiResponse.badRequest("File không được để trống");
                return ResponseEntity.badRequest().body(response);
            }
            
            if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
                ApiResponse<List<SubjectResponse>> response = ApiResponse.badRequest("Chỉ chấp nhận file Excel (.xlsx)");
                return ResponseEntity.badRequest().body(response);
            }
            
            // Đọc dữ liệu từ Excel
            List<SubjectRequest> subjectRequests = excelReaderService.readSubjectsFromExcel(file, semester);
            
            if (subjectRequests.isEmpty()) {
                ApiResponse<List<SubjectResponse>> response = ApiResponse.badRequest("File Excel không có dữ liệu hợp lệ");
                return ResponseEntity.badRequest().body(response);
            }
//            for(SubjectRequest request : subjectRequests) {
//                System.out.println("Parsed Subject Request from Excel: " + request);
//            }

            // Tạo subjects
            List<SubjectResponse> createdSubjects = new ArrayList<>();
            List<String> creationErrors = new ArrayList<>();
            
            for (int i = 0; i < subjectRequests.size(); i++) {
                try {
                    SubjectResponse subject = subjectService.createSubject(subjectRequests.get(i));
                    createdSubjects.add(subject);
                } catch (RuntimeException e) {
//                    creationErrors.add("Dòng " + (i + 2) + ": " + e.getMessage());
                    throw  new RuntimeException("Dòng " + (i + 2) + ": " + e.getMessage());
                }
            }
            
            if (!creationErrors.isEmpty()) {
                String errorMessage = "Một số môn học không thể tạo:\n" + String.join("\n", creationErrors);
                ApiResponse<List<SubjectResponse>> response = ApiResponse.success(createdSubjects, 
                    "Tạo thành công " + createdSubjects.size() + " môn học. " + errorMessage);
                return ResponseEntity.ok(response);
            }
            
            ApiResponse<List<SubjectResponse>> response = ApiResponse.success(
                "Tạo thành công " + createdSubjects.size() + " môn học từ file Excel");
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            ApiResponse<List<SubjectResponse>> response = ApiResponse.badRequest("Lỗi xử lý file: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(
            summary = "Lấy danh sách môn học và ngành theo khóa",
            description = "Truyền classYear và programType để lấy danh sách môn học và ngành tương ứng"
    )
    @GetMapping("/majors")
    public ResponseEntity<ApiResponse<List<SubjectMajorDTO>>> getSubjectsAndMajorByMajorCodes(
            @Nullable @RequestParam String classYear,
            @RequestParam String programType,
            @Nullable @RequestParam List<String> majorCodes) {  // ✅ List<String>
        try {

            List<SubjectMajorDTO> subjects =
                    subjectService.getSubjectAndMajorCodeByClassYear(classYear, programType, majorCodes);
            if(programType.equals("Chung")){
                subjects = subjectService.getCommonSubjects();
            }
            ApiResponse<List<SubjectMajorDTO>> response =
                    ApiResponse.success(subjects, "Lấy danh sách môn học và ngành theo khóa thành công");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            ApiResponse<List<SubjectMajorDTO>> response = ApiResponse.badRequest(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }


    @Operation(
            summary = "Lấy danh sách nhóm ngành học chung môn",
            description = "Truyền classYear và programType để lấy danh sách nhóm ngành có môn học chung"
    )
    @GetMapping("/group-majors")
    public ResponseEntity<ApiResponse<List<Set<String>>>> getGroupedMajors(
            @RequestParam String classYear,
            @RequestParam String programType) {
        try {
            System.out.println("classYear: " + classYear);
            System.out.println("programType: " + programType);
            List<Set<String>> groupedMajors = subjectService.groupMajorsBySharedSubjects(classYear, programType);

            if (groupedMajors == null || groupedMajors.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Không tìm thấy dữ liệu nhóm ngành cho khóa " + classYear));
            }

            return ResponseEntity.ok(ApiResponse.success(groupedMajors, "Lấy danh sách nhóm ngành thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xử lý dữ liệu", e.getMessage(), 500));
        }
    }


    @Operation(
            summary = "Lấy danh sách môn chung ",
            description = "Trả về các môn học chung như Anh văn, Chính trị, Kỹ năng mềm."
    )
    @GetMapping("/common-subjects")
    public ResponseEntity<ApiResponse<List<SubjectMajorDTO>>> getCommonSubjects(
          ) {
        try {
            List<SubjectMajorDTO> commonSubjects = subjectService.getCommonSubjects();

            if (commonSubjects == null || commonSubjects.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.notFound("Không tìm thấy môn chung"));
            }

            return ResponseEntity.ok(ApiResponse.success(commonSubjects, "Lấy danh sách môn chung thành công"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi khi xử lý dữ liệu", e.getMessage(), 500));
        }
    }

}
