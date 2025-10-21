package com.ptit.schedule;

import com.ptit.schedule.dto.SubjectRequest;
import com.ptit.schedule.entity.Subject;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class test {

    public static void main(String[] args) {
        String excelFilePath = "data/subjects.xlsx"; // 🔁 Cập nhật đường dẫn ở đây
        List<SubjectRequest> subjects = readSubjectsFromExcel(excelFilePath);

        // In ra tất cả các dòng
        for (SubjectRequest s : subjects) {
            System.out.println(s);
        }
    }

    public static List<SubjectRequest> readSubjectsFromExcel(String filePath) {
        List<SubjectRequest> subjects = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Bỏ qua dòng tiêu đề

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
                        .facultyId(formatter.formatCellValue(row.getCell(17)))
                        .department(formatter.formatCellValue(row.getCell(18)))                // P// R
                        .examFormat(formatter.formatCellValue(row.getCell(20)))                    // S
                        .build();

                subjects.add(subject);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return subjects;
    }

    private static int parseIntSafe(String value) {
        try {
            return (value == null || value.isBlank()) ? -1 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
