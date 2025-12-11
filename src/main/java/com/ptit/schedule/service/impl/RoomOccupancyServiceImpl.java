package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.BulkCreateRoomOccupancyRequest;
import com.ptit.schedule.dto.RoomOccupancyResponse;
import com.ptit.schedule.dto.RoomWithOccupancyStatus;
import com.ptit.schedule.entity.OccupancyStatus;
import com.ptit.schedule.entity.Room;
import com.ptit.schedule.entity.RoomOccupancy;
import com.ptit.schedule.entity.Semester;
import com.ptit.schedule.exception.ResourceNotFoundException;
import com.ptit.schedule.repository.RoomOccupancyRepository;
import com.ptit.schedule.repository.RoomRepository;
import com.ptit.schedule.repository.SemesterRepository;
import com.ptit.schedule.service.RoomOccupancyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomOccupancyServiceImpl implements RoomOccupancyService {

        private final RoomOccupancyRepository roomOccupancyRepository;
        private final RoomRepository roomRepository;
        private final SemesterRepository semesterRepository;

        // Map để chuyển đổi dayOfWeek (2-7) thành tên
        private static final Map<Integer, String> DAY_NAMES = Map.of(
                        2, "Thứ 2",
                        3, "Thứ 3",
                        4, "Thứ 4",
                        5, "Thứ 5",
                        6, "Thứ 6",
                        7, "Thứ 7");

        // Map để chuyển đổi period (1-6) thành tên
        private static final Map<Integer, String> PERIOD_NAMES = Map.of(
                        1, "Kíp 1",
                        2, "Kíp 2",
                        3, "Kíp 3",
                        4, "Kíp 4",
                        5, "Kíp 5",
                        6, "Kíp 6");

        @Override
        public List<RoomOccupancyResponse> getOccupanciesByRoomAndSemester(Long roomId, Long semesterId) {
                log.info("Fetching occupancies for roomId={}, semesterId={}", roomId, semesterId);

                List<RoomOccupancy> occupancies = roomOccupancyRepository.findByRoomIdAndSemesterId(roomId, semesterId);

                return occupancies.stream()
                                .map(this::toResponse)
                                .sorted(Comparator.comparing(RoomOccupancyResponse::getDayOfWeek)
                                                .thenComparing(RoomOccupancyResponse::getPeriod))
                                .collect(Collectors.toList());
        }

        @Override
        public List<RoomOccupancyResponse> getOccupanciesByRoom(Long roomId) {
                log.info("Fetching all occupancies for roomId={}", roomId);

                List<RoomOccupancy> occupancies = roomOccupancyRepository.findByRoomId(roomId);

                return occupancies.stream()
                                .map(this::toResponse)
                                .sorted(Comparator.comparing(RoomOccupancyResponse::getSemesterId)
                                                .thenComparing(RoomOccupancyResponse::getDayOfWeek)
                                                .thenComparing(RoomOccupancyResponse::getPeriod))
                                .collect(Collectors.toList());
        }

        @Override
        public Page<RoomOccupancyResponse> getOccupanciesByRoom(Long roomId, Pageable pageable) {
                log.info("Fetching occupancies for roomId={} with pagination", roomId);

                List<RoomOccupancy> occupancies = roomOccupancyRepository.findByRoomId(roomId);

                List<RoomOccupancyResponse> responses = occupancies.stream()
                                .map(this::toResponse)
                                .sorted(Comparator.comparing(RoomOccupancyResponse::getSemesterId)
                                                .thenComparing(RoomOccupancyResponse::getDayOfWeek)
                                                .thenComparing(RoomOccupancyResponse::getPeriod))
                                .collect(Collectors.toList());

                // Manual pagination
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), responses.size());

                List<RoomOccupancyResponse> pageContent = start >= responses.size() ? List.of()
                                : responses.subList(start, end);

                return new PageImpl<>(pageContent, pageable, responses.size());
        }

        @Override
        public Page<RoomOccupancyResponse> getOccupanciesByRoomWithFilters(
                        Long roomId,
                        Long semesterId,
                        Integer dayOfWeek,
                        Integer period,
                        String search,
                        Pageable pageable) {
                log.info("Fetching occupancies for roomId={} with filters: semesterId={}, dayOfWeek={}, period={}, search={}",
                                roomId, semesterId, dayOfWeek, period, search);

                List<RoomOccupancy> occupancies = roomOccupancyRepository.findByRoomId(roomId);

                // Apply filters
                List<RoomOccupancyResponse> responses = occupancies.stream()
                                .map(this::toResponse)
                                .filter(response -> {
                                        // Filter by semesterId
                                        if (semesterId != null && !response.getSemesterId().equals(semesterId)) {
                                                return false;
                                        }

                                        // Filter by dayOfWeek
                                        if (dayOfWeek != null && !response.getDayOfWeek().equals(dayOfWeek)) {
                                                return false;
                                        }

                                        // Filter by period
                                        if (period != null && !response.getPeriod().equals(period)) {
                                                return false;
                                        }

                                        // Search in semesterName, academicYear, note
                                        if (search != null && !search.trim().isEmpty()) {
                                                String searchLower = search.toLowerCase().trim();
                                                boolean matchSemesterName = response.getSemesterName() != null
                                                                && response.getSemesterName().toLowerCase()
                                                                                .contains(searchLower);
                                                boolean matchAcademicYear = response.getAcademicYear() != null
                                                                && response.getAcademicYear().toLowerCase()
                                                                                .contains(searchLower);
                                                boolean matchNote = response.getNote() != null
                                                                && response.getNote().toLowerCase()
                                                                                .contains(searchLower);

                                                return matchSemesterName || matchAcademicYear || matchNote;
                                        }

                                        return true;
                                })
                                .sorted(Comparator.comparing(RoomOccupancyResponse::getSemesterId)
                                                .thenComparing(RoomOccupancyResponse::getDayOfWeek)
                                                .thenComparing(RoomOccupancyResponse::getPeriod))
                                .collect(Collectors.toList());

                // Manual pagination
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), responses.size());

                List<RoomOccupancyResponse> pageContent = start >= responses.size() ? List.of()
                                : responses.subList(start, end);

                return new PageImpl<>(pageContent, pageable, responses.size());
        }

        @Override
        public List<RoomOccupancyResponse> getOccupanciesBySemester(Long semesterId) {
                log.info("Fetching all occupancies for semesterId={}", semesterId);

                List<RoomOccupancy> occupancies = roomOccupancyRepository.findBySemesterId(semesterId);

                return occupancies.stream()
                                .map(this::toResponse)
                                .sorted(Comparator.comparing(RoomOccupancyResponse::getRoomName)
                                                .thenComparing(RoomOccupancyResponse::getDayOfWeek)
                                                .thenComparing(RoomOccupancyResponse::getPeriod))
                                .collect(Collectors.toList());
        }

        @Override
        public Page<RoomOccupancyResponse> getOccupanciesBySemester(Long semesterId, Pageable pageable) {
                log.info("Fetching occupancies for semesterId={} with pagination", semesterId);

                List<RoomOccupancy> occupancies = roomOccupancyRepository.findBySemesterId(semesterId);

                List<RoomOccupancyResponse> responses = occupancies.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());

                // Manual pagination
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), responses.size());

                List<RoomOccupancyResponse> pageContent = start >= responses.size() ? List.of()
                                : responses.subList(start, end);

                return new PageImpl<>(pageContent, pageable, responses.size());
        }

        @Override
        public Map<String, Object> getRoomUsageStatistics(Long semesterId) {
                log.info("Calculating room usage statistics for semesterId={}", semesterId);

                List<RoomOccupancy> occupancies = roomOccupancyRepository.findBySemesterId(semesterId);

                Map<String, Object> stats = new HashMap<>();

                // Tổng số phòng được sử dụng
                long totalRoomsUsed = occupancies.stream()
                                .map(RoomOccupancy::getRoom)
                                .map(Room::getId)
                                .distinct()
                                .count();
                stats.put("totalRoomsUsed", totalRoomsUsed);

                // Tổng số slots occupied
                stats.put("totalOccupiedSlots", occupancies.size());

                // Thống kê theo ngày
                Map<Integer, Long> occupancyByDay = occupancies.stream()
                                .collect(Collectors.groupingBy(RoomOccupancy::getDayOfWeek, Collectors.counting()));

                Map<String, Long> dayStats = new LinkedHashMap<>();
                for (int day = 2; day <= 7; day++) {
                        dayStats.put(DAY_NAMES.get(day), occupancyByDay.getOrDefault(day, 0L));
                }
                stats.put("occupancyByDay", dayStats);

                // Thống kê theo ca
                Map<Integer, Long> occupancyByPeriod = occupancies.stream()
                                .collect(Collectors.groupingBy(RoomOccupancy::getPeriod, Collectors.counting()));

                Map<String, Long> periodStats = new LinkedHashMap<>();
                for (int period = 1; period <= 6; period++) {
                        periodStats.put(PERIOD_NAMES.get(period), occupancyByPeriod.getOrDefault(period, 0L));
                }
                stats.put("occupancyByPeriod", periodStats);

                // Top 10 phòng được sử dụng nhiều nhất
                List<Map<String, Object>> topRooms = occupancies.stream()
                                .collect(Collectors.groupingBy(RoomOccupancy::getRoom, Collectors.counting()))
                                .entrySet().stream()
                                .sorted(Map.Entry.<Room, Long>comparingByValue().reversed())
                                .limit(10)
                                .map(entry -> {
                                        Map<String, Object> roomInfo = new HashMap<>();
                                        roomInfo.put("roomName", entry.getKey().getName());
                                        roomInfo.put("building", entry.getKey().getBuilding());
                                        roomInfo.put("occupiedSlots", entry.getValue());
                                        return roomInfo;
                                })
                                .collect(Collectors.toList());
                stats.put("topRooms", topRooms);

                return stats;
        }

        @Override
        public boolean isSlotAvailable(Long roomId, Long semesterId, Integer dayOfWeek, Integer period) {
                log.info("Checking slot availability: roomId={}, semesterId={}, day={}, period={}",
                                roomId, semesterId, dayOfWeek, period);

                Optional<RoomOccupancy> existing = roomOccupancyRepository
                                .findByRoomIdAndSemesterIdAndDayOfWeekAndPeriod(roomId, semesterId, dayOfWeek, period);

                return existing.isEmpty();
        }

        @Override
        @Transactional
        public void deleteOccupanciesBySemester(Long semesterId) {
                log.info("Deleting all room occupancies for semesterId={}", semesterId);

                // Kiểm tra semester có tồn tại không
                if (!semesterRepository.existsById(semesterId)) {
                        log.warn("Semester not found with id: {}", semesterId);
                        throw new RuntimeException("Không tìm thấy học kỳ với ID: " + semesterId);
                }

                // Xóa tất cả occupancies của semester
                roomOccupancyRepository.deleteBySemesterId(semesterId);

                log.info("Successfully deleted all room occupancies for semesterId={}", semesterId);
        }

        @Override
        @Transactional
        public List<RoomOccupancyResponse> bulkCreateOccupancies(BulkCreateRoomOccupancyRequest request) {
                log.info("Bulk creating {} room occupancies for semesterId={}",
                                request.getItems().size(), request.getSemesterId());

                // Validate semester exists
                Semester semester = semesterRepository.findById(request.getSemesterId())
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Không tìm thấy học kỳ với ID: " + request.getSemesterId()));

                List<RoomOccupancy> occupancies = new ArrayList<>();
                int skipCount = 0;

                for (BulkCreateRoomOccupancyRequest.OccupancyItem item : request.getItems()) {
                        try {
                                // Skip if roomId is null (phòng chưa được gán)
                                if (item.getRoomId() == null) {
                                        skipCount++;
                                        continue;
                                }

                                // Validate room exists
                                Room room = roomRepository.findById(item.getRoomId())
                                                .orElseThrow(() -> new ResourceNotFoundException(
                                                                "Không tìm thấy phòng với ID: " + item.getRoomId()));

                                // Kip to Period mapping: 1 kip = 1 period
                                Integer period = item.getKip();

                                // Check if already occupied
                                boolean exists = roomOccupancyRepository
                                                .existsByRoomIdAndSemesterIdAndDayOfWeekAndPeriod(
                                                                item.getRoomId(), request.getSemesterId(),
                                                                item.getDayOfWeek(), period);

                                if (exists) {
                                        log.debug("Room occupancy already exists: roomId={}, day={}, period={}",
                                                        item.getRoomId(), item.getDayOfWeek(), period);
                                        skipCount++;
                                        continue;
                                }

                                // Create unique key
                                String uniqueKey = String.format("%s-%s|%d|%d",
                                                room.getName(), room.getBuilding(), item.getDayOfWeek(), period);

                                // Build note
                                String note = null;
                                if (item.getSubjectCode() != null || item.getSubjectName() != null) {
                                        note = String.format("%s - %s",
                                                        item.getSubjectCode() != null ? item.getSubjectCode() : "",
                                                        item.getSubjectName() != null ? item.getSubjectName() : "")
                                                        .trim();
                                }

                                // Create occupancy
                                RoomOccupancy occupancy = RoomOccupancy.builder()
                                                .room(room)
                                                .semester(semester)
                                                .dayOfWeek(item.getDayOfWeek())
                                                .period(period)
                                                .uniqueKey(uniqueKey)
                                                .note(note)
                                                .build();

                                occupancies.add(occupancy);

                        } catch (Exception e) {
                                log.error("Error processing roomId={}: {}", item.getRoomId(), e.getMessage());
                                skipCount++;
                        }
                }

                // Save all occupancies
                if (!occupancies.isEmpty()) {
                        occupancies = roomOccupancyRepository.saveAll(occupancies);
                        log.info("Successfully created {} room occupancies (skipped: {})",
                                        occupancies.size(), skipCount);
                }

                return occupancies.stream()
                                .map(this::toResponse)
                                .collect(Collectors.toList());
        }

        @Override
        public List<RoomWithOccupancyStatus> getRoomsWithOccupancyStatus(Long semesterId) {
                log.info("Fetching all rooms with occupancy status for semesterId={}", semesterId);

                // Validate semester
                Semester semester = semesterRepository.findById(semesterId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Không tìm thấy học kỳ với ID: " + semesterId));

                // Get all rooms
                List<Room> allRooms = roomRepository.findAll();

                // Get all occupancies for this semester
                List<RoomOccupancy> allOccupancies = roomOccupancyRepository.findBySemesterId(semesterId);

                // Group occupancies by room
                Map<Long, List<RoomOccupancy>> occupanciesByRoom = allOccupancies.stream()
                                .collect(Collectors.groupingBy(occ -> occ.getRoom().getId()));

                // Build response
                return allRooms.stream()
                                .map(room -> buildRoomWithOccupancyStatus(room, semester,
                                                occupanciesByRoom.getOrDefault(room.getId(), List.of())))
                                .sorted(Comparator.comparing(RoomWithOccupancyStatus::getBuilding)
                                                .thenComparing(RoomWithOccupancyStatus::getName))
                                .collect(Collectors.toList());
        }

        @Override
        public Page<RoomWithOccupancyStatus> getRoomsWithOccupancyStatus(Long semesterId, Pageable pageable) {
                log.info("Fetching rooms with occupancy status for semesterId={} with pagination", semesterId);

                // Get all data first
                List<RoomWithOccupancyStatus> allRooms = getRoomsWithOccupancyStatus(semesterId);

                // Manual pagination
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), allRooms.size());

                List<RoomWithOccupancyStatus> pageContent = start >= allRooms.size() ? List.of()
                                : allRooms.subList(start, end);

                return new PageImpl<>(pageContent, pageable, allRooms.size());
        }

        @Override
        public Page<RoomWithOccupancyStatus> getRoomsWithOccupancyStatusWithFilters(
                        Long semesterId,
                        String building,
                        com.ptit.schedule.entity.RoomType type,
                        com.ptit.schedule.entity.OccupancyStatus occupancyStatus,
                        Integer minCapacity,
                        Integer maxCapacity,
                        String search,
                        Pageable pageable) {
                log.info("Fetching rooms with filters: building={}, type={}, occupancyStatus={}, minCapacity={}, maxCapacity={}, search={}",
                                building, type, occupancyStatus, minCapacity, maxCapacity, search);

                List<RoomWithOccupancyStatus> allRooms = getRoomsWithOccupancyStatus(semesterId);

                // Apply filters
                List<RoomWithOccupancyStatus> filteredRooms = allRooms.stream()
                                .filter(room -> {
                                        // Filter by building
                                        if (building != null && !building.trim().isEmpty()) {
                                                if (room.getBuilding() == null || !room.getBuilding()
                                                                .equalsIgnoreCase(building.trim())) {
                                                        return false;
                                                }
                                        }

                                        // Filter by type
                                        if (type != null && !type.equals(room.getType())) {
                                                return false;
                                        }

                                        // Filter by occupancyStatus
                                        if (occupancyStatus != null
                                                        && !occupancyStatus.equals(room.getOccupancyStatus())) {
                                                return false;
                                        }

                                        // Filter by minCapacity
                                        if (minCapacity != null) {
                                                if (room.getCapacity() == null || room.getCapacity() < minCapacity) {
                                                        return false;
                                                }
                                        }

                                        // Filter by maxCapacity
                                        if (maxCapacity != null) {
                                                if (room.getCapacity() == null || room.getCapacity() > maxCapacity) {
                                                        return false;
                                                }
                                        }

                                        // Search in room name or building
                                        if (search != null && !search.trim().isEmpty()) {
                                                String searchLower = search.toLowerCase().trim();
                                                boolean matchName = room.getName() != null
                                                                && room.getName().toLowerCase().contains(searchLower);
                                                boolean matchBuilding = room.getBuilding() != null
                                                                && room.getBuilding().toLowerCase()
                                                                                .contains(searchLower);

                                                return matchName || matchBuilding;
                                        }

                                        return true;
                                })
                                .collect(Collectors.toList());

                // Manual pagination
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), filteredRooms.size());

                List<RoomWithOccupancyStatus> pageContent = start >= filteredRooms.size() ? List.of()
                                : filteredRooms.subList(start, end);

                return new PageImpl<>(pageContent, pageable, filteredRooms.size());
        }

        @Override
        public RoomWithOccupancyStatus getRoomOccupancyStatus(Long roomId, Long semesterId) {
                log.info("Fetching occupancy status for roomId={}, semesterId={}", roomId, semesterId);

                // Validate room
                Room room = roomRepository.findById(roomId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Không tìm thấy phòng với ID: " + roomId));

                // Validate semester
                Semester semester = semesterRepository.findById(semesterId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Không tìm thấy học kỳ với ID: " + semesterId));

                // Get occupancies
                List<RoomOccupancy> occupancies = roomOccupancyRepository
                                .findByRoomIdAndSemesterId(roomId, semesterId);

                return buildRoomWithOccupancyStatus(room, semester, occupancies);
        }

        @Override
        public List<RoomWithOccupancyStatus> getAvailableRoomsByDayAndPeriod(
                        Long semesterId, Integer dayOfWeek, Integer period) {
                log.info("Fetching available rooms for semesterId={}, day={}, period={}",
                                semesterId, dayOfWeek, period);

                // Validate semester
                Semester semester = semesterRepository.findById(semesterId)
                                .orElseThrow(() -> new ResourceNotFoundException(
                                                "Không tìm thấy học kỳ với ID: " + semesterId));

                // Get all rooms
                List<Room> allRooms = roomRepository.findAll();

                // Get occupied rooms at this time slot
                List<RoomOccupancy> occupiedAtThisTime = roomOccupancyRepository
                                .findBySemesterIdAndDayOfWeekAndPeriod(semesterId, dayOfWeek, period);

                Set<Long> occupiedRoomIds = occupiedAtThisTime.stream()
                                .map(occ -> occ.getRoom().getId())
                                .collect(Collectors.toSet());

                // Filter available rooms
                return allRooms.stream()
                                .filter(room -> !occupiedRoomIds.contains(room.getId()))
                                .map(room -> {
                                        List<RoomOccupancy> roomOccupancies = roomOccupancyRepository
                                                        .findByRoomIdAndSemesterId(room.getId(), semesterId);
                                        return buildRoomWithOccupancyStatus(room, semester, roomOccupancies);
                                })
                                .sorted(Comparator.comparing(RoomWithOccupancyStatus::getBuilding)
                                                .thenComparing(RoomWithOccupancyStatus::getName))
                                .collect(Collectors.toList());
        }

        @Override
        public Page<RoomWithOccupancyStatus> getAvailableRoomsByDayAndPeriod(
                        Long semesterId, Integer dayOfWeek, Integer period, Pageable pageable) {
                log.info("Fetching available rooms for semesterId={}, day={}, period={} with pagination",
                                semesterId, dayOfWeek, period);

                // Get all data first
                List<RoomWithOccupancyStatus> allAvailableRooms = getAvailableRoomsByDayAndPeriod(
                                semesterId, dayOfWeek, period);

                // Manual pagination
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), allAvailableRooms.size());

                List<RoomWithOccupancyStatus> pageContent = start >= allAvailableRooms.size() ? List.of()
                                : allAvailableRooms.subList(start, end);

                return new PageImpl<>(pageContent, pageable, allAvailableRooms.size());
        }

        private RoomWithOccupancyStatus buildRoomWithOccupancyStatus(
                        Room room, Semester semester, List<RoomOccupancy> occupancies) {

                // Calculate statistics
                int totalSlots = 36; // 6 days * 6 periods
                int occupiedSlots = occupancies.size();
                int availableSlots = totalSlots - occupiedSlots;
                double occupancyRate = (double) occupiedSlots / totalSlots * 100;

                // Determine status based on occupancy rate
                com.ptit.schedule.entity.OccupancyStatus status;
                if (occupiedSlots == 0) {
                        status = OccupancyStatus.AVAILABLE; // Phòng trống hoàn toàn
                } else if (occupiedSlots >= totalSlots) {
                        status = OccupancyStatus.UNAVAILABLE; // Phòng đã đầy
                } else {
                        status = OccupancyStatus.USED; // Phòng đang sử dụng một phần
                }

                // Build occupied slots
                List<RoomWithOccupancyStatus.OccupiedSlot> occupiedSlotsList = occupancies.stream()
                                .map(occ -> RoomWithOccupancyStatus.OccupiedSlot.builder()
                                                .dayOfWeek(occ.getDayOfWeek())
                                                .dayName(DAY_NAMES.get(occ.getDayOfWeek()))
                                                .period(occ.getPeriod())
                                                .periodName(PERIOD_NAMES.get(occ.getPeriod()))
                                                .note(occ.getNote())
                                                .build())
                                .sorted(Comparator.comparing(RoomWithOccupancyStatus.OccupiedSlot::getDayOfWeek)
                                                .thenComparing(RoomWithOccupancyStatus.OccupiedSlot::getPeriod))
                                .collect(Collectors.toList());

                return RoomWithOccupancyStatus.builder()
                                .id(room.getId())
                                .name(room.getName())
                                .capacity(room.getCapacity())
                                .building(room.getBuilding())
                                .type(room.getType())
                                .typeDisplayName(room.getType().getDisplayName())
                                .semesterId(semester.getId())
                                .semesterName(semester.getSemesterName())
                                .academicYear(semester.getAcademicYear())
                                .totalOccupiedSlots(occupiedSlots)
                                .totalAvailableSlots(availableSlots)
                                .occupancyRate(Math.round(occupancyRate * 100.0) / 100.0)
                                .occupancyStatus(status)
                                .occupiedSlots(occupiedSlotsList)
                                .build();
        }

        /**
         * Chuyển đổi entity sang response DTO
         */
        private RoomOccupancyResponse toResponse(RoomOccupancy occupancy) {
                Room room = occupancy.getRoom();
                Semester semester = occupancy.getSemester();

                return RoomOccupancyResponse.builder()
                                .id(occupancy.getId())
                                .roomId(room.getId())
                                .roomName(room.getName())
                                .building(room.getBuilding())
                                .semesterId(semester.getId())
                                .semesterName(semester.getSemesterName())
                                .academicYear(semester.getAcademicYear())
                                .dayOfWeek(occupancy.getDayOfWeek())
                                .dayOfWeekName(DAY_NAMES.get(occupancy.getDayOfWeek()))
                                .period(occupancy.getPeriod())
                                .periodName(PERIOD_NAMES.get(occupancy.getPeriod()))
                                .uniqueKey(occupancy.getUniqueKey())
                                .note(occupancy.getNote())
                                .build();
        }
}
