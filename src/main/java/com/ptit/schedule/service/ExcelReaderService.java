package com.ptit.schedule.service;

import com.ptit.schedule.dto.ExcelImportResult;
import com.ptit.schedule.dto.SubjectRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExcelReaderService {
    
    /**
     * Đọc file Excel và trả về danh sách SubjectRequest
     * @deprecated Use readAndValidateSubjectsFromExcel instead
     */
    @Deprecated
    List<SubjectRequest> readSubjectsFromExcel(MultipartFile file, String semester, String academicYear);

    /**
     * Đọc file Excel, validate duplicates và trả về kết quả import
     * @param file File Excel chứa danh sách môn học
     * @param semester Tên học kỳ
     * @param academicYear Năm học
     * @return ExcelImportResult chứa danh sách valid subjects và warnings
     */
    ExcelImportResult readAndValidateSubjectsFromExcel(MultipartFile file, String semester, String academicYear);

}
