package com.ptit.schedule.service;

import com.ptit.schedule.dto.SemesterRequest;
import com.ptit.schedule.dto.SemesterResponse;

import java.util.List;

public interface SemesterService {
    
    /**
     * Lấy tất cả semesters
     */
    List<SemesterResponse> getAllSemesters();
    
    /**
     * Lấy semester theo ID
     */
    SemesterResponse getSemesterById(Long id);
    
    /**
     * Lấy semester theo tên học kỳ
     */
    SemesterResponse getSemesterByName(String semesterName);
    
    /**
     * Lấy semester đang hoạt động
     */
    SemesterResponse getActiveSemester();
    
    /**
     * Tạo semester mới
     */
    SemesterResponse createSemester(SemesterRequest request);
    
    /**
     * Cập nhật semester
     */
    SemesterResponse updateSemester(Long id, SemesterRequest request);
    
    /**
     * Xóa semester
     */
    void deleteSemester(Long id);
    
    /**
     * Kích hoạt semester (set isActive = true, các semester khác = false)
     */
    SemesterResponse activateSemester(Long id);
    
    /**
     * Lấy danh sách tên học kỳ (distinct)
     */
    List<String> getAllSemesterNames();
}
