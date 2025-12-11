package com.ptit.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomPickResult {
    private String roomCode;
    private String roomId;
    private String building;
    private Long databaseRoomId; // Database ID của Room entity
    private Integer distanceScore;
    private boolean isPreferredBuilding;

    public boolean hasRoom() {
        return roomCode != null && !roomCode.trim().isEmpty();
    }

    public String getMaPhong() {
        return roomId;
    }
}
