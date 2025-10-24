package com.ptit.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomPickResult {
    private String roomCode;
    private String roomId;

    public boolean hasRoom() {
        return roomCode != null && !roomCode.trim().isEmpty();
    }

    public String getMaPhong() {
        return roomId;
    }
}
