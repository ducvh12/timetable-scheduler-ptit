package com.ptit.schedule.repository;

import com.ptit.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    List<Schedule> findBySubjectId(String subjectId);
    List<Schedule> findByMajor(String major);
    List<Schedule> findByStudentYear(String studentYear);
    List<Schedule> findByUserId(Long userId);
    List<Schedule> findByUserIdOrderByIdAsc(Long userId);
}