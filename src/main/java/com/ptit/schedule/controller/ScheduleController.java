package com.ptit.schedule.controller;

import com.ptit.schedule.entity.Schedule;
import com.ptit.schedule.entity.User;
import com.ptit.schedule.exception.ResourceNotFoundException;
import com.ptit.schedule.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping("/save-batch")
    public ResponseEntity<String> saveBatch(@RequestBody List<Schedule> schedules) {
        try {
            // Get current logged-in user from JWT token
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();
            
            // Set user for all schedules
            schedules.forEach(schedule -> schedule.setUser(currentUser));
            
            scheduleService.saveAll(schedules);
            return ResponseEntity.ok("Đã lưu TKB vào database!");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllSchedules() {
        try {
            // Get current logged-in user from JWT token
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = (User) authentication.getPrincipal();
            
            // Get schedules of current user only
            List<Schedule> schedules = scheduleService.getSchedulesByUserId(currentUser.getId());
            return ResponseEntity.ok(schedules);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
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
        scheduleService.deleteScheduleById(id);
        return ResponseEntity.ok("Đã xóa lịch học!");
    }

    @DeleteMapping
    public ResponseEntity<String> deleteAllSchedules() {
        scheduleService.deleteAllSchedules();
        return ResponseEntity.ok("Đã xóa toàn bộ lịch học!");
    }
}