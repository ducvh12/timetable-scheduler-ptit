package com.ptit.schedule.service;

import com.ptit.schedule.dto.RoomRequest;
import com.ptit.schedule.dto.RoomResponse;
import com.ptit.schedule.model.RoomData;

import java.util.List;
import java.util.Set;

public interface RoomService {
    
    RoomResponse createRoom(RoomRequest request);
    
    List<RoomResponse> getAllRooms();
    
    RoomResponse getRoomById(String id);
    
    RoomResponse updateRoom(String id, RoomRequest request);
    
    void deleteRoom(String id);
    
    boolean existsByRoomNumber(String roomNumber);
    
    boolean existsByRoomCode(String roomCode);
    
    /**
     * Pick a room for TKB assignment based on constraints
     * Implements exact Python _pick_room logic
     * 
     * @param rooms List of available rooms
     * @param sisoPerClass Required capacity per class
     * @param occupied Set of occupied room keys
     * @param thu Day of week
     * @param kip Session
     * @param subjectType Type of subject (e.g., 'english')
     * @param studentYear Student year (e.g., '2024')
     * @param heDacThu Special system type (e.g., 'CLC')
     * @param weekSchedule Week schedule for conflict detection
     * @return RoomPickResult with room code and ma_phong
     */
    RoomPickResult pickRoom(List<RoomData> rooms, Integer sisoPerClass, Set<Object> occupied,
                           Integer thu, Integer kip, String subjectType, String studentYear,
                           String heDacThu, List<String> weekSchedule);
    
    /**
     * Result class for room picking
     */
    class RoomPickResult {
        private final String roomCode;
        private final String maPhong;
        
        public RoomPickResult(String roomCode, String maPhong) {
            this.roomCode = roomCode;
            this.maPhong = maPhong;
        }
        
        public String getRoomCode() {
            return roomCode;
        }
        
        public String getMaPhong() {
            return maPhong;
        }
        
        public boolean hasRoom() {
            return roomCode != null && maPhong != null;
        }
    }
}
