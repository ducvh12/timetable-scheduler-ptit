package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.ExcelImportResult;
import com.ptit.schedule.dto.SubjectRequest;
import com.ptit.schedule.exception.FileProcessingException;
import com.ptit.schedule.service.ExcelReaderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class ExcelReaderServiceImpl extends BaseExcelReaderService implements ExcelReaderService {

    // Column indices (0-based) - Documented clearly
    private static final int COL_SUBJECT_CODE = 0;        // A - Mã môn học
    private static final int COL_CLASS_YEAR = 1;          // B - Khóa
    private static final int COL_MAJOR_ID = 2;            // C - Mã ngành
    private static final int COL_PROGRAM_TYPE = 4;        // E - Loại hệ
    private static final int COL_NUMBER_OF_STUDENTS = 6;  // G - Số SV
    private static final int COL_NUMBER_OF_CLASSES = 7;   // H - Số lớp
    private static final int COL_SUBJECT_NAME = 9;        // J - Tên môn
    private static final int COL_CREDITS = 10;            // K - Số tín chỉ
    private static final int COL_THEORY_HOURS = 12;       // M - Giờ lý thuyết
    private static final int COL_EXERCISE_HOURS = 13;     // N - Giờ bài tập
    private static final int COL_PROJECT_HOURS = 14;      // O - Giờ đồ án
    private static final int COL_LAB_HOURS = 15;          // P - Giờ thực hành
    private static final int COL_SELF_STUDY_HOURS = 16;   // Q - Giờ tự học
    private static final int COL_FACULTY_ID = 18;         // S - Mã khoa
    private static final int COL_DEPARTMENT = 19;         // T - Bộ môn
    private static final int COL_EXAM_FORMAT = 22;        // W - Hình thức thi
    private static final int COL_IS_COMMON = 24;          // Y - Môn chung

    private static final int MIN_COLUMNS = 25;            // Minimum required columns
    private static final int HEADER_ROW = 0;              // First row is header

    @Override
    @Deprecated
    public List<SubjectRequest> readSubjectsFromExcel(MultipartFile file, String semesterName, String academicYear) {
        ExcelImportResult result = readAndValidateSubjectsFromExcel(file, semesterName, academicYear);
        return result.getValidSubjects();
    }

    @Override
    public ExcelImportResult readAndValidateSubjectsFromExcel(MultipartFile file, String semesterName, String academicYear) {
        List<SubjectRequest> allSubjects = new ArrayList<>();
        ExcelImportResult result = ExcelImportResult.builder()
                .successCount(0)
                .skippedCount(0)
                .totalRows(0)
                .warnings(new ArrayList<>())
                .validSubjects(new ArrayList<>())
                .build();

        try (Workbook workbook = openWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            // Validate header row
            Row headerRow = sheet.getRow(HEADER_ROW);
            validateColumnCount(headerRow, MIN_COLUMNS, "Chương trình đào tạo");

            int totalRows = sheet.getLastRowNum();
            result.setTotalRows(totalRows);
            
            log.info("Reading subjects from Excel. Total rows: {}", totalRows);

            // Read data rows (skip header)
            for (int i = HEADER_ROW + 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);

                // Skip empty rows
                if (isRowEmpty(row)) {
                    continue;
                }

                try {
                    SubjectRequest subject = createSubjectFromRow(row, formatter, semesterName, academicYear, i);

                    // Validate required fields
                    if (subject != null && isValidSubject(subject)) {
                        allSubjects.add(subject);
                    } else {
                        log.warn("Skipped invalid subject at row {}", i + 1);
                        result.addWarning(String.format("Dòng %d: Dữ liệu không hợp lệ (thiếu thông tin bắt buộc)", i + 1));
                    }
                } catch (Exception e) {
                    log.error("Error reading row {}: {}", i + 1, e.getMessage());
                    result.addWarning(String.format("Dòng %d: %s", i + 1, e.getMessage()));
                }
            }

            log.info("Successfully parsed {} subjects from Excel", allSubjects.size());

            // Validate and filter duplicates
            validateAndFilterDuplicates(allSubjects, result);

        } catch (IOException e) {
            log.error("Error reading Excel file", e);
            throw new FileProcessingException("Lỗi đọc file Excel: " + e.getMessage(), e);
        }

        return result;
    }

    private SubjectRequest createSubjectFromRow(Row row, DataFormatter formatter,
                                                String semesterName, String academicYear, int rowIndex) {

        String subjectCode = getCellValue(row, COL_SUBJECT_CODE, formatter);

        // Skip if no subject code
        if (subjectCode.isBlank()) {
            return null;
        }

        String programType = getCellValue(row, COL_PROGRAM_TYPE, formatter);
        if (programType.isBlank()) {
            programType = "Chính quy"; // Default value
        }

        return SubjectRequest.builder()
                .subjectCode(subjectCode)
                .subjectName(getCellValue(row, COL_SUBJECT_NAME, formatter))
                .classYear(getCellValue(row, COL_CLASS_YEAR, formatter))
                .majorId(getCellValue(row, COL_MAJOR_ID, formatter))
                .programType(programType)
                .numberOfStudents(parseIntSafe(getCellValue(row, COL_NUMBER_OF_STUDENTS, formatter)))
                .numberOfClasses(parseIntSafe(getCellValue(row, COL_NUMBER_OF_CLASSES, formatter)))
                .credits(parseIntSafe(getCellValue(row, COL_CREDITS, formatter)))
                .theoryHours(parseIntSafe(getCellValue(row, COL_THEORY_HOURS, formatter)))
                .exerciseHours(parseIntSafe(getCellValue(row, COL_EXERCISE_HOURS, formatter)))
                .projectHours(parseIntSafe(getCellValue(row, COL_PROJECT_HOURS, formatter)))
                .labHours(parseIntSafe(getCellValue(row, COL_LAB_HOURS, formatter)))
                .selfStudyHours(parseIntSafe(getCellValue(row, COL_SELF_STUDY_HOURS, formatter)))
                .facultyId(getCellValue(row, COL_FACULTY_ID, formatter))
                .department(getCellValue(row, COL_DEPARTMENT, formatter))
                .examFormat(getCellValue(row, COL_EXAM_FORMAT, formatter))
                .isCommon(getCellValue(row, COL_IS_COMMON, formatter).equalsIgnoreCase("chung"))
                .semesterName(semesterName)
                .academicYear(academicYear)
                .build();
    }

    private boolean isValidSubject(SubjectRequest subject) {
        return subject.getSubjectCode() != null && !subject.getSubjectCode().isBlank() &&
                subject.getSubjectName() != null && !subject.getSubjectName().isBlank() &&
                subject.getCredits() != null && subject.getCredits() > 0;
    }

    /**
     * Validate duplicates and filter them out, adding warnings to result
     * Keep only the first occurrence of each subject
     * Unique key: subjectCode + majorId + classYear + semesterName + academicYear
     */
    private void validateAndFilterDuplicates(List<SubjectRequest> allSubjects, ExcelImportResult result) {
        Map<String, Integer> firstSeen = new HashMap<>();
        List<SubjectRequest> validSubjects = new ArrayList<>();
        
        for (int i = 0; i < allSubjects.size(); i++) {
            SubjectRequest subject = allSubjects.get(i);
            
            // Create unique key: subjectCode + majorId + classYear + semesterName + academicYear
            // This matches the DB uniqueness constraint
            String key = subject.getSubjectCode() 
                    + "-" + subject.getMajorId() 
                    + "-" + subject.getClassYear()
                    + "-" + subject.getSemesterName()
                    + "-" + subject.getAcademicYear();

            if (firstSeen.containsKey(key)) {
                // Duplicate found - add warning and skip
                result.addWarning(
                    String.format("Môn '%s' (Ngành: %s, Khóa: %s, HK: %s, Năm: %s) bị trùng: dòng %d và dòng %d - Bỏ qua dòng %d",
                        subject.getSubjectCode(),
                        subject.getMajorId(),
                        subject.getClassYear(),
                        subject.getSemesterName(),
                        subject.getAcademicYear(),
                        firstSeen.get(key) + 2,  // +2 because: +1 for 0-indexed, +1 for header row
                        i + 2,                    // +2 for same reason
                        i + 2)                    // Skipped row
                );
                result.setSkippedCount(result.getSkippedCount() + 1);
            } else {
                // First occurrence - keep it
                firstSeen.put(key, i);
                validSubjects.add(subject);
            }
        }
        
        result.setValidSubjects(validSubjects);
        result.setSuccessCount(validSubjects.size());
        
        log.info("Validation complete: {} valid subjects, {} duplicates skipped", 
                validSubjects.size(), result.getSkippedCount());
    }
}