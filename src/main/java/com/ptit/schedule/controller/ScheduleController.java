package com.ptit.schedule.controller;

import com.ptit.schedule.entity.Schedule;
import com.ptit.schedule.entity.User;
import com.ptit.schedule.exception.InvalidDataException;
import com.ptit.schedule.exception.ResourceNotFoundException;
import com.ptit.schedule.service.ScheduleService;
import com.ptit.schedule.service.TimetableSchedulingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final TimetableSchedulingService timetableSchedulingService;

    @PostMapping("/save-batch")
    public ResponseEntity<String> saveBatch(@RequestBody List<Schedule> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            throw new InvalidDataException("Danh sách lịch học không được rỗng");
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = (User) authentication.getPrincipal();
        
        if (currentUser == null) {
            throw new ResourceNotFoundException("Không tìm thấy thông tin người dùng");
        }
        
        schedules.forEach(schedule -> schedule.setUser(currentUser));
        
        scheduleService.saveAll(schedules);
        
        // Auto-commit lastSlotIdx to Redis sau khi lưu TKB
        if (!schedules.isEmpty()) {
            Schedule firstSchedule = schedules.get(0);
            String academicYear = firstSchedule.getAcademicYear();
            String semester = firstSchedule.getSemester();
            
            if (academicYear != null && semester != null) {
                timetableSchedulingService.commitSessionToRedis(currentUser.getId(), academicYear, semester);
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
}