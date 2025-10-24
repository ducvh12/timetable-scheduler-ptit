package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.RoomRequest;
import com.ptit.schedule.dto.RoomResponse;
import com.ptit.schedule.dto.RoomStatusUpdateRequest;
import com.ptit.schedule.entity.Room;
import com.ptit.schedule.entity.RoomStatus;
import com.ptit.schedule.entity.RoomType;
import com.ptit.schedule.dto.RoomPickResult;
import com.ptit.schedule.repository.RoomRepository;
import com.ptit.schedule.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAllRooms() {
        return roomRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(Long id) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với ID: " + id));
        return convertToResponse(room);
    }

    @Override
    public RoomResponse createRoom(RoomRequest roomRequest) {
        // Kiểm tra phòng đã tồn tại chưa
        Optional<Room> existingRoom = roomRepository.findByPhongAndDay(
                roomRequest.getPhong(), roomRequest.getDay());
        if (existingRoom.isPresent()) {
            throw new RuntimeException("Phòng " + roomRequest.getPhong() +
                    " trong tòa nhà " + roomRequest.getDay() + " đã tồn tại");
        }

        Room room = Room.builder()
                .phong(roomRequest.getPhong())
                .capacity(roomRequest.getCapacity())
                .day(roomRequest.getDay())
                .type(roomRequest.getType())
                .status(RoomStatus.AVAILABLE) // Mặc định là trống
                .note(roomRequest.getNote())
                .build();

        Room savedRoom = roomRepository.save(room);
        return convertToResponse(savedRoom);
    }

    @Override
    public RoomResponse updateRoom(Long id, RoomRequest roomRequest) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với ID: " + id));

        // Kiểm tra phòng khác có cùng số phòng và tòa nhà không
        Optional<Room> existingRoom = roomRepository.findByPhongAndDay(
                roomRequest.getPhong(), roomRequest.getDay());
        if (existingRoom.isPresent() && !existingRoom.get().getId().equals(id)) {
            throw new RuntimeException("Phòng " + roomRequest.getPhong() +
                    " trong tòa nhà " + roomRequest.getDay() + " đã tồn tại");
        }

        room.setPhong(roomRequest.getPhong());
        room.setCapacity(roomRequest.getCapacity());
        room.setDay(roomRequest.getDay());
        room.setType(roomRequest.getType());
        room.setNote(roomRequest.getNote());

        Room updatedRoom = roomRepository.save(room);
        return convertToResponse(updatedRoom);
    }

    @Override
    public void deleteRoom(Long id) {
        if (!roomRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy phòng với ID: " + id);
        }
        roomRepository.deleteById(id);
    }

    @Override
    public RoomResponse updateRoomStatus(Long id, RoomStatusUpdateRequest statusRequest) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng với ID: " + id));

        room.setStatus(statusRequest.getStatus());
        Room updatedRoom = roomRepository.save(room);
        return convertToResponse(updatedRoom);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByBuilding(String day) {
        return roomRepository.findByDay(day).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByStatus(RoomStatus status) {
        return roomRepository.findByStatus(status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByType(RoomType type) {
        return roomRepository.findByType(type).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getAvailableRoomsWithCapacity(Integer requiredCapacity) {
        return roomRepository.findAvailableRoomsWithCapacity(requiredCapacity).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByBuildingAndStatus(String day, RoomStatus status) {
        return roomRepository.findByDayAndStatus(day, status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByTypeAndStatus(RoomType type, RoomStatus status) {
        return roomRepository.findByTypeAndStatus(type, status).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // Method để pick room cho TimetableSchedulingService (sử dụng Room Entity trực
    // tiếp)
    public RoomPickResult pickRoom(List<Room> rooms, Integer sisoPerClass, Set<Object> occupied,
            Integer thu, Integer kip, String subjectType, String studentYear,
            String heDacThu, List<String> weekSchedule) {

        // Skip room assignment for rows with tiet_bd = 12 (no room needed)
        if (thu == null || kip == null) {
            return new RoomPickResult(null, null);
        }

        // Filter rooms by constraints
        List<Room> suitableRooms = new ArrayList<>();

        for (Room r : rooms) {
            String code = r.getPhong();
            if (code == null || code.trim().isEmpty()) {
                continue;
            }

            // Check if room is occupied
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
            int cap = r.getCapacity();
            if (sisoPerClass != null && cap < sisoPerClass) {
                continue;
            }

            // Check room type constraints
            String roomType = r.getType().name().toLowerCase();
            String roomNote = r.getNote() != null ? r.getNote().toLowerCase() : "";

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
                            roomNote.contains("nt") || roomNote.contains("phòng học ta")
                            || roomNote.contains("lớp clc")) {
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
        suitableRooms.sort(Comparator.comparingInt(Room::getCapacity));

        if (!suitableRooms.isEmpty()) {
            Room room = suitableRooms.get(0);
            return new RoomPickResult(room.getPhong(), room.getPhong() + "-" + room.getDay());
        }

        // Fallback logic for CLC Khóa 2024: if no suitable room found, try any CLC room
        if ("CLC".equals(heDacThu) && "2024".equals(studentYear)) {
            List<Room> clcFallbackRooms = new ArrayList<>();

            for (Room r : rooms) {
                String code = r.getPhong();
                if (code == null || code.trim().isEmpty()) {
                    continue;
                }

                // Check if room is occupied
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
                int cap = r.getCapacity();
                if (sisoPerClass != null && cap < sisoPerClass) {
                    continue;
                }

                // Check if it's any CLC room (ignore year constraint)
                String roomType = r.getType().name().toLowerCase();
                String roomNote = r.getNote() != null ? r.getNote().toLowerCase() : "";

                if (roomNote.contains("clc") || "clc".equals(roomType)) {
                    clcFallbackRooms.add(r);
                }
            }

            // Sort by capacity and return first available CLC room
            clcFallbackRooms.sort(Comparator.comparingInt(Room::getCapacity));
            if (!clcFallbackRooms.isEmpty()) {
                Room room = clcFallbackRooms.get(0);
                return new RoomPickResult(room.getPhong(), room.getPhong() + "-" + room.getDay());
            }
        }

        return new RoomPickResult(null, null);
    }

    private RoomResponse convertToResponse(Room room) {
        return RoomResponse.builder()
                .id(room.getId())
                .phong(room.getPhong())
                .capacity(room.getCapacity())
                .day(room.getDay())
                .type(room.getType())
                .typeDisplayName(room.getType().getDisplayName())
                .status(room.getStatus())
                .statusDisplayName(room.getStatus().getDisplayName())
                .note(room.getNote())
                .build();
    }
}
