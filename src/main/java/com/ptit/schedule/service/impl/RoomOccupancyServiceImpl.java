package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.RoomOccupancyResponse;
import com.ptit.schedule.entity.Room;
import com.ptit.schedule.entity.RoomOccupancy;
import com.ptit.schedule.entity.Semester;
import com.ptit.schedule.repository.RoomOccupancyRepository;
import com.ptit.schedule.repository.RoomRepository;
import com.ptit.schedule.repository.SemesterRepository;
import com.ptit.schedule.service.RoomOccupancyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                        1, "Ca 1",
                        2, "Ca 2",
                        3, "Ca 3",
                        4, "Ca 4",
                        5, "Ca 5",
                        6, "Ca 6");

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
