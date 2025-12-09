package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.RoomRequest;
import com.ptit.schedule.dto.RoomResponse;
import com.ptit.schedule.dto.RoomStatusUpdateRequest;
import com.ptit.schedule.dto.RoomBulkStatusUpdateRequest;
import com.ptit.schedule.entity.Room;
import com.ptit.schedule.entity.RoomStatus;
import com.ptit.schedule.entity.RoomType;
import com.ptit.schedule.dto.RoomPickResult;
import com.ptit.schedule.exception.ResourceNotFoundException;
import com.ptit.schedule.repository.RoomRepository;
import com.ptit.schedule.service.RoomService;
import com.ptit.schedule.service.SubjectRoomMappingService;
import com.ptit.schedule.service.MajorBuildingPreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
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
                .orElseThrow(() -> new ResourceNotFoundException("phòng học", "mã", id));
        return convertToResponse(room);
    }

    @Override
    public RoomResponse createRoom(RoomRequest roomRequest) {
        // Kiểm tra phòng đã tồn tại chưa
        Optional<Room> existingRoom = roomRepository.findByNameAndBuilding(
                roomRequest.getName(), roomRequest.getBuilding());
        if (existingRoom.isPresent()) {
            throw new RuntimeException("Phòng " + roomRequest.getName() +
                    " trong tòa nhà " + roomRequest.getBuilding() + " đã tồn tại");
        }

        Room room = Room.builder()
                .name(roomRequest.getName())
                .capacity(roomRequest.getCapacity())
                .building(roomRequest.getBuilding())
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
                .orElseThrow(() -> new ResourceNotFoundException("phòng học", "mã", id));

        // Kiểm tra phòng khác có cùng số phòng và tòa nhà không
        Optional<Room> existingRoom = roomRepository.findByNameAndBuilding(
                roomRequest.getName(), roomRequest.getBuilding());
        if (existingRoom.isPresent() && !existingRoom.get().getId().equals(id)) {
            throw new RuntimeException("Phòng " + roomRequest.getName() +
                    " trong tòa nhà " + roomRequest.getBuilding() + " đã tồn tại");
        }

        room.setName(roomRequest.getName());
        room.setCapacity(roomRequest.getCapacity());
        room.setBuilding(roomRequest.getBuilding());
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
    public List<RoomResponse> bulkUpdateRoomStatus(RoomBulkStatusUpdateRequest request) {
        List<RoomResponse> updatedRooms = new ArrayList<>();
        List<Long> notFoundRoomIds = new ArrayList<>();

        for (Long roomId : request.getRoomIds()) {
            Optional<Room> roomOpt = roomRepository.findById(roomId);
            if (roomOpt.isPresent()) {
                Room room = roomOpt.get();
                room.setStatus(request.getStatus());
                Room updatedRoom = roomRepository.save(room);
                updatedRooms.add(convertToResponse(updatedRoom));
            } else {
                notFoundRoomIds.add(roomId);
            }
        }

        if (!notFoundRoomIds.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Không tìm thấy các phòng với ID: " + notFoundRoomIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining(", ")));
        }

        return updatedRooms;
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

    public RoomPickResult pickRoom(List<Room> rooms, Integer sisoPerClass, Set<Object> occupied,
            Integer thu, Integer kip, String subjectType, String studentYear,
            String heDacThu, List<String> weekSchedule, String nganh, String maMon) {

        // Validate required parameters
        if (rooms == null || rooms.isEmpty()) {
            throw new IllegalArgumentException("Danh sách phòng học không được null hoặc rỗng");
        }
        if (sisoPerClass == null || sisoPerClass <= 0) {
            throw new IllegalArgumentException("Sĩ số mỗi lớp phải lớn hơn 0, nhận được: " + sisoPerClass);
        }
        if (maMon == null || maMon.trim().isEmpty()) {
            throw new IllegalArgumentException("Mã môn học không được null hoặc rỗng");
        }
        if (occupied == null) {
            throw new IllegalArgumentException("Danh sách phòng đã sử dụng không được null");
        }

        // Auto-detect English subject if subjectType is not set
        String effectiveSubjectType = detectSubjectType(subjectType, maMon);

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
                    && isRoomSuitable(room, effectiveSubjectType, studentYear, heDacThu)) {
                return createRoomPickResult(room, 0, true);
            }
        }

        // 2. Get preferred buildings for major
        List<String> preferredBuildings = majorBuildingPreferenceService.getPreferredBuildingsForMajor(nganh);
        if (preferredBuildings == null || preferredBuildings.isEmpty()) {
            preferredBuildings = Arrays.asList("A2", "A1", "A3"); // Default fallback
        }
        final List<String> finalPreferredBuildings = preferredBuildings;

        // 3. Filter rooms by constraints
        List<Room> suitableRooms = new ArrayList<>();

        for (Room r : rooms) {
            String code = r.getName();
            if (code == null || code.trim().isEmpty()) {
                continue;
            }

            // Check if room is occupied
            if (thu != null && kip != null) {
                String roomUniqueCode = buildRoomUniqueCode(r);
                String occupationKey = buildOccupationKey(roomUniqueCode, thu, kip);
                String legacyOccupationKey = buildLegacyOccupationKey(code, thu, kip);

                boolean traditionalConflict = occupied.contains(occupationKey)
                        || occupied.contains(legacyOccupationKey);

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

            // Check room suitability (subject type, student year, special system)
            try {
                if (isRoomSuitable(r, effectiveSubjectType, studentYear, heDacThu)) {
                    suitableRooms.add(r);
                }
            } catch (Exception e) {
                log.warn("Lỗi khi kiểm tra phòng {} phù hợp: {}", r.getName(), e.getMessage());
            }
        }

        if (suitableRooms.isEmpty()) {
            log.warn("Không tìm thấy phòng phù hợp cho môn {} (subjectType: {}), thử logic dự phòng",
                    maMon, effectiveSubjectType);

            // Fallback: allow rooms that still satisfy suitability rules but ignore
            // building preference
            for (Room r : rooms) {
                String code = r.getName();
                if (code == null || code.trim().isEmpty()) {
                    continue;
                }

                // Check if room is occupied
                if (thu != null && kip != null) {
                    String roomUniqueCode = buildRoomUniqueCode(r);
                    String occupationKey = buildOccupationKey(roomUniqueCode, thu, kip);
                    String legacyOccupationKey = buildLegacyOccupationKey(code, thu, kip);
                    boolean traditionalConflict = occupied.contains(occupationKey)
                            || occupied.contains(legacyOccupationKey);
                    if (traditionalConflict) {
                        continue;
                    }
                }

                // Check capacity
                int cap = r.getCapacity();
                if (sisoPerClass != null && cap < sisoPerClass) {
                    continue;
                }

                // Still enforce suitability to avoid mixing special-purpose rooms
                try {
                    if (isRoomSuitable(r, effectiveSubjectType, studentYear, heDacThu)) {
                        suitableRooms.add(r);
                    }
                } catch (Exception e) {
                }
            }

            if (suitableRooms.isEmpty()) {
                String errorMsg = String.format(
                        "Không tìm thấy phòng phù hợp cho môn %s (subjectType: %s, Ngành: %s, Sĩ số: %d, Thứ: %d, Kíp: %d)",
                        maMon, effectiveSubjectType, nganh, sisoPerClass, thu, kip);
                log.error(errorMsg);

                // Additional debug for English subjects
                if ("english".equals(effectiveSubjectType)) {
                    log.error("Chi tiết: Môn tiếng anh {} cần phòng ENGLISH_CLASS với capacity >= {}",
                            maMon, sisoPerClass);
                    log.error("Danh sách phòng ENGLISH_CLASS trong hệ thống:");
                    rooms.stream()
                            .filter(r -> r.getType() != null
                                    && "english_class".equals(r.getType().name().toLowerCase()))
                            .forEach(r -> log.error("  - Phòng {}: capacity={}, day={}",
                                    r.getName(), r.getCapacity(), r.getBuilding()));
                }

                throw new RuntimeException(errorMsg);
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
        subjectRoomMappingService.setSubjectRoom(maMon, selectedRoom.getName());

        boolean isPreferredBuilding = selectedRoom.getBuilding().equals(finalPreferredBuildings.get(0));
        int distanceToPreferred = calculateDistance(selectedRoom.getBuilding(), finalPreferredBuildings.get(0));

        return createRoomPickResult(selectedRoom, distanceToPreferred, isPreferredBuilding);
    }

    // Helper methods
    private int calculateRoomScore(Room room, List<String> preferredBuildings,
            String existingRoom, Integer sisoPerClass) {
        int score = 0;

        // Highest priority: same room as before
        if (existingRoom != null && room.getName().equals(existingRoom)) {
            return -10000;
        }

        // Building priority and distance optimization
        String building = room.getBuilding();
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
                .filter(r -> r.getName().equals(roomCode))
                .findFirst()
                .orElse(null);
    }

    private boolean isRoomAvailable(Room room, Integer thu, Integer kip,
            Set<Object> occupied, List<String> weekSchedule,
            Integer sisoPerClass) {
        // Check occupation
        String roomUniqueCode = buildRoomUniqueCode(room);
        String key = buildOccupationKey(roomUniqueCode, thu, kip);
        String legacyKey = buildLegacyOccupationKey(room.getName(), thu, kip);
        if (occupied.contains(key) || occupied.contains(legacyKey))
            return false;

        // Check capacity
        if (room.getCapacity() < sisoPerClass) {
            return false;
        }

        return true;
    }

    private boolean isRoomSuitable(Room room, String subjectType, String studentYear, String heDacThu) {
        if (room == null) {
            throw new IllegalArgumentException("Phòng học không được null");
        }
        if (room.getType() == null) {
            throw new IllegalStateException("Loại phòng của " + room.getName() + " không được null");
        }

        String roomType = room.getType().name().toLowerCase();
        String roomNote = room.getNote() != null ? room.getNote().toLowerCase() : "";

        // 1. SPECIAL HANDLING FOR COMMON SUBJECTS (Môn chung)
        if (subjectType == null || subjectType.isEmpty() || "general".equals(subjectType)) {
            // Accept general rooms for common subjects
            if ("general".equals(roomType)) {
                return true;
            }
            // Avoid all special-purpose rooms for common subjects
            if (Arrays.asList("ngoc_truc", "english_class", "clc").contains(roomType)) {
                return false;
            }
            // Accept khoa_2024 rooms as fallback for common subjects
            if ("khoa_2024".equals(roomType)) {
                return true;
            }
            return true; // Accept other general-type rooms
        }

        // 2. SPECIAL HANDLING FOR ENGLISH SUBJECTS (Môn Tiếng Anh)
        if ("english".equals(subjectType)) {
            // English subjects MUST use ENGLISH_CLASS rooms only
            if ("english_class".equals(roomType)) {
                return true;
            }
            // Reject all other room types for English subjects
            return false;
        }

        // 3. SPECIAL SYSTEM ROOM ASSIGNMENT (Hệ đặc thù - CLC, CTTT, etc.)
        String normalizedHeDacThu = normalizeSpecialSystem(heDacThu);
        if (normalizedHeDacThu != null && !normalizedHeDacThu.isEmpty()) {
            if ("chinhquy".equals(normalizedHeDacThu)) {
                normalizedHeDacThu = null;
            }
        }

        if (normalizedHeDacThu != null && !normalizedHeDacThu.isEmpty()) {
            if ("clc".equals(normalizedHeDacThu)) {
                // CLC system
                if ("2024".equals(studentYear)) {
                    // CLC Khóa 2024: must have "lớp clc 2024" in note
                    if (roomNote.contains("lớp clc 2024")) {
                        return true;
                    }
                    return false;
                } else {
                    // CLC other years: room type = CLC but NOT 2024
                    if ("clc".equals(roomType) && !roomNote.contains("2024")) {
                        return true;
                    }
                    return false;
                }
            } else {
                // Other special systems (CTTT, etc.): NO room assignment
                return false;
            }
        }

        // 4. REGULAR SYSTEM (Hệ Chính quy)

        // Khóa 2022 → NGOC_TRUC rooms only
        if ("2022".equals(studentYear)) {
            if ("ngoc_truc".equals(roomType)) {
                return true;
            }
            return false;
        }

        // Khóa 2024 → KHOA_2024 or GENERAL rooms
        if ("2024".equals(studentYear)) {
            // Reject special-purpose rooms
            if (Arrays.asList("ngoc_truc", "english_class", "clc").contains(roomType)) {
                return false;
            }
            // Accept KHOA_2024 or GENERAL
            if (Arrays.asList("khoa_2024", "general").contains(roomType)) {
                return true;
            }
            return false;
        }

        // Other years (2023, 2021, etc.) → GENERAL rooms only
        // Reject all special-purpose rooms
        if (Arrays.asList("ngoc_truc", "english_class", "clc", "khoa_2024").contains(roomType)) {
            return false;
        }

        // Accept only GENERAL rooms
        if ("general".equals(roomType)) {
            return true;
        }

        return false;
    }

    private RoomPickResult createRoomPickResult(Room room, int distanceScore,
            boolean isPreferred) {
        return RoomPickResult.builder()
                .roomCode(room.getName())
                .roomId(buildRoomUniqueCode(room))
                .building(room.getBuilding())
                .distanceScore(distanceScore)
                .isPreferredBuilding(isPreferred)
                .build();
    }

    private String buildRoomUniqueCode(Room room) {
        return room.getName() + "-" + room.getBuilding();
    }

    private String buildOccupationKey(String roomUniqueCode, Integer thu, Integer kip) {
        return roomUniqueCode + "|" + thu + "|" + kip;
    }

    private String buildLegacyOccupationKey(String roomCode, Integer thu, Integer kip) {
        return roomCode + "|" + thu + "|" + kip;
    }

    /**
     * Auto-detect subject type based on subject code or existing subjectType
     * Automatically identifies English courses (Course 1, 2, 3, 3+)
     */
    private String detectSubjectType(String subjectType, String maMon) {
        // If subjectType is already set and not empty, use it
        if (subjectType != null && !subjectType.trim().isEmpty()
                && !"general".equals(subjectType.trim().toLowerCase())) {
            return subjectType.trim().toLowerCase();
        }

        // Auto-detect English subjects by specific course codes
        if (maMon != null) {
            String upperCode = maMon.trim().toUpperCase();

            // Specific English course codes from curriculum:
            // BAS1157 = Course 1, BAS1158 = Course 2, BAS1159 = Course 3, BAS1160 = Course
            // 3+
            if (upperCode.equals("BAS1157") || // Tiếng Anh Course 1
                    upperCode.equals("BAS1158") || // Tiếng Anh Course 2
                    upperCode.equals("BAS1159") || // Tiếng Anh Course 3
                    upperCode.equals("BAS1160")) { // Tiếng Anh Course 3+
                return "english";
            }

            // Also check generic English code patterns
            if (upperCode.startsWith("ENG") ||
                    upperCode.startsWith("ANH") ||
                    upperCode.matches(".*ENGLISH.*") ||
                    upperCode.matches(".*TIENG.*ANH.*")) {
                return "english";
            }
        }

        // Default to general for other subjects
        return subjectType != null ? subjectType.trim().toLowerCase() : "general";
    }

    private String normalizeSpecialSystem(String heDacThu) {
        if (heDacThu == null) {
            return null;
        }
        String trimmed = heDacThu.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "");
        return normalized;
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
                .name(room.getName())
                .capacity(room.getCapacity())
                .building(room.getBuilding())
                .type(room.getType())
                .typeDisplayName(room.getType().getDisplayName())
                .status(room.getStatus())
                .statusDisplayName(room.getStatus().getDisplayName())
                .note(room.getNote())
                .build();
    }
}
