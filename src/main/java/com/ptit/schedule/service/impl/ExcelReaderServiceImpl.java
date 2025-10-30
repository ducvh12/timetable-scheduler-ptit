package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.SubjectRequest;
import com.ptit.schedule.service.ExcelReaderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ExcelReaderServiceImpl implements ExcelReaderService {
    // Column indices (0-based)
    private static final int COL_SUBJECT_CODE = 0; // A
    private static final int COL_CLASS_YEAR = 1;   // B
    private static final int COL_MAJOR_ID = 2;     // C
    private static final int COL_PROGRAM_TYPE = 4; // D
    private static final int COL_NUMBER_OF_STUDENTS = 6; // E
    private static final int COL_NUMBER_OF_CLASSES = 7;  // F
    private static final int COL_SUBJECT_NAME = 9; // H
    private static final int COL_CREDITS = 10;     // I/J
    private static final int COL_THEORY_HOURS = 12; // K
    private static final int COL_EXERCISE_HOURS = 13; // L
    private static final int COL_PROJECT_HOURS = 14; // M
    private static final int COL_LAB_HOURS = 15;   // N
    private static final int COL_SELF_STUDY_HOURS = 16; // O
    private static final int COL_FACULTY_ID = 18;  // S? (as used before)
    private static final int COL_DEPARTMENT = 19;  // T? (as used before)
    private static final int COL_EXAM_FORMAT = 22; // V? (as used before)

    
    @Override
    public List<SubjectRequest> readSubjectsFromExcel(MultipartFile file, String semester) {
        List<SubjectRequest> subjects = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên
            DataFormatter formatter = new DataFormatter();

            for (Row row : sheet) {
                if (row == null || row.getRowNum() == 0){
//                    System.out.println("Skip row: " + row.getRowNum());
                    continue;
                }
//                System.out.println(row);
                SubjectRequest subject = createSubjectFromRow(row, formatter, semester);
                if (!subject.getSubjectCode().isBlank() && subject != null) subjects.add(subject);
            }

        } catch (IOException e) {
            log.error("Error reading Excel file", e);
            throw new RuntimeException("Không thể đọc file Excel: " + e.getMessage());
        }
        
        return subjects;
    }

    private static int parseIntSafe(String value) {
        try {
            return (value == null || value.isBlank()) ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private SubjectRequest createSubjectFromRow(Row row, DataFormatter formatter, String semester) {
        try {
            String tmp = formatter.formatCellValue(row.getCell(4));

            SubjectRequest subject = SubjectRequest.builder()
                    .subjectCode(formatter.formatCellValue(row.getCell(COL_SUBJECT_CODE)))   // A
                    .classYear(formatter.formatCellValue(row.getCell(COL_CLASS_YEAR)))         // B
                    .majorId(formatter.formatCellValue(row.getCell(COL_MAJOR_ID)))        // C
                    .programType(formatter.formatCellValue(row.getCell(COL_PROGRAM_TYPE))) // D
                    .numberOfStudents(parseIntSafe(formatter.formatCellValue(row.getCell(COL_NUMBER_OF_STUDENTS)))) // E
                    .numberOfClasses(parseIntSafe(formatter.formatCellValue(row.getCell(COL_NUMBER_OF_CLASSES)))) // F
                    .subjectName(formatter.formatCellValue(row.getCell(COL_SUBJECT_NAME)))  // H
                    .credits(parseIntSafe(formatter.formatCellValue(row.getCell(COL_CREDITS))))          // I// J
                    .theoryHours(parseIntSafe(formatter.formatCellValue(row.getCell(COL_THEORY_HOURS))))     // K
                    .exerciseHours(parseIntSafe(formatter.formatCellValue(row.getCell(COL_EXERCISE_HOURS))))   // L
                    .projectHours(parseIntSafe(formatter.formatCellValue(row.getCell(COL_PROJECT_HOURS))))    // M
                    .labHours(parseIntSafe(formatter.formatCellValue(row.getCell(COL_LAB_HOURS))))        // N
                    .selfStudyHours(parseIntSafe(formatter.formatCellValue(row.getCell(COL_SELF_STUDY_HOURS))))  // O
                    .facultyId(formatter.formatCellValue(row.getCell(COL_FACULTY_ID)))
                    .department(formatter.formatCellValue(row.getCell(COL_DEPARTMENT)))                // P// R
                    .examFormat(formatter.formatCellValue(row.getCell(COL_EXAM_FORMAT)))
                    .semester(semester)// S
                    .build();

            if(tmp != null && !tmp.isBlank()){
//                System.out.println(subject.getClassYear() + " - " + subject.getMajorId() + " - " + tmp);
                subject.setProgramType(tmp);
            } else {
                subject.setProgramType("Chính quy");
            }
            return subject;

        } catch (Exception e) {
            log.error("Error parsing row {}: {}", e.getMessage());
            return null;
        }
    }


    @Override
    public List<String> validateExcelData(List<SubjectRequest> subjects) {
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < subjects.size(); i++) {
            SubjectRequest subject = subjects.get(i);
            int rowNumber = i + 2; // +2 vì bỏ qua header và index bắt đầu từ 0

            // Validate required fields
            if (subject.getSubjectCode() == null || subject.getSubjectCode().trim().isEmpty()) {
                errors.add("Dòng " + rowNumber + ": Subject ID không được để trống");
            }

            if (subject.getSubjectName() == null || subject.getSubjectName().trim().isEmpty()) {
                errors.add("Dòng " + rowNumber + ": Subject Name không được để trống");
            }

            if (subject.getStudentsPerClass() == null || subject.getStudentsPerClass() <= 0) {
                errors.add("Dòng " + rowNumber + ": Students Per Class phải lớn hơn 0");
            }

            if (subject.getNumberOfClasses() == null || subject.getNumberOfClasses() <= 0) {
                errors.add("Dòng " + rowNumber + ": Number of Classes phải lớn hơn 0");
            }

            if (subject.getCredits() == null || subject.getCredits() <= 0) {
                errors.add("Dòng " + rowNumber + ": Credits phải lớn hơn 0");
            }

            if (subject.getFacultyId() == null || subject.getFacultyId().trim().isEmpty()) {
                errors.add("Dòng " + rowNumber + ": Faculty ID không được để trống");
            }

            if (subject.getMajorId() == null || subject.getMajorId().trim().isEmpty()) {
                errors.add("Dòng " + rowNumber + ": Major ID không được để trống");
            }

            if (subject.getMajorName() == null || subject.getMajorName().trim().isEmpty()) {
                errors.add("Dòng " + rowNumber + ": Major Name không được để trống");
            }

            if (subject.getClassYear() == null || subject.getClassYear().trim().isEmpty()) {
                errors.add("Dòng " + rowNumber + ": Class Year không được để trống");
            }
        }

        return errors;
    }
}
