package com.ptit.schedule.service;

import com.ptit.schedule.dto.SubjectRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ExcelReaderService {
    
    /**
     * Đọc file Excel và trả về danh sách SubjectRequest
     */
    List<SubjectRequest> readSubjectsFromExcel(MultipartFile file);
    
    /**
     * Validate dữ liệu Excel
     */
    List<String> validateExcelData(List<SubjectRequest> subjects);
}
