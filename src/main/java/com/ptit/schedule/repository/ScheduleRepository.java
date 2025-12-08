package com.ptit.schedule.repository;

import com.ptit.schedule.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    @Query("SELECT s FROM Schedule s WHERE s.subject.subjectCode = :subjectCode")
    List<Schedule> findBySubjectId(@Param("subjectCode") String subjectCode);
    
    @Query("SELECT s FROM Schedule s WHERE s.subject.major.majorCode = :majorCode")
    List<Schedule> findByMajor(@Param("majorCode") String majorCode);
    
    List<Schedule> findByStudentYear(String studentYear);
    List<Schedule> findByUserId(Long userId);
    List<Schedule> findByUserIdOrderByIdAsc(Long userId);
}