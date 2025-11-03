package com.ptit.schedule.controller;

import com.ptit.schedule.entity.Schedule;
import com.ptit.schedule.service.ScheduleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/schedules")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @PostMapping("/save-batch")
    public ResponseEntity<String> saveBatch(@RequestBody List<Schedule> schedules) {
        scheduleService.saveAll(schedules);
        return ResponseEntity.ok("Đã lưu TKB vào database!");
    }

    @GetMapping
    public ResponseEntity<?> getAllSchedules() {
        try {
            List<Schedule> schedules = scheduleService.getAllSchedules();
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
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