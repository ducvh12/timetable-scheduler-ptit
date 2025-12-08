package com.ptit.schedule.controller;

import com.ptit.schedule.dto.*;
import com.ptit.schedule.entity.Schedule;
import com.ptit.schedule.entity.Subject;
import com.ptit.schedule.entity.TKBTemplate;
import com.ptit.schedule.entity.User;
import com.ptit.schedule.exception.InvalidDataException;
import com.ptit.schedule.exception.ResourceNotFoundException;
import com.ptit.schedule.repository.SubjectRepository;
import com.ptit.schedule.repository.TKBTemplateRepository;
import com.ptit.schedule.service.ScheduleService;
import com.ptit.schedule.service.DataLoaderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
@Tag(name = "Schedule Controller", description = "APIs for schedule and timetable management")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final TKBTemplateRepository tkbTemplateRepository;
    private final SubjectRepository subjectRepository;
    private final DataLoaderService dataLoaderService;

    @PostMapping("/save-batch")
    public ResponseEntity<String> saveBatch(@RequestBody List<SaveScheduleRequest> scheduleRequests) {
        if (scheduleRequests == null || scheduleRequests.isEmpty()) {
            throw new InvalidDataException("Danh sách lịch học không được rỗng");
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        if (currentUser == null) {
            throw new ResourceNotFoundException("Không tìm thấy thông tin người dùng");
        }
        
        // Convert DTO to Entity and attach TKBTemplate
        List<Schedule> schedules = new ArrayList<>();
        for (SaveScheduleRequest request : scheduleRequests) {
            if (request.getSubjectId() == null) {
                throw new InvalidDataException("Subject ID không được rỗng");
            }
            
            if (request.getTemplateDatabaseId() == null) {
                throw new InvalidDataException("Template ID không được rỗng");
            }
            
            // Load Subject from database
            Subject subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy môn học với ID: " + request.getSubjectId()));
            
            // Load TKBTemplate from database
            TKBTemplate template = tkbTemplateRepository.findById(request.getTemplateDatabaseId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy template với ID: " + request.getTemplateDatabaseId()));
            
            // Create Schedule entity
            Schedule schedule = Schedule.builder()
                    .subject(subject) // Gắn subject entity
                    .classNumber(request.getClassNumber())
                    .studentYear(request.getStudentYear())
                    .major(request.getMajor()) // Lưu major từ FE
                    .specialSystem(request.getSpecialSystem())
                    .siSoMotLop(request.getSiSoMotLop()) // Lưu sĩ số một lớp từ FE
                    .roomNumber(request.getRoomNumber())
                    .user(currentUser)
                    .tkbTemplate(template) // Gắn template vào schedule
                    .build();
            
            schedules.add(schedule);
        }
        
        scheduleService.saveAll(schedules);
        
        // Auto-commit lastSlotIdx to Redis sau khi lưu TKB
        if (!schedules.isEmpty()) {
            Schedule firstSchedule = schedules.get(0);
            
            if (firstSchedule.getSemester() != null) {
                scheduleService.commitSessionToRedis(currentUser.getId(), 
                    firstSchedule.getSemester().getAcademicYear(), 
                    firstSchedule.getSemester().getSemesterName());
                System.out.println("✅ [ScheduleController] Auto-committed lastSlotIdx to Redis after saving schedules");
            }
        }
        
        return ResponseEntity.ok("Đã lưu TKB vào database!");
    }

    @GetMapping
    public ResponseEntity<List<Schedule>> getAllSchedules() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        if (currentUser == null) {
            throw new ResourceNotFoundException("Không tìm thấy thông tin người dùng");
        }
        
        List<Schedule> schedules = scheduleService.getSchedulesByUserId(currentUser.getId());
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/subject/{subjectId}")
    public ResponseEntity<List<Schedule>> getSchedulesBySubject(@PathVariable String subjectId) {
        List<Schedule> schedules = scheduleService.getSchedulesBySubjectId(subjectId);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/major/{major}")
    public ResponseEntity<List<Schedule>> getSchedulesByMajor(@PathVariable String major) {
        List<Schedule> schedules = scheduleService.getSchedulesByMajor(major);
        return ResponseEntity.ok(schedules);
    }

    @GetMapping("/student-year/{studentYear}")
    public ResponseEntity<List<Schedule>> getSchedulesByStudentYear(@PathVariable String studentYear) {
        List<Schedule> schedules = scheduleService.getSchedulesByStudentYear(studentYear);
        return ResponseEntity.ok(schedules);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteSchedule(@PathVariable Long id) {
        if (id == null || id <= 0) {
            throw new InvalidDataException("ID lịch học không hợp lệ");
        }
        
        scheduleService.deleteScheduleById(id);
        return ResponseEntity.ok("Đã xóa lịch học!");
    }

    @DeleteMapping
    public ResponseEntity<String> deleteAllSchedules() {
        scheduleService.deleteAllSchedules();
        return ResponseEntity.ok("Đã xóa toàn bộ lịch học!");
    }

    // ==================== TKB Generation APIs (moved from TKBController) ====================

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
        
        TKBBatchResponse response = scheduleService.simulateExcelFlowBatch(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Health check", description = "Kiểm tra trạng thái Schedule controller")
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Schedule Controller is OK");
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
        scheduleService.resetState();
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

        TKBBatchResponse response = scheduleService.simulateExcelFlowBatch(batchRequest);

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

    @Operation(summary = "Import data lịch mẫu", description = "Import file Excel chứa dữ liệu lịch mẫu và lưu vào file JSON theo học kỳ")
    @PostMapping("/import-data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> importDataTemplate(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file,
            @RequestParam("semester") String semester) {
        if (file.isEmpty()) {
            throw new InvalidDataException("File không được để trống");
        }
        
        if (semester == null || semester.isEmpty()) {
            throw new InvalidDataException("Học kỳ không được để trống");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new InvalidDataException("File phải có định dạng Excel (.xlsx hoặc .xls)");
        }

        // Import và lưu với tên file theo học kỳ
        String jsonFilename = dataLoaderService.importDataFromExcel(file, semester);

        Map<String, Object> result = new HashMap<>();
        result.put("filename", filename);
        result.put("semester", semester);
        result.put("jsonFile", jsonFilename);
        result.put("message", "Đã import dữ liệu và lưu vào " + jsonFilename);

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
        
        scheduleService.commitSessionToRedis(userId, academicYear, semester);

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
        
        scheduleService.resetOccupiedRoomsRedis(userId, academicYear, semester);

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