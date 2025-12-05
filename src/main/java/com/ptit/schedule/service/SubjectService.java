package com.ptit.schedule.service;

import com.ptit.schedule.dto.SubjectFullDTO;
import com.ptit.schedule.dto.SubjectMajorDTO;
import com.ptit.schedule.dto.SubjectRequest;
import com.ptit.schedule.dto.SubjectResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Set;

public interface SubjectService {

    // Lấy tất cả subjects(thông tin đặc trưng) cùng majors
    List<SubjectMajorDTO> getAllSubjects();

    // Lấy tất cả subjects với phân trang
    Page<SubjectFullDTO> getAllSubjectsWithPagination(int page, int size, String sortBy, String sortDir);

    // Lấy tất cả subject phân trang dùng specification
    Page<SubjectFullDTO> getSubjects(
            String search,
            String semester,
            String classYear,
            String majorCode,
            String programType,
            String academicYear,
            Pageable pageable
    );

    // Lấy tất cả subjects với phân trang và filter
    Page<SubjectFullDTO> getAllSubjectsWithPaginationAndFilters(
        int page, 
        int size, 
        String sortBy, 
        String sortDir,
        String academicYear,
        String semester,
        String classYear,
        String majorCode,
        String programType
    );

    // Lấy subjects theo major ID
    List<SubjectResponse> getSubjectsByMajorId(Integer majorId);

    // Lấy subject kèm mã ngành theo semester, academic year, class year, program type và major codes
    List<SubjectMajorDTO> getSubjectAndMajorCodeByClassYear(
        String semesterName,
        String academicYear,
        String classYear,
        String programType,
        List<String> majorCodes
    );

    // Nhóm các ngành có cùng tập môn học chung theo semester, academic year, class year và program type
    List<Set<String>> groupMajorsBySharedSubjects(
        String semesterName,
        String academicYear,
        String classYear,
        String programType
    );

    // Lấy danh sách môn học chung theo semester và academic year
    List<SubjectMajorDTO> getCommonSubjects(String semesterName, String academicYear);

    // Tạo subject mới
    SubjectResponse createSubject(SubjectRequest request);
    
    // Cập nhật subject
    SubjectResponse updateSubject(Long id, SubjectRequest request);
    
    // Xóa subject theo ID
    void deleteSubject(Long id);

    // Xóa subjects theo semesterName
    int deleteSubjectsBySemesterName(String semesterName);

    // Xóa subjects theo semesterName và academicYear
    int deleteSubjectsBySemesterNameAndAcademicYear(String semesterName, String academicYear);

    // Lấy tất cả program types (distinct)
    List<String> getAllProgramTypes();

    // Lấy program types theo semester và academic year
    List<String> getProgramTypesBySemesterAndAcademicYear(String semesterName, String academicYear);

    // Lấy tất cả class years (distinct)
    List<String> getAllClassYears();

    // Lấy class years theo semester, academic year và program type
    List<String> getClassYearsBySemesterAndAcademicYearAndProgramType(String semesterName, String academicYear, String programType);
}
