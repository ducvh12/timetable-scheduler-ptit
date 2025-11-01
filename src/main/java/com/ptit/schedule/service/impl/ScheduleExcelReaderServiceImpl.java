package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.ScheduleEntry;
import com.ptit.schedule.service.ScheduleExcelReaderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
public class ScheduleExcelReaderServiceImpl implements ScheduleExcelReaderService {

    // Mapping dựa trên Excel layout từ ảnh
    private static final int COL_SUBJECT_CODE = 1;     // Cột B - Mã môn học
    private static final int COL_SUBJECT_NAME = 2;     // Cột C - Tên môn học
    private static final int COL_CLASS_GROUP = 3;      // Cột D - Nhóm/Phóng
    private static final int COL_ROOM = 10;             // Cột K - Phòng
    private static final int COL_STUDENT_COUNT = 19;    // Cột T - Số SV
    private static final int COL_TEACHER_ID = 21;       // Cột V - Mã GV
    private static final int COL_TEACHER_NAME = 22;     // Cột W - Tên GV
    private static final int COL_BUILDING = 11;
    
    // Các cột chứa thông tin thời gian từ ảnh Excel
    private static final int COL_DAY_OF_WEEK = 6;       // Cột G - Thứ (2, 3, 4, 5, 6, 7, CN)
    private static final int COL_SHIFT = 7;             // Cột H - Kíp (1, 2, 3...)
    private static final int COL_START_PERIOD = 8;      // Cột I - Tiết BB (1, 2, 3, 4...)
    private static final int COL_NUMBER_OF_PERIODS = 9; // Cột J - Số tiết (1, 2, 3, 4...)
    
    // Các cột tuần từ AB đến AR (27-43)
    private static final int COL_WEEK_1 = 27;   // Cột AB - Tuần 1
    private static final int COL_WEEK_2 = 28;   // Cột AC - Tuần 2
    private static final int COL_WEEK_3 = 29;   // Cột AD - Tuần 3
    private static final int COL_WEEK_4 = 30;   // Cột AE - Tuần 4
    private static final int COL_WEEK_5 = 31;   // Cột AF - Tuần 5
    private static final int COL_WEEK_6 = 32;   // Cột AG - Tuần 6
    private static final int COL_WEEK_7 = 33;   // Cột AH - Tuần 7
    private static final int COL_WEEK_8 = 34;   // Cột AI - Tuần 8
    private static final int COL_WEEK_9 = 35;   // Cột AJ - Tuần 9
    private static final int COL_WEEK_10 = 36;  // Cột AK - Tuần 10
    private static final int COL_WEEK_11 = 37;  // Cột AL - Tuần 11
    private static final int COL_WEEK_12 = 38;  // Cột AM - Tuần 12
    private static final int COL_WEEK_13 = 39;  // Cột AN - Tuần 13
    private static final int COL_WEEK_14 = 40;  // Cột AO - Tuần 14
    private static final int COL_WEEK_15 = 41;  // Cột AP - Tuần 15
    private static final int COL_WEEK_16 = 42;  // Cột AQ - Tuần 16
    private static final int COL_WEEK_17 = 43;  // Cột AR - Tuần 17
    
    // Cột thời gian bắt đầu từ cột AB (index 27) - theo ảnh
    private static final int TIME_SLOT_START_COL = 27;
    
    // Array các cột tuần để dễ dàng iterate
    private static final int[] WEEK_COLUMNS = {
        COL_WEEK_1, COL_WEEK_2, COL_WEEK_3, COL_WEEK_4, COL_WEEK_5, COL_WEEK_6,
        COL_WEEK_7, COL_WEEK_8, COL_WEEK_9, COL_WEEK_10, COL_WEEK_11, COL_WEEK_12,
        COL_WEEK_13, COL_WEEK_14, COL_WEEK_15, COL_WEEK_16, COL_WEEK_17
    };

    @Override
    public List<ScheduleEntry> readScheduleFromExcel(MultipartFile file) {
        List<ScheduleEntry> scheduleEntries = new ArrayList<>();

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            log.info("Excel file has {} rows", sheet.getLastRowNum());

            // Đọc từng dòng dữ liệu (bỏ qua 3 dòng header đầu)
            for (int i = 3; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                ScheduleEntry entry = createScheduleEntryFromRow(row, formatter);
                if (entry != null && isValidEntry(entry)) {
//                    log.debug("Successfully parsed entry: {}", entry.getSubjectCode());
                    scheduleEntries.add(entry);
                } else if (entry != null) {
                    log.warn("Invalid entry at row {}: {}", i, entry.getSubjectCode());
                }
            }

//            log.info("Successfully parsed {} schedule entries", scheduleEntries.size());

        } catch (IOException e) {
            log.error("Error reading schedule Excel file", e);
            throw new RuntimeException("Không thể đọc file Excel thời khóa biểu: " + e.getMessage());
        }

        return scheduleEntries;
    }

    @Override
    public boolean validateScheduleExcelFormat(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            return false;
        }

        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            
            // Kiểm tra có đủ cột không
            if (headerRow == null || headerRow.getLastCellNum() < TIME_SLOT_START_COL + 10) {
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error validating Excel format", e);
            return false;
        }
    }

    private ScheduleEntry createScheduleEntryFromRow(Row row, DataFormatter formatter) {
        try {
            // Đọc thông tin cơ bản
            String subjectCode = getCellValue(row, COL_SUBJECT_CODE, formatter);
            String subjectName = getCellValue(row, COL_SUBJECT_NAME, formatter);
            String classGroup = getCellValue(row, COL_CLASS_GROUP, formatter);
            String room = getCellValue(row, COL_ROOM, formatter);
            String building = getCellValue(row, COL_BUILDING, formatter);
            String teacherId = getCellValue(row, COL_TEACHER_ID, formatter);
            String teacherName = getCellValue(row, COL_TEACHER_NAME, formatter);
            if(building.equals("Online") || building.equals("LMS") || room.equals("LMS")) return  null;
            
            int studentCount = parseIntSafe(getCellValue(row, COL_STUDENT_COUNT, formatter));

            // Tạo room đầy đủ (phòng + tòa nhà)
            String fullRoom = room;
            if (!building.isEmpty()) {
                fullRoom = room + " - " + building;
            }

            // Đọc thời gian (tìm các ô có 'x')
            List<ScheduleEntry.TimeSlot> timeSlots = parseTimeSlots(row, formatter);

            ScheduleEntry se =  ScheduleEntry.builder()
                    .subjectCode(subjectCode)
                    .subjectName(subjectName)
                    .classGroup(classGroup)
                    .room(fullRoom)
                    .teacherId(teacherId)
                    .teacherName(teacherName)
                    .studentCount(studentCount)
                    .timeSlots(timeSlots)
                    .building(building)
                    .build();
//            System.out.println("Parsed entry: " + se);
            return se;

        } catch (Exception e) {
            log.warn("Error parsing row {}: {}", row.getRowNum(), e.getMessage());
            return null;
        }
    }

    private List<ScheduleEntry.TimeSlot> parseTimeSlots(Row row, DataFormatter formatter) {
        List<ScheduleEntry.TimeSlot> timeSlots = new ArrayList<>();

        // Đọc thông tin thời gian từ các cột cụ thể
        String dayOfWeekStr = getCellValue(row, COL_DAY_OF_WEEK, formatter);     // Cột F - Thứ
        String shiftStr = getCellValue(row, COL_SHIFT, formatter);               // Cột H - Kíp  
        String startPeriodStr = getCellValue(row, COL_START_PERIOD, formatter);  // Cột I - Tiết BB
        String numberOfPeriodsStr = getCellValue(row, COL_NUMBER_OF_PERIODS, formatter); // Cột J - Số tiết

        // Convert dayOfWeek number to text
        String dayOfWeek = convertDayOfWeek(dayOfWeekStr);

        // Kiểm tra các cột tuần để tìm 'x'
        for (int i = 0; i < WEEK_COLUMNS.length; i++) {
            int colIndex = WEEK_COLUMNS[i];
            
            Cell cell = row.getCell(colIndex);
            if (cell != null) {
                String cellValue = formatter.formatCellValue(cell).trim().toLowerCase();
                
                // Nếu có 'x' thì đây là time slot được sử dụng
                if (cellValue.contains("x")) {
                    // Tạo time slot với thông tin từ các cột cụ thể
                    int weekNumber = i + 1; // Tuần 1, 2, 3...
                    
                    ScheduleEntry.TimeSlot timeSlot = ScheduleEntry.TimeSlot.builder()
                            .date("Tuần " + weekNumber)                    // "Tuần 1", "Tuần 2"...
                            .dayOfWeek(dayOfWeek)                          // "Thứ 2", "Thứ 3"...
                            .shift(shiftStr)                               // "1", "2", "3"...
                            .startPeriod(startPeriodStr)                   // "1", "2", "3"...
                            .numberOfPeriods(numberOfPeriodsStr)           // "1", "2", "3"...
                            .build();
                    timeSlots.add(timeSlot);
                }
            }
        }

        return timeSlots;
    }
    
    private String getCellValue(Row row, int colIndex, DataFormatter formatter) {
        Cell cell = row.getCell(colIndex);
        return cell != null ? formatter.formatCellValue(cell).trim() : "";
    }

    /**
     * Convert day of week number to text
     */
    private String convertDayOfWeek(String dayOfWeekStr) {
        if (dayOfWeekStr == null || dayOfWeekStr.trim().isEmpty()) {
            return "Không xác định";
        }
        
        switch (dayOfWeekStr.trim()) {
            case "2": return "Thứ 2";
            case "3": return "Thứ 3";
            case "4": return "Thứ 4";
            case "5": return "Thứ 5";
            case "6": return "Thứ 6";
            case "7": return "Thứ 7";
            case "CN": return "Chủ nhật";
            default: return "Thứ " + dayOfWeekStr;
        }
    }

    private int parseIntSafe(String value) {
        try {
            return (value == null || value.isEmpty()) ? 0 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private boolean isValidEntry(ScheduleEntry entry) {
        return entry.getSubjectCode() != null && !entry.getSubjectCode().isEmpty() &&
               entry.getTeacherId() != null && !entry.getTeacherId().isEmpty() &&
               entry.getRoom() != null && !entry.getRoom().isEmpty() &&
               entry.getTimeSlots() != null && !entry.getTimeSlots().isEmpty();
    }
}