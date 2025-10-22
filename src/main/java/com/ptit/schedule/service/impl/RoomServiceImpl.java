package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.RoomRequest;
import com.ptit.schedule.dto.RoomResponse;
import com.ptit.schedule.entity.Room;
import com.ptit.schedule.model.RoomData;
import com.ptit.schedule.repository.RoomRepository;
import com.ptit.schedule.service.RoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoomServiceImpl implements RoomService {
    
    private final RoomRepository roomRepository;
    
    @Override
    public RoomResponse createRoom(RoomRequest request) {
        log.info("Creating room: {}", request.getRoomNumber());
        
        // Kiểm tra room number đã tồn tại chưa
        if (roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new RuntimeException("Room number already exists: " + request.getRoomNumber());
        }
        
        // Kiểm tra room code đã tồn tại chưa
        if (roomRepository.existsByRoomCode(request.getRoomCode())) {
            throw new RuntimeException("Room code already exists: " + request.getRoomCode());
        }
        
        Room room = Room.builder()
                .id(UUID.randomUUID().toString())
                .roomNumber(request.getRoomNumber())
                .capacity(request.getCapacity())
                .building(request.getBuilding())
                .roomCode(request.getRoomCode())
                .roomType(request.getRoomType())
                .note(request.getNote())
                .build();
        
        Room savedRoom = roomRepository.save(room);
        log.info("Created room: {} with ID: {}", savedRoom.getRoomNumber(), savedRoom.getId());
        
        return RoomResponse.fromEntity(savedRoom);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        log.info("Getting all rooms");
        return roomRepository.findAll().stream()
                .map(RoomResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(String id) {
        log.info("Getting room by ID: {}", id);
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with ID: " + id));
        return RoomResponse.fromEntity(room);
    }
    
    @Override
    public RoomResponse updateRoom(String id, RoomRequest request) {
        log.info("Updating room: {}", id);
        
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with ID: " + id));
        
        // Kiểm tra room number đã tồn tại chưa (trừ room hiện tại)
        if (!room.getRoomNumber().equals(request.getRoomNumber()) && 
            roomRepository.existsByRoomNumber(request.getRoomNumber())) {
            throw new RuntimeException("Room number already exists: " + request.getRoomNumber());
        }
        
        // Kiểm tra room code đã tồn tại chưa (trừ room hiện tại)
        if (!room.getRoomCode().equals(request.getRoomCode()) && 
            roomRepository.existsByRoomCode(request.getRoomCode())) {
            throw new RuntimeException("Room code already exists: " + request.getRoomCode());
        }
        
        room.setRoomNumber(request.getRoomNumber());
        room.setCapacity(request.getCapacity());
        room.setBuilding(request.getBuilding());
        room.setRoomCode(request.getRoomCode());
        room.setRoomType(request.getRoomType());
        room.setNote(request.getNote());
        
        Room updatedRoom = roomRepository.save(room);
        log.info("Updated room: {} with ID: {}", updatedRoom.getRoomNumber(), updatedRoom.getId());
        
        return RoomResponse.fromEntity(updatedRoom);
    }
    
    @Override
    public void deleteRoom(String id) {
        log.info("Deleting room: {}", id);
        
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Room not found with ID: " + id));
        
        roomRepository.delete(room);
        log.info("Deleted room: {} with ID: {}", room.getRoomNumber(), room.getId());
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByRoomNumber(String roomNumber) {
        return roomRepository.existsByRoomNumber(roomNumber);
    }
    
    @Override
    @Transactional(readOnly = true)
    public boolean existsByRoomCode(String roomCode) {
        return roomRepository.existsByRoomCode(roomCode);
    }
    
    @Override
    public RoomPickResult pickRoom(List<RoomData> rooms, Integer sisoPerClass, Set<Object> occupied,
                                   Integer thu, Integer kip, String subjectType, String studentYear,
                                   String heDacThu, List<String> weekSchedule) {
        
        // Skip room assignment for rows with tiet_bd = 12 (no room needed)
        // This is handled by checking if thu and kip are both null or invalid
        if (thu == null || kip == null) {
            log.info("Skipping room assignment: thu={}, kip={} (no room needed for this schedule)", thu, kip);
            return new RoomPickResult(null, null);
        }
        
        // Filter rooms by constraints
        List<RoomData> suitableRooms = new ArrayList<>();
        
        for (RoomData r : rooms) {
            String code = r.getPhong();
            if (code == null || code.trim().isEmpty()) {
                continue;
            }
            
            // Check if room is occupied (only if thu and kip are provided)
            if (thu != null && kip != null) {
                // Check traditional conflict: (room_code, thu, kip)
                String traditionalKey = code + "|" + thu + "|" + kip;
                boolean traditionalConflict = occupied.contains(traditionalKey);
                
                // Check week schedule conflict if provided
                boolean weekConflict = false;
                if (weekSchedule != null && !weekSchedule.isEmpty()) {
                    // Find existing occupations for this room/time that might overlap
                    for (Object occupiedKey : occupied) {
                        if (occupiedKey instanceof String) {
                            String[] parts = ((String) occupiedKey).split("\\|");
                            if (parts.length >= 4) {
                                String occCode = parts[0];
                                String occThu = parts[1];
                                String occKip = parts[2];
                                String occWeeks = parts[3];
                                
                                if (occCode.equals(code) && occThu.equals(String.valueOf(thu)) && 
                                    occKip.equals(String.valueOf(kip))) {
                                    // Check week overlap
                                    List<String> occWeekList = Arrays.asList(occWeeks.split(","));
                                    weekConflict = weekSchedule.stream().anyMatch(occWeekList::contains);
                                    if (weekConflict) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (traditionalConflict || weekConflict) {
                    continue;
                }
            }
            
            // Check capacity
            int cap = r.getCapacityValue();
            if (sisoPerClass != null && cap < sisoPerClass) {
                continue;
            }
            
            // Check room type constraints
            String roomType = r.getTypeValue();
            String roomNote = r.getNoteValue();
            
            // Apply constraints based on subject type and student year
            boolean isSuitable = true;
            
            // Special system room assignment rules (Hệ đặc thù)
            if (heDacThu != null && !heDacThu.trim().isEmpty()) {
                if ("CLC".equals(heDacThu)) {
                    // Handle CLC room assignment based on student year
                    if ("2024".equals(studentYear)) {
                        // CLC + Khóa 2024: Ưu tiên phòng có "Lớp CLC 2024" trong note
                        if (!roomNote.contains("lớp clc 2024")) {
                            isSuitable = false;
                        }
                    } else {
                        // CLC + Khóa khác: phòng CLC nhưng KHÔNG được có "2024" trong note
                        if ((!roomNote.contains("clc") && !"clc".equals(roomType)) || roomNote.contains("2024")) {
                            isSuitable = false;
                        }
                    }
                } else {
                    // Other special systems (CTTT, etc.): NO room assignment
                    isSuitable = false;
                }
            } 
            // Hệ thường (không phải he_dac_thu)
            else {
                // Khóa 2022 → phòng NT
                if ("2022".equals(studentYear)) {
                    // Must be phòng NT: type="nt" và note chứa "NT"
                    if (!"nt".equals(roomType) || !roomNote.contains("nt")) {
                        isSuitable = false;
                    }
                }
                // Môn "Tiếng Anh" → phòng Tiếng Anh
                else if ("english".equals(subjectType)) {
                    // Must be phòng Tiếng Anh: type="english" và note chứa "Phòng học TA"
                    if (!"english".equals(roomType) || !roomNote.contains("phòng học ta")) {
                        isSuitable = false;
                    }
                }
                // Còn lại (khóa khác + môn thường) → phòng theo khóa
                else {
                    // Không được dùng phòng NT, Tiếng Anh, CLC
                    if (Arrays.asList("nt", "english", "clc").contains(roomType) || 
                        roomNote.contains("nt") || roomNote.contains("phòng học ta") || roomNote.contains("lớp clc")) {
                        isSuitable = false;
                    }
                    
                    // Ưu tiên phòng theo khóa (nếu không phù hợp với khóa thì reject)
                    if ("2024".equals(studentYear)) {
                        // Khóa 2024 → phòng year2024 hoặc general
                        if (!Arrays.asList("year2024", "general").contains(roomType)) {
                            isSuitable = false;
                        }
                    } else {
                        // Khóa khác → chỉ phòng general (không year2024)
                        if (!"general".equals(roomType)) {
                            isSuitable = false;
                        }
                    }
                }
            }
            
            if (isSuitable) {
                suitableRooms.add(r);
            }
        }
        
        // Sort by capacity (prefer smaller rooms first to save space)
        suitableRooms.sort(Comparator.comparingInt(RoomData::getCapacityValue));
        
        if (!suitableRooms.isEmpty()) {
            RoomData room = suitableRooms.get(0);
            return new RoomPickResult(room.getPhong(), room.getMa_phong());
        }
        
        // Fallback logic for CLC Khóa 2024: if no suitable room found, try any CLC room
        if ("CLC".equals(heDacThu) && "2024".equals(studentYear)) {
            List<RoomData> clcFallbackRooms = new ArrayList<>();
            
            for (RoomData r : rooms) {
                String code = r.getPhong();
                if (code == null || code.trim().isEmpty()) {
                    continue;
                }
                
                // Check if room is occupied (only if thu and kip are provided)
                if (thu != null && kip != null) {
                    String traditionalKey = code + "|" + thu + "|" + kip;
                    boolean traditionalConflict = occupied.contains(traditionalKey);
                    
                    // Check week schedule conflict if provided
                    boolean weekConflict = false;
                    if (weekSchedule != null && !weekSchedule.isEmpty()) {
                        for (Object occupiedKey : occupied) {
                            if (occupiedKey instanceof String) {
                                String[] parts = ((String) occupiedKey).split("\\|");
                                if (parts.length >= 4) {
                                    String occCode = parts[0];
                                    String occThu = parts[1];
                                    String occKip = parts[2];
                                    String occWeeks = parts[3];
                                    
                                    if (occCode.equals(code) && occThu.equals(String.valueOf(thu)) && 
                                        occKip.equals(String.valueOf(kip))) {
                                        List<String> occWeekList = Arrays.asList(occWeeks.split(","));
                                        weekConflict = weekSchedule.stream().anyMatch(occWeekList::contains);
                                        if (weekConflict) {
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    if (traditionalConflict || weekConflict) {
                        continue;
                    }
                }
                
                // Check capacity
                int cap = r.getCapacityValue();
                if (sisoPerClass != null && cap < sisoPerClass) {
                    continue;
                }
                
                // Check if it's any CLC room (ignore year constraint)
                String roomType = r.getTypeValue();
                String roomNote = r.getNoteValue();
                
                if (roomNote.contains("clc") || "clc".equals(roomType)) {
                    clcFallbackRooms.add(r);
                }
            }
            
            // Sort by capacity and return first available CLC room
            clcFallbackRooms.sort(Comparator.comparingInt(RoomData::getCapacityValue));
            if (!clcFallbackRooms.isEmpty()) {
                RoomData room = clcFallbackRooms.get(0);
                return new RoomPickResult(room.getPhong(), room.getMa_phong());
            }
        }
        
        return new RoomPickResult(null, null);
    }
}
