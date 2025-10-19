package com.ptit.schedule.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface ExcelParseService {
    Map<String, Object> parseExcelAndCreateData(MultipartFile file);
}
