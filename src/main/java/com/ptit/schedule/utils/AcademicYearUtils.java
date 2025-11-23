package com.ptit.schedule.utils;

import org.modelmapper.internal.Pair;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AcademicYearUtils {

    public static String resolveAcademicYear(String academicYear) {
        if (academicYear != null && !academicYear.trim().isEmpty()) {
            return academicYear;
        }

        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        // Năm học bắt đầu từ tháng 8
        int startYear = (month >= 8) ? year : year - 1;

        return startYear + "-" + (startYear + 1);
    }

    public static Pair<String, String> splitSemesterAndYear(String input) {
        if (input == null) return null;

        // chuẩn hóa dấu gạch nối
        String normalized = input.replace("–", "-");

        // Regex: tách theo dấu "-" nhưng lấy đúng nhóm 2 bên
        Pattern pattern = Pattern.compile("^(.+?)\\s*-\\s*(\\d{4}-\\d{4})$");
        Matcher matcher = pattern.matcher(normalized);

        if (matcher.matches()) {
            String semester = matcher.group(1).trim();
            String academicYear = matcher.group(2).trim();
            return Pair.of(semester, academicYear);
        }

        return null;
    }
}