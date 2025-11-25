package com.ptit.schedule.service.impl;

import com.ptit.schedule.entity.Schedule;
import com.ptit.schedule.repository.ScheduleRepository;
import com.ptit.schedule.service.ScheduleService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ScheduleServiceImpl implements ScheduleService {
    private final ScheduleRepository scheduleRepository;

    public ScheduleServiceImpl(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Override
    public void saveAll(List<Schedule> schedules) {
        scheduleRepository.saveAll(schedules);
    }

    @Override
    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    @Override
    public List<Schedule> getSchedulesBySubjectId(String subjectId) {
        return scheduleRepository.findBySubjectId(subjectId);
    }

    @Override
    public List<Schedule> getSchedulesByMajor(String major) {
        return scheduleRepository.findByMajor(major);
    }

    @Override
    public List<Schedule> getSchedulesByStudentYear(String studentYear) {
        return scheduleRepository.findByStudentYear(studentYear);
    }

    @Override
    public List<Schedule> getSchedulesByUserId(Long userId) {
        return scheduleRepository.findByUserIdOrderByIdAsc(userId);
    }

    @Override
    public void deleteScheduleById(Long id) {
        scheduleRepository.deleteById(id);
    }

    @Override
    public void deleteAllSchedules() {
        scheduleRepository.deleteAll();
    }
}