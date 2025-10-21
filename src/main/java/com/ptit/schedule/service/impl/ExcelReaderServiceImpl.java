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

    @Override
    public List<SubjectRequest> readSubjectsFromExcel(MultipartFile file) {
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
                SubjectRequest subject = createSubjectFromRow(row, formatter);
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

    private SubjectRequest createSubjectFromRow(Row row, DataFormatter formatter) {
        try {
            SubjectRequest subject = SubjectRequest.builder()
                    .subjectCode(formatter.formatCellValue(row.getCell(0)))   // A
                    .classYear(formatter.formatCellValue(row.getCell(1)))         // B
                    .majorId(formatter.formatCellValue(row.getCell(2)))        // C
                    .programType(formatter.formatCellValue(row.getCell(4))) // D
                    .numberOfStudents(parseIntSafe(formatter.formatCellValue(row.getCell(6)))) // E
                    .numberOfClasses(parseIntSafe(formatter.formatCellValue(row.getCell(7)))) // F
                    .subjectName(formatter.formatCellValue(row.getCell(9)))  // H
                    .credits(parseIntSafe(formatter.formatCellValue(row.getCell(10))))          // I// J
                    .theoryHours(parseIntSafe(formatter.formatCellValue(row.getCell(12))))     // K
                    .exerciseHours(parseIntSafe(formatter.formatCellValue(row.getCell(13))))   // L
                    .projectHours(parseIntSafe(formatter.formatCellValue(row.getCell(14))))    // M
                    .labHours(parseIntSafe(formatter.formatCellValue(row.getCell(15))))        // N
                    .selfStudyHours(parseIntSafe(formatter.formatCellValue(row.getCell(16))))  // O
                    .facultyId(formatter.formatCellValue(row.getCell(18)))
                    .department(formatter.formatCellValue(row.getCell(19)))                // P// R
                    .examFormat(formatter.formatCellValue(row.getCell(21)))                    // S
                    .build();


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
