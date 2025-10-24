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
import com.ptit.schedule.service.SubjectRoomMappingService;
import com.ptit.schedule.service.MajorBuildingPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final SubjectRoomMappingService subjectRoomMappingService;
    private final MajorBuildingPreferenceService majorBuildingPreferenceService;

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
            String heDacThu, List<String> weekSchedule, String nganh, String maMon) {

        // Skip room assignment for rows with tiet_bd = 12 (no room needed)
        if (thu == null || kip == null) {
            return RoomPickResult.builder()
                    .roomCode(null)
                    .roomId(null)
                    .building(null)
                    .distanceScore(null)
                    .isPreferredBuilding(false)
                    .build();
        }

        // 1. Check if subject already has assigned room (highest priority)
        String existingRoom = subjectRoomMappingService.getSubjectRoom(maMon);
        if (existingRoom != null) {
            Room room = findRoomByCode(rooms, existingRoom);
            if (room != null && isRoomAvailable(room, thu, kip, occupied, weekSchedule, sisoPerClass)
                    && isRoomSuitable(room, subjectType, studentYear, heDacThu)) {
                return createRoomPickResult(room, 0, true);
            }
        }

        // 2. Get preferred buildings for major
        List<String> preferredBuildings = majorBuildingPreferenceService.getPreferredBuildingsForMajor(nganh);
        if (preferredBuildings.isEmpty()) {
            preferredBuildings = Arrays.asList("A2", "A1", "A3"); // Default fallback
        }
        final List<String> finalPreferredBuildings = preferredBuildings;

        log.info("Picking room for subject: {}, major: {}, preferred buildings: {}",
                maMon, nganh, finalPreferredBuildings);

        // 3. Filter rooms by constraints
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
                log.debug("Room {} rejected: insufficient capacity ({} < {})",
                        r.getPhong(), cap, sisoPerClass);
                continue;
            }

            // Check room suitability (subject type, student year, special system)
            if (isRoomSuitable(r, subjectType, studentYear, heDacThu)) {
                suitableRooms.add(r);
                log.debug("Room {} passed suitability check", r.getPhong());
            } else {
                log.debug(
                        "Room {} failed suitability check - type: {}, note: {}, subjectType: {}, studentYear: {}, heDacThu: {}",
                        r.getPhong(), r.getType(), r.getNote(), subjectType, studentYear, heDacThu);
            }
        }

        log.info("Found {} suitable rooms out of {} total rooms", suitableRooms.size(), rooms.size());

        // Verify all suitable rooms have sufficient capacity
        for (Room room : suitableRooms) {
            if (room.getCapacity() < sisoPerClass) {
                log.error("CRITICAL: Room {} in suitable list has insufficient capacity ({} < {})",
                        room.getPhong(), room.getCapacity(), sisoPerClass);
            }
        }

        if (suitableRooms.isEmpty()) {
            log.warn(
                    "No suitable rooms found for subject: {}, major: {}, subjectType: {}, studentYear: {}, heDacThu: {}",
                    maMon, nganh, subjectType, studentYear, heDacThu);

            // Fallback: try to find any available room (relax constraints)
            log.info("Trying fallback logic with relaxed constraints...");
            for (Room r : rooms) {
                String code = r.getPhong();
                if (code == null || code.trim().isEmpty()) {
                    continue;
                }

                // Check if room is occupied
                if (thu != null && kip != null) {
                    String traditionalKey = code + "|" + thu + "|" + kip;
                    boolean traditionalConflict = occupied.contains(traditionalKey);
                    if (traditionalConflict) {
                        continue;
                    }
                }

                // Check capacity
                int cap = r.getCapacity();
                if (sisoPerClass != null && cap < sisoPerClass) {
                    log.debug("Fallback: Room {} rejected: insufficient capacity ({} < {})",
                            r.getPhong(), cap, sisoPerClass);
                    continue;
                }

                // Accept any room that passes basic checks
                suitableRooms.add(r);
                log.info("Fallback: Added room {} to suitable list", r.getPhong());
            }

            if (suitableRooms.isEmpty()) {
                log.error("No rooms available even with fallback logic");
                return RoomPickResult.builder()
                        .roomCode(null)
                        .roomId(null)
                        .building(null)
                        .distanceScore(null)
                        .isPreferredBuilding(false)
                        .build();
            }
        }

        // 4. Sort by priority: same room > preferred buildings > capacity fit
        suitableRooms.sort((r1, r2) -> {
            int score1 = calculateRoomScore(r1, finalPreferredBuildings, existingRoom, sisoPerClass);
            int score2 = calculateRoomScore(r2, finalPreferredBuildings, existingRoom, sisoPerClass);
            return Integer.compare(score1, score2);
        });

        // 5. Select best room and save mapping
        Room selectedRoom = suitableRooms.get(0);
        subjectRoomMappingService.setSubjectRoom(maMon, selectedRoom.getPhong());

        boolean isPreferredBuilding = selectedRoom.getDay().equals(finalPreferredBuildings.get(0));
        int distanceToPreferred = calculateDistance(selectedRoom.getDay(), finalPreferredBuildings.get(0));

        log.info("Selected room {} in building {} for subject {} (major: {}, preferred: {}, distance: {})",
                selectedRoom.getPhong(), selectedRoom.getDay(), maMon, nganh,
                isPreferredBuilding ? "YES" : "NO", distanceToPreferred);

        return createRoomPickResult(selectedRoom, distanceToPreferred, isPreferredBuilding);
    }

    // Helper methods
    private int calculateRoomScore(Room room, List<String> preferredBuildings,
            String existingRoom, Integer sisoPerClass) {
        int score = 0;

        // Highest priority: same room as before
        if (existingRoom != null && room.getPhong().equals(existingRoom)) {
            return -10000;
        }

        // Building priority and distance optimization
        String building = room.getDay();
        int buildingIndex = preferredBuildings.indexOf(building);
        if (buildingIndex >= 0) {
            // Preferred building: lower index = higher priority
            score += buildingIndex * 100; // Priority 1=0, 2=100, 3=200
        } else {
            // Not in preferred list: calculate distance to closest preferred building
            int minDistance = Integer.MAX_VALUE;
            for (String preferredBuilding : preferredBuildings) {
                int distance = calculateDistance(building, preferredBuilding);
                minDistance = Math.min(minDistance, distance);
            }
            score += 1000 + (minDistance * 50); // Base penalty + distance penalty
        }

        // Capacity fit (prefer just enough capacity)
        score += Math.abs(room.getCapacity() - sisoPerClass);

        return score;
    }

    private Room findRoomByCode(List<Room> rooms, String roomCode) {
        return rooms.stream()
                .filter(r -> r.getPhong().equals(roomCode))
                .findFirst()
                .orElse(null);
    }

    private boolean isRoomAvailable(Room room, Integer thu, Integer kip,
            Set<Object> occupied, List<String> weekSchedule,
            Integer sisoPerClass) {
        // Check occupation
        String key = room.getPhong() + "|" + thu + "|" + kip;
        if (occupied.contains(key))
            return false;

        // Check capacity
        if (room.getCapacity() < sisoPerClass) {
            log.debug("Room {} rejected in isRoomAvailable: insufficient capacity ({} < {})",
                    room.getPhong(), room.getCapacity(), sisoPerClass);
            return false;
        }

        return true;
    }

    private boolean isRoomSuitable(Room room, String subjectType, String studentYear, String heDacThu) {
        String roomType = room.getType().name().toLowerCase();
        String roomNote = room.getNote() != null ? room.getNote().toLowerCase() : "";

        log.debug("Checking room suitability: phong={}, type={}, note={}, subjectType={}, studentYear={}, heDacThu={}",
                room.getPhong(), roomType, roomNote, subjectType, studentYear, heDacThu);

        // Special system room assignment rules (Hệ đặc thù)
        if (heDacThu != null && !heDacThu.trim().isEmpty()) {
            if ("CLC".equals(heDacThu)) {
                // Handle CLC room assignment based on student year
                if ("2024".equals(studentYear)) {
                    // CLC + Khóa 2024: Ưu tiên phòng có "Lớp CLC 2024" trong note
                    if (!roomNote.contains("lớp clc 2024")) {
                        return false;
                    }
                } else {
                    // CLC + Khóa khác: phòng CLC nhưng KHÔNG được có "2024" trong note
                    if ((!roomNote.contains("clc") && !"clc".equals(roomType)) || roomNote.contains("2024")) {
                        return false;
                    }
                }
            } else {
                // Other special systems (CTTT, etc.): NO room assignment
                return false;
            }
        }
        // Hệ thường (không phải he_dac_thu)
        else {
            // Khóa 2022 → phòng NT
            if ("2022".equals(studentYear)) {
                // Must be phòng NT: type="ngoc_truc"
                if (!"ngoc_truc".equals(roomType)) {
                    return false;
                }
            }
            // Môn "Tiếng Anh" → phòng Tiếng Anh
            else if ("english".equals(subjectType)) {
                // Must be phòng Tiếng Anh: type="english_class"
                if (!"english_class".equals(roomType)) {
                    return false;
                }
            }
            // Còn lại (khóa khác + môn thường) → phòng theo khóa
            else {
                // Không được dùng phòng NT, Tiếng Anh, CLC
                if (Arrays.asList("ngoc_truc", "english_class", "clc").contains(roomType) ||
                        roomNote.contains("nt") || roomNote.contains("phòng học ta")
                        || roomNote.contains("lớp clc")) {
                    return false;
                }

                // Ưu tiên phòng theo khóa (nếu không phù hợp với khóa thì reject)
                if ("2024".equals(studentYear)) {
                    // Khóa 2024 → phòng year2024 hoặc general
                    if (!Arrays.asList("khoa_2024", "general").contains(roomType)) {
                        return false;
                    }
                } else {
                    // Khóa khác → chỉ phòng general (không year2024)
                    if (!"general".equals(roomType)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private RoomPickResult createRoomPickResult(Room room, int distanceScore,
            boolean isPreferred) {
        return RoomPickResult.builder()
                .roomCode(room.getPhong())
                .roomId(room.getPhong() + "-" + room.getDay())
                .building(room.getDay())
                .distanceScore(distanceScore)
                .isPreferredBuilding(isPreferred)
                .build();
    }

    private int calculateDistance(String building1, String building2) {
        if (building1.equals(building2))
            return 0;

        Map<String, Integer> buildingDistance = Map.of(
                "A1", 0, "A2", 1, "A3", 2, "NT", 3);

        return Math.abs(buildingDistance.getOrDefault(building1, 0) -
                buildingDistance.getOrDefault(building2, 0));
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
