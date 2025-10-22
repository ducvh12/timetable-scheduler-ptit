package com.ptit.schedule.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Room data model for loading from rooms.json
 * This is a simple POJO for JSON data, not a JPA entity
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoomData {
    private String phong;        // Room code (primary identifier)
    private String ma_phong;     // Room ID/number  
    private Integer capacity;    // Room capacity (number of students)
    private String type;         // Room type: "general", "nt", "english", "clc", "year2024"
    private String note;         // Room description/note for constraint matching
    
    /**
     * Helper method to get capacity as integer, defaulting to 0 if null
     */
    public int getCapacityValue() {
        return capacity != null ? capacity : 0;
    }
    
    /**
     * Helper method to get type, defaulting to "general" if null
     */
    public String getTypeValue() {
        return type != null ? type : "general";
    }
    
    /**
     * Helper method to get note in lowercase for case-insensitive matching
     */
    public String getNoteValue() {
        return note != null ? note.toLowerCase() : "";
    }
}