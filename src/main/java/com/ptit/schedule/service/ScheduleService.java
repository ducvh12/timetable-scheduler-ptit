package com.ptit.schedule.service;

import com.ptit.schedule.dto.TKBBatchRequest;
import com.ptit.schedule.dto.TKBBatchResponse;
import com.ptit.schedule.entity.Schedule;
import java.util.List;
import java.util.Map;

public interface ScheduleService {
    // CRUD operations
    void saveAll(List<Schedule> schedules);
    List<Schedule> getAllSchedules();
    List<Schedule> getSchedulesBySubjectId(String subjectId);
    List<Schedule> getSchedulesByMajor(String major);
    List<Schedule> getSchedulesByStudentYear(String studentYear);
    List<Schedule> getSchedulesByUserId(Long userId);
    void deleteScheduleById(Long id);
    void deleteAllSchedules();
    
    // Timetable generation operations
    TKBBatchResponse generateSchedule(TKBBatchRequest request);
    void commitSessionToRedis(Long userId, String academicYear, String semester);
    void resetState();
    void resetOccupiedRooms();
    void resetLastSlotIndexRedis(Long userId, String academicYear, String semester);
    Map<String, Integer> getOccupiedRoomsInfo();
}