package com.ptit.schedule.service;

import com.ptit.schedule.entity.Schedule;
import java.util.List;

public interface ScheduleService {
    void saveAll(List<Schedule> schedules);
    List<Schedule> getAllSchedules();
    List<Schedule> getSchedulesBySubjectId(String subjectId);
    List<Schedule> getSchedulesByMajor(String major);
    List<Schedule> getSchedulesByStudentYear(String studentYear);
    List<Schedule> getSchedulesByUserId(Long userId);
    void deleteScheduleById(Long id);
    void deleteAllSchedules();
}