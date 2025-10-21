package com.ptit.schedule.service;

import com.ptit.schedule.dto.SubjectRequest;
import com.ptit.schedule.dto.SubjectResponse;

import java.util.List;

public interface SubjectService {
    
    /**
     * Lấy tất cả subjects
     */
    List<SubjectResponse> getAllSubjects();

    
    /**
     * Lấy subjects theo major ID
     */
    List<SubjectResponse> getSubjectsByMajorId(Integer majorId);

    /**
     * Tạo subject mới
     */
    SubjectResponse createSubject(SubjectRequest request);
    
    /**
     * Cập nhật subject
     */
    SubjectResponse updateSubject(Long id, SubjectRequest request);
    
    /**
     * Xóa subject
     */
    void deleteSubject(Long id);
}
