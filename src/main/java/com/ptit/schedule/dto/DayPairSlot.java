package com.ptit.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a paired-day slot for 60-period subjects
 * Each slot covers 2 consecutive days with the same specific kip
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DayPairSlot {
    private Integer day1;     // First day (thu)
    private Integer day2;     // Second day (thu)
    private Integer kip;      // Specific kip (1, 2, 3, or 4)
    
    /**
     * Get both days as a list
     */
    public List<Integer> getDays() {
        return Arrays.asList(day1, day2);
    }
}
