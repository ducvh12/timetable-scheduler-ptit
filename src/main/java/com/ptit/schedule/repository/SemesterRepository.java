package com.ptit.schedule.repository;

import com.ptit.schedule.entity.Semester;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SemesterRepository extends JpaRepository<Semester, Long> {
    
    /**
     * Tìm semester theo tên học kỳ và năm học
     */
    Optional<Semester> findBySemesterNameAndAcademicYear(String semesterName, String academicYear);
    
    /**
     * Tìm semester theo tên học kỳ
     */
    Optional<Semester> findBySemesterName(String semesterName);

    
    /**
     * Tìm semester theo năm học
     */
    List<Semester> findByAcademicYear(String academicYear);
    
    /**
     * Tìm semester đang hoạt động
     */
    Optional<Semester> findByIsActiveTrue();
    
    /**
     * Lấy tất cả semester theo thứ tự giảm dần
     */
    @Query("SELECT s FROM Semester s ORDER BY s.academicYear DESC, s.semesterName DESC")
    List<Semester> findAllOrderByYearAndSemesterDesc();
    
    /**
     * Kiểm tra tên học kỳ và năm học đã tồn tại chưa
     */
    boolean existsBySemesterNameAndAcademicYear(String semesterName, String academicYear);
    
    /**
     * Kiểm tra tên học kỳ đã tồn tại chưa
     */
    boolean existsBySemesterName(String semesterName);
}
