package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.ScheduleEntry;
import com.ptit.schedule.exception.FileProcessingException;
import com.ptit.schedule.exception.InvalidDataException;
import com.ptit.schedule.service.ScheduleExcelReaderService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
public class ScheduleExcelReaderServiceImpl extends BaseExcelReaderService implements ScheduleExcelReaderService {

    // Basic info columns
    private static final int COL_SUBJECT_CODE = 1;    // B - Mã môn học
    private static final int COL_SUBJECT_NAME = 2;    // C - Tên môn học
    private static final int COL_CLASS_GROUP = 3;     // D - Nhóm/Lớp
    private static final int COL_DAY_OF_WEEK = 6;     // G - Thứ
    private static final int COL_SHIFT = 7;           // H - Kíp
    private static final int COL_START_PERIOD = 8;    // I - Tiết bắt đầu
    private static final int COL_NUMBER_OF_PERIODS = 9; // J - Số tiết
    private static final int COL_ROOM = 10;           // K - Phòng
    private static final int COL_BUILDING = 11;       // L - Tòa nhà
    private static final int COL_STUDENT_COUNT = 19;  // T - Số SV
    private static final int COL_TEACHER_ID = 21;     // V - Mã GV
    private static final int COL_TEACHER_NAME = 22;   // W - Tên GV

    // Week columns (AB-AR: 27-43)
    private static final int COL_WEEK_START = 27;     // AB - Tuần 1
    private static final int COL_WEEK_END = 43;       // AR - Tuần 17
    private static final int TOTAL_WEEKS = 17;

    private static final int MIN_COLUMNS = 44;
    private static final int HEADER_ROWS = 3;         // Skip 3 header rows

    @Override
    public List<ScheduleEntry> readScheduleFromExcel(MultipartFile file) {
        List<ScheduleEntry> scheduleEntries = new ArrayList<>();

        try (Workbook workbook = openWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            log.info("Reading schedule from Excel. Total rows: {}", sheet.getLastRowNum());

            // Read data rows (skip header rows)
            for (int i = HEADER_ROWS; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);

                if (isRowEmpty(row)) {
                    continue;
                }

                try {
                    ScheduleEntry entry = createScheduleEntryFromRow(row, formatter, i);

                    if (entry != null && isValidEntry(entry)) {
                        scheduleEntries.add(entry);
                    }
                } catch (Exception e) {
                    log.warn("Error parsing row {}: {}", i + 1, e.getMessage());
                }
            }

            log.info("Successfully parsed {} schedule entries", scheduleEntries.size());

        } catch (IOException e) {
            log.error("Error reading schedule Excel file", e);
            throw new FileProcessingException("Lỗi đọc file thời khóa biểu: " + e.getMessage(), e);
        }

        return scheduleEntries;
    }

    @Override
    public boolean validateScheduleExcelFormat(MultipartFile file) {
        try (Workbook workbook = openWorkbook(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(0);

            validateColumnCount(headerRow, MIN_COLUMNS, "Thời khóa biểu");
            return true;

        } catch (Exception e) {
            log.error("Invalid schedule Excel format", e);
            return false;
        }
    }

    private ScheduleEntry createScheduleEntryFromRow(Row row, DataFormatter formatter, int rowIndex) {
        String subjectCode = getCellValue(row, COL_SUBJECT_CODE, formatter);
        String building = getCellValue(row, COL_BUILDING, formatter);
        String room = getCellValue(row, COL_ROOM, formatter);

        // Skip online/LMS entries
        if (isOnlineEntry(building, room)) {
            return null;
        }

        // Build full room name
        String fullRoom = building.isEmpty() ? room : room + " - " + building;

        // Parse time slots from week columns
        List<ScheduleEntry.TimeSlot> timeSlots = parseTimeSlots(row, formatter);

        if (timeSlots.isEmpty()) {
            log.debug("No time slots found for subject {} at row {}", subjectCode, rowIndex + 1);
            return null;
        }

        return ScheduleEntry.builder()
                .subjectCode(subjectCode)
                .subjectName(getCellValue(row, COL_SUBJECT_NAME, formatter))
                .classGroup(getCellValue(row, COL_CLASS_GROUP, formatter))
                .room(fullRoom)
                .building(building)
                .teacherId(getCellValue(row, COL_TEACHER_ID, formatter))
                .teacherName(getCellValue(row, COL_TEACHER_NAME, formatter))
                .studentCount(parseIntSafe(getCellValue(row, COL_STUDENT_COUNT, formatter)))
                .timeSlots(timeSlots)
                .build();
    }

    private List<ScheduleEntry.TimeSlot> parseTimeSlots(Row row, DataFormatter formatter) {
        List<ScheduleEntry.TimeSlot> timeSlots = new ArrayList<>();

        // Get common time info (same for all weeks)
        String dayOfWeek = convertDayOfWeek(getCellValue(row, COL_DAY_OF_WEEK, formatter));
        String shift = getCellValue(row, COL_SHIFT, formatter);
        String startPeriod = getCellValue(row, COL_START_PERIOD, formatter);
        String numberOfPeriods = getCellValue(row, COL_NUMBER_OF_PERIODS, formatter);

        // Check each week column for 'x' marker
        for (int weekNum = 1; weekNum <= TOTAL_WEEKS; weekNum++) {
            int colIndex = COL_WEEK_START + (weekNum - 1);
            String cellValue = getCellValue(row, colIndex, formatter).toLowerCase();

            if (cellValue.contains("x")) {
                ScheduleEntry.TimeSlot timeSlot = ScheduleEntry.TimeSlot.builder()
                        .date("Tuần " + weekNum)
                        .dayOfWeek(dayOfWeek)
                        .shift(shift)
                        .startPeriod(startPeriod)
                        .numberOfPeriods(numberOfPeriods)
                        .build();
                timeSlots.add(timeSlot);
            }
        }

        return timeSlots;
    }

    private boolean isOnlineEntry(String building, String room) {
        String[] onlineKeywords = {"online", "lms"};

        for (String keyword : onlineKeywords) {
            if (building.equalsIgnoreCase(keyword) || room.equalsIgnoreCase(keyword)) {
                return true;
            }
        }
        return false;
    }

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

    private boolean isValidEntry(ScheduleEntry entry) {
        return entry.getSubjectCode() != null && !entry.getSubjectCode().isEmpty() &&
                entry.getTeacherId() != null && !entry.getTeacherId().isEmpty() &&
                entry.getRoom() != null && !entry.getRoom().isEmpty() &&
                entry.getTimeSlots() != null && !entry.getTimeSlots().isEmpty();
    }
}