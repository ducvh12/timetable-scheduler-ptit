package com.ptit.schedule.utils;

import java.time.LocalDate;

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

}