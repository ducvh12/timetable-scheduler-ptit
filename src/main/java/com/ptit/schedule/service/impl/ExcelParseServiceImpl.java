package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.FacultyRequest;
import com.ptit.schedule.dto.MajorRequest;
import com.ptit.schedule.dto.SubjectRequest;
import com.ptit.schedule.service.ExcelParseService;
import com.ptit.schedule.service.FacultyService;
import com.ptit.schedule.service.MajorService;
import com.ptit.schedule.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelParseServiceImpl implements ExcelParseService {
    
    private final FacultyService facultyService;
    private final MajorService majorService;
    private final SubjectService subjectService;
    
    @Override
    public Map<String, Object> parseExcelAndCreateData(MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            log.info("Bắt đầu parse file Excel: {}", file.getOriginalFilename());
            Sheet sheet = workbook.getSheetAt(0);
            
            log.info("File Excel có {} rows", sheet.getLastRowNum() + 1);
            
            // Đọc header để xác định cột
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                log.error("Không tìm thấy header row!");
                result.put("error", "Không tìm thấy header row trong file Excel");
                return result;
            }
            
            Map<String, Integer> columnMap = createColumnMap(headerRow);
            
            log.info("Tìm thấy {} cột: {}", columnMap.size(), columnMap.keySet());
            
            if (columnMap.isEmpty()) {
                log.error("Không tìm thấy cột nào trong file Excel!");
                result.put("error", "Không tìm thấy cột nào trong file Excel");
                return result;
            }
            
            Set<String> faculties = new HashSet<>();
            Map<String, Map<String, Object>> majorDataMap = new HashMap<>(); // majorName -> data
            List<Map<String, Object>> subjects = new ArrayList<>();
            
            // Đọc dữ liệu từ row 1 trở đi
            log.info("Bắt đầu đọc dữ liệu từ row 1 đến {}", sheet.getLastRowNum());
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    log.debug("Row {} is null, skipping", i);
                    continue;
                }
                
                try {
                    Map<String, Object> rowData = extractRowData(row, columnMap);
                    log.debug("Row {} data: {}", i, rowData.keySet());
                    
                    // Extract faculty (sử dụng cột Khoa thứ 2 - index 18)
                    String faculty = getStringValue(rowData, "Khoa");
                    // Kiểm tra xem có phải là tên khoa không (không phải năm)
                    if (faculty != null && !faculty.trim().isEmpty() && 
                        !faculty.matches("\\d{4}\\.?\\d*")) { // Không phải năm như 2022, 2023, 2024
                        faculties.add(faculty.trim());
                        log.debug("Found faculty: {}", faculty);
                    }
                    
                    // Extract major với dữ liệu đầy đủ
                    String major = getStringValue(rowData, "Nganh");
                    if (major != null && !major.trim().isEmpty()) {
                        majorDataMap.put(major.trim(), rowData); // Lưu data để lấy si_so, khoa
                        log.debug("Found major: {}", major);
                    }
                    
                    // Collect subject data
                    subjects.add(rowData);
                    
                } catch (Exception e) {
                    log.warn("Lỗi xử lý row {}: {}", i, e.getMessage());
                }
            }
            
            log.info("Tìm thấy {} faculties: {}", faculties.size(), faculties);
            log.info("Tìm thấy {} majors: {}", majorDataMap.size(), majorDataMap.keySet());
            log.info("Tìm thấy {} subjects", subjects.size());
            
            if (faculties.isEmpty()) {
                log.warn("Không tìm thấy Faculty nào! Kiểm tra cột 'Khoa.1'");
            }
            if (majorDataMap.isEmpty()) {
                log.warn("Không tìm thấy Major nào! Kiểm tra cột 'Nganh'");
            }
            
            // Tạo Faculty
            Map<String, String> facultyIdMap = createFaculties(faculties);
            log.info("Đã tạo {} faculties", facultyIdMap.size());
            
            // Tạo Major
            Map<String, String> majorIdMap = createMajors(majorDataMap, subjects, facultyIdMap);
            log.info("Đã tạo {} majors", majorIdMap.size());
            
            // Tạo Subject
            int subjectCount = createSubjects(subjects, facultyIdMap, majorIdMap);
            log.info("Đã tạo {} subjects", subjectCount);
            
            result.put("faculties_created", facultyIdMap.size());
            result.put("majors_created", majorIdMap.size());
            result.put("subjects_created", subjectCount);
            result.put("total_rows_processed", subjects.size());
            result.put("message", "Import Faculty, Major và Subject thành công!");
            
        } catch (IOException e) {
            log.error("Lỗi đọc file Excel: {}", e.getMessage(), e);
            result.put("error", "Không thể đọc file Excel: " + e.getMessage());
        } catch (Exception e) {
            log.error("Lỗi không xác định: {}", e.getMessage(), e);
            result.put("error", "Lỗi không xác định: " + e.getMessage());
        }
        
        return result;
    }
    
    private Map<String, Integer> createColumnMap(Row headerRow) {
        Map<String, Integer> columnMap = new HashMap<>();
        
        for (Cell cell : headerRow) {
            String columnName = getCellValueAsString(cell).trim();
            columnMap.put(columnName, cell.getColumnIndex());
            log.info("Column: {} -> Index: {}", columnName, cell.getColumnIndex());
        }
        
        return columnMap;
    }
    
    private Map<String, Object> extractRowData(Row row, Map<String, Integer> columnMap) {
        Map<String, Object> rowData = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : columnMap.entrySet()) {
            String columnName = entry.getKey();
            int columnIndex = entry.getValue();
            
            Cell cell = row.getCell(columnIndex);
            Object value = getCellValue(cell);
            rowData.put(columnName, value);
        }
        
        return rowData;
    }
    
    private Object getCellValue(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    return cell.getNumericCellValue();
                }
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    private String getCellValueAsString(Cell cell) {
        Object value = getCellValue(cell);
        return value != null ? value.toString() : "";
    }
    
    private String getStringValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString().trim() : null;
    }
    
    
    private Map<String, String> createFaculties(Set<String> faculties) {
        Map<String, String> facultyIdMap = new HashMap<>();
        
        for (String facultyName : faculties) {
            log.info("Attempting to create Faculty: {}", facultyName);
            try {
                // Kiểm tra xem Faculty đã tồn tại chưa
                var existingFaculty = facultyService.getAllFaculties().stream()
                    .filter(f -> f.getFacultyName().equals(facultyName))
                    .findFirst();
                
                if (existingFaculty.isPresent()) {
                    facultyIdMap.put(facultyName, existingFaculty.get().getId());
                    log.info("Found existing Faculty: {} with ID: {}", facultyName, existingFaculty.get().getId());
                } else {
                    FacultyRequest request = new FacultyRequest();
                    request.setFacultyName(facultyName);
                    var facultyResponse = facultyService.createFaculty(request);
                    facultyIdMap.put(facultyName, facultyResponse.getId());
                    log.info("Created new Faculty: {} with ID: {}", facultyName, facultyResponse.getId());
                }
            } catch (Exception e) {
                log.error("Lỗi tạo Faculty {}: {}", facultyName, e.getMessage(), e);
            }
        }
        
        return facultyIdMap;
    }
    
    private Map<String, String> createMajors(Map<String, Map<String, Object>> majorDataMap, List<Map<String, Object>> subjects, Map<String, String> facultyIdMap) {
        Map<String, String> majorIdMap = new HashMap<>();
        
        for (Map.Entry<String, Map<String, Object>> entry : majorDataMap.entrySet()) {
            String majorName = entry.getKey();
            Map<String, Object> majorData = entry.getValue();
            
            log.info("Attempting to create Major: {}", majorName);
            try {
                String facultyId = findFacultyForMajor(majorName, subjects, facultyIdMap);
                log.info("Found facultyId for Major {}: {}", majorName, facultyId);
                
                if (facultyId == null) {
                    log.warn("Major {} không có Faculty, bỏ qua", majorName);
                    continue;
                }
                
                // Kiểm tra xem Major đã tồn tại chưa
                var existingMajor = majorService.getAllMajors().stream()
                    .filter(m -> m.getMajorName().equals(majorName))
                    .findFirst();
                
                if (existingMajor.isPresent()) {
                    majorIdMap.put(majorName, existingMajor.get().getId());
                    log.info("Found existing Major: {} with ID: {}", majorName, existingMajor.get().getId());
                } else {
                    MajorRequest request = new MajorRequest();
                    request.setMajorName(majorName);
                    
                    // Lấy dữ liệu thực từ Excel
                    Integer numberOfStudents = getIntValue(majorData, "Sỹ số");
                    request.setNumberOfStudents(numberOfStudents != null ? numberOfStudents : 100);
                    
                    // Lấy classYear từ cột "Khoa" (index 1) - năm học
                    String classYear = getStringValue(majorData, "Khoa");
                    // Kiểm tra xem có phải là năm không
                    if (classYear != null && classYear.matches("\\d{4}\\.?\\d*")) {
                        request.setClassYear(classYear);
                    } else {
                        request.setClassYear("2024"); // Default nếu không phải năm
                    }
                    
                    request.setFacultyId(facultyId);
                    
                    var majorResponse = majorService.createMajor(request);
                    majorIdMap.put(majorName, majorResponse.getId());
                    log.info("Created new Major: {} with {} students, class year: {} with ID: {}", 
                        majorName, numberOfStudents, classYear, majorResponse.getId());
                }
            } catch (Exception e) {
                log.error("Lỗi tạo Major {}: {}", majorName, e.getMessage(), e);
            }
        }
        
        return majorIdMap;
    }
    
    private int createSubjects(List<Map<String, Object>> subjects, Map<String, String> facultyIdMap, Map<String, String> majorIdMap) {
        int subjectCount = 0;
        
        for (Map<String, Object> subjectData : subjects) {
            try {
                SubjectRequest request = createSubjectRequest(subjectData, facultyIdMap, majorIdMap);
                if (request != null) {
                    // Kiểm tra xem Subject đã tồn tại chưa (dựa vào subjectName)
                    var existingSubject = subjectService.getAllSubjects().stream()
                        .filter(s -> s.getSubjectName().equals(request.getSubjectName()))
                        .findFirst();
                    
                    if (existingSubject.isPresent()) {
                        log.info("Found existing Subject: {} with ID: {}", request.getSubjectName(), existingSubject.get().getId());
                    } else {
                        subjectService.createSubject(request);
                        subjectCount++;
                        log.info("Created new Subject: {}", request.getSubjectName());
                    }
                }
            } catch (Exception e) {
                log.warn("Lỗi tạo Subject: {}", e.getMessage());
            }
        }
        
        return subjectCount;
    }
    
    private String findFacultyForMajor(String majorName, List<Map<String, Object>> subjects, Map<String, String> facultyIdMap) {
        for (Map<String, Object> subject : subjects) {
            String subjectMajor = getStringValue(subject, "Nganh");
            String subjectFaculty = getStringValue(subject, "Khoa");
            
            // Kiểm tra xem có phải là tên khoa không (không phải năm)
            if (majorName.equals(subjectMajor) && subjectFaculty != null && 
                !subjectFaculty.matches("\\d{4}\\.?\\d*")) { // Không phải năm như 2022, 2023, 2024
                return facultyIdMap.get(subjectFaculty);
            }
        }
        
        // Fallback to first faculty if not found
        if (!facultyIdMap.isEmpty()) {
            return facultyIdMap.values().iterator().next();
        } else {
            log.warn("Không tìm thấy Faculty nào cho Major: {}", majorName);
            return null;
        }
    }
    
    private SubjectRequest createSubjectRequest(Map<String, Object> data, 
                                               Map<String, String> facultyIdMap, 
                                               Map<String, String> majorIdMap) {
        try {
            SubjectRequest request = new SubjectRequest();
            
            // Basic info
            request.setSubjectId(getStringValue(data, "MMH"));
            request.setSubjectName(getStringValue(data, "TMH"));
            
            // Numbers
            Integer totalStudents = getIntValue(data, "Sỹ số");
            Integer numberOfClasses = getIntValue(data, "Số lớp");
            Integer studentsPerClass = (totalStudents != null && numberOfClasses != null && numberOfClasses > 0) 
                ? totalStudents / numberOfClasses 
                : 50; // Default nếu không tính được
            request.setStudentsPerClass(studentsPerClass);
            request.setNumberOfClasses(numberOfClasses != null ? numberOfClasses : 1);
            Integer credits = getIntValue(data, "TC");
            request.setCredits(credits != null ? credits : 0);
            
            // Hours - xử lý null values
            Integer theoryHours = getIntValue(data, "Ly thuyet");
            request.setTheoryHours(theoryHours != null ? theoryHours : 0);
            
            Integer exerciseHours = getIntValue(data, "TL/BT");
            request.setExerciseHours(exerciseHours != null ? exerciseHours : 0);
            
            Integer projectHours = getIntValue(data, "BT lớn");
            request.setProjectHours(projectHours != null ? projectHours : 0);
            
            Integer labHours = getIntValue(data, "TN/TH");
            request.setLabHours(labHours != null ? labHours : 0);
            
            Integer selfStudyHours = getIntValue(data, "Tự học");
            request.setSelfStudyHours(selfStudyHours != null ? selfStudyHours : 0);
            
            // Other info
            request.setDepartment(getStringValue(data, "Bo mon"));
            request.setExamFormat(getStringValue(data, "Hinh thuc thi (dự kiến)"));
            // Lấy classYear từ cột "Khoa" (index 1) - năm học
            String classYear = getStringValue(data, "Khoa");
            if (classYear != null && classYear.matches("\\d{4}\\.?\\d*")) {
                request.setClassYear(classYear);
            } else {
                request.setClassYear("2024"); // Default nếu không phải năm
            }
            
            // Faculty and Major
            String facultyName = getStringValue(data, "Khoa");
            String majorName = getStringValue(data, "Nganh");
            
            // Kiểm tra xem có phải là tên khoa không (không phải năm)
            if (facultyName != null && facultyName.matches("\\d{4}\\.?\\d*")) {
                facultyName = null; // Bỏ qua nếu là năm
            }
            
            if (facultyName != null && majorName != null) {
                request.setFacultyId(facultyIdMap.get(facultyName));
                request.setMajorId(majorIdMap.get(majorName));
                request.setMajorName(majorName);
                request.setNumberOfStudents(getIntValue(data, "Sỹ số"));
                
                return request;
            }
            
        } catch (Exception e) {
            log.warn("Lỗi tạo SubjectRequest: {}", e.getMessage());
        }
        
        return null;
    }
    
    private Integer getIntValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
