package com.ptit.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Represents a time slot in rotating schedule system
 * Maps directly to Python's rotating_slots structure
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimetableSlot {
    private Integer thu; // Day of week (2-7)
    private String session; // "sang" or "chieu"
    
    /**
     * Get kip set for session - exact Python logic
     */
    public Set<Integer> getKipSet() {
        return "sang".equals(session) ? Set.of(1, 2) : Set.of(3, 4);
    }
}