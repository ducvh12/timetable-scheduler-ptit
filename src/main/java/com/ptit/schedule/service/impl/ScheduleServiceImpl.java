package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.*;
import com.ptit.schedule.entity.Room;
import com.ptit.schedule.entity.Schedule;
import com.ptit.schedule.entity.Subject;
import com.ptit.schedule.entity.Semester;
import com.ptit.schedule.exception.InvalidDataException;
import com.ptit.schedule.repository.ScheduleRepository;
import com.ptit.schedule.repository.SemesterRepository;
import com.ptit.schedule.repository.SubjectRepository;
import com.ptit.schedule.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleServiceImpl implements ScheduleService {
    private final ScheduleRepository scheduleRepository;
    private final DataLoaderService dataLoaderService;
    private final RoomService roomService;
    private final SubjectRoomMappingService subjectRoomMappingService;
    private final RedisOccupiedRoomService redisOccupiedRoomService;
    private final SemesterRepository semesterRepository;
    private final SubjectRepository subjectRepository;

    private Set<String> sessionOccupiedRooms = new HashSet<>();

    private static final List<TimetableSlot> ROTATING_SLOTS = Arrays.asList(
            new TimetableSlot(2, "sang"), new TimetableSlot(3, "chieu"),
            new TimetableSlot(4, "sang"), new TimetableSlot(5, "chieu"),
            new TimetableSlot(6, "sang"), new TimetableSlot(7, "chieu"),
            new TimetableSlot(2, "chieu"), new TimetableSlot(3, "sang"),
            new TimetableSlot(4, "chieu"), new TimetableSlot(5, "sang"),
            new TimetableSlot(6, "chieu"), new TimetableSlot(7, "sang"));

    private static final List<DayPairSlot> ROTATING_SLOTS_60 = Arrays.asList(
            new DayPairSlot(2, 3, 1),
            new DayPairSlot(2, 3, 2),
            new DayPairSlot(4, 5, 3),
            new DayPairSlot(4, 5, 4),
            new DayPairSlot(6, 7, 1),
            new DayPairSlot(6, 7, 2),
            new DayPairSlot(2, 3, 3),
            new DayPairSlot(2, 3, 4),
            new DayPairSlot(4, 5, 1),
            new DayPairSlot(4, 5, 2),
            new DayPairSlot(6, 7, 3),
            new DayPairSlot(6, 7, 4));

    private int lastSlotIdx = -1;
    private int sessionLastSlotIdx = -1;

    @Override
    public void saveAll(List<Schedule> schedules) {
        scheduleRepository.saveAll(schedules);
    }

    @Override
    public List<Schedule> getAllSchedules() {
        return scheduleRepository.findAll();
    }

    @Override
    public List<Schedule> getSchedulesBySubjectId(String subjectId) {
        return scheduleRepository.findBySubjectId(subjectId);
    }

    @Override
    public List<Schedule> getSchedulesByMajor(String major) {
        return scheduleRepository.findByMajor(major);
    }

    @Override
    public List<Schedule> getSchedulesByStudentYear(String studentYear) {
        return scheduleRepository.findByStudentYear(studentYear);
    }

    @Override
    public List<Schedule> getSchedulesByUserId(Long userId) {
        return scheduleRepository.findByUserIdOrderByIdAsc(userId);
    }

    @Override
    public void deleteScheduleById(Long id) {
        scheduleRepository.deleteById(id);
    }

    @Override
    public void deleteAllSchedules() {
        scheduleRepository.deleteAll();
    }

    // ==================== TIMETABLE GENERATION OPERATIONS ====================

    /**
     * Tạo thời khóa biểu cho danh sách môn học
     */
    @Override
    public TKBBatchResponse simulateExcelFlowBatch(TKBBatchRequest request) {
        // Lấy thông tin từ request
        Long userId = request.getUserId();

        // Lấy academicYear và semester từ item đầu tiên (vì tất cả items cùng học kỳ,
        // năm học)
        String academicYear = request.getAcademicYear();
        String semester = request.getSemester();

        if ((academicYear == null || semester == null) && !request.getItems().isEmpty()) {
            TKBRequest firstItem = request.getItems().get(0);
            academicYear = firstItem.getAcademic_year();
            semester = firstItem.getSemester();
        }

        // Auto-detect và set semesterId cho DataLoaderService
        if (academicYear != null && semester != null) {
            Optional<Semester> semesterEntity = semesterRepository
                    .findBySemesterNameAndAcademicYear(semester, academicYear);

            if (semesterEntity.isPresent()) {
                Long semesterId = semesterEntity.get().getId();
                dataLoaderService.setCurrentSemesterId(semesterId);
                log.info("✅ Auto-detected semesterId: {} for {}/{}", semesterId, academicYear, semester);
            } else {
                log.warn("⚠️ Semester not found for {}/{}, will fallback to JSON", academicYear, semester);
                dataLoaderService.setCurrentSemesterId(null);
            }
        } else {
            log.warn("⚠️ academicYear/semester is null, will fallback to JSON");
            dataLoaderService.setCurrentSemesterId(null);
        }

        System.out.println("📋 [ScheduleService] Request Info:");
        System.out.println("   - userId: " + userId);
        System.out.println("   - academicYear: " + academicYear);
        System.out.println("   - semester (raw): " + semester);
        
        // Normalize semester: "1" -> "HK1", "2" -> "HK2", "HK1" -> "HK1"
        String normalizedSemester = semester;
        if (semester != null && semester.matches("^[12]$")) {
            normalizedSemester = "HK" + semester;
        }
        System.out.println("   - semester (normalized): " + normalizedSemester);
        
        // Load template data for this semester
        String semesterKey = normalizedSemester + " " + academicYear; // VD: "HK1 2024-2025"
        System.out.println("   - semesterKey for loading: " + semesterKey);
        
        List<DataLoaderService.TKBTemplateRow> dataRows = dataLoaderService.loadTemplateData(semesterKey);
        if (dataRows.isEmpty()) {
            throw new InvalidDataException("Chưa có dữ liệu lịch mẫu cho " + semesterKey + ". Vui lòng import dữ liệu lịch mẫu trước khi sinh TKB.");
        }
        
        System.out.println("✅ [ScheduleService] Loaded " + dataRows.size() + " templates for " + semesterKey);

        List<Room> rooms = loadRooms();
        Set<Object> occupiedRooms = initializeOccupiedRooms(userId, academicYear, semester);

        List<TKBBatchItemResponse> itemsOut = new ArrayList<>();
        int totalRows = 0;
        int totalClasses = 0;

        // Load lastSlotIdx từ Redis
        if (userId != null && academicYear != null && semester != null) {
            lastSlotIdx = redisOccupiedRoomService.loadLastSlotIdx(userId, academicYear, semester);
            System.out.println("✅ [ScheduleService] Load lastSlotIdx từ REDIS: " + lastSlotIdx +
                    " (key: " + userId + ":" + academicYear + ":" + semester + ")");
        } else {
            System.out.println(
                    "⚠️ [ScheduleService] Cannot load lastSlotIdx: userId/academicYear/semester null. Using -1");
            lastSlotIdx = -1;
        }
        sessionLastSlotIdx = lastSlotIdx;

        List<TKBRequest> sortedItems = sortSubjectsByPeriods(request.getItems());

        for (TKBRequest tkbRequest : sortedItems) {
            TKBBatchItemResponse itemResponse = processSubject(tkbRequest, dataRows, rooms, occupiedRooms);
            itemsOut.add(itemResponse);

            if (!itemResponse.getRows().isEmpty()) {
                totalRows += itemResponse.getRows().size();
                totalClasses++;
            }
        }

        return TKBBatchResponse.builder()
                .items(itemsOut)
                .totalRows(totalRows)
                .totalClasses(totalClasses)
                .lastSlotIdx(sessionLastSlotIdx)
                .occupiedRoomsCount(sessionOccupiedRooms.size())
                .build();
    }

    /**
     * Lưu lastSlotIdx vào Redis (occupied rooms vẫn lưu vào file)
     */
    @Override
    public void commitSessionToRedis(Long userId, String academicYear, String semester) {
        // Save occupied rooms to file (không đổi)
        if (!sessionOccupiedRooms.isEmpty()) {
            Set<Object> globalOccupied = dataLoaderService.loadGlobalOccupiedRooms();
            globalOccupied.addAll(sessionOccupiedRooms);
            dataLoaderService.saveGlobalOccupiedRooms(globalOccupied);
            sessionOccupiedRooms.clear();
        }

        // Save lastSlotIdx to Redis
        if (userId != null && academicYear != null && semester != null) {
            redisOccupiedRoomService.saveLastSlotIdx(userId, academicYear, semester, sessionLastSlotIdx);
            System.out.println("✅ [ScheduleService] Save lastSlotIdx vào REDIS: " + sessionLastSlotIdx +
                    " (key: " + userId + ":" + academicYear + ":" + semester + ")");
        } else {
            System.out.println("⚠️ [ScheduleService] Cannot save lastSlotIdx: userId/academicYear/semester null");
        }

        lastSlotIdx = sessionLastSlotIdx;
    }

    @Override
    public void resetState() {
        lastSlotIdx = -1;
    }

    @Override
    public void resetOccupiedRooms() {
        sessionOccupiedRooms.clear();

        Set<Object> emptySet = new HashSet<>();
        dataLoaderService.saveGlobalOccupiedRooms(emptySet);

        lastSlotIdx = -1;
        sessionLastSlotIdx = -1;
    }

    @Override
    public void resetLastSlotIndexRedis(Long userId, String academicYear, String semester) {
        if (userId != null && academicYear != null && semester != null) {
            redisOccupiedRoomService.clearLastSlotIdx(userId, academicYear, semester);
        }
        lastSlotIdx = -1;
        sessionLastSlotIdx = -1;
    }

    @Override
    public Map<String, Integer> getOccupiedRoomsInfo() {
        Set<Object> globalRooms = dataLoaderService.loadGlobalOccupiedRooms();

        Map<String, Integer> info = new HashMap<>();
        info.put("session", sessionOccupiedRooms.size());
        info.put("global", globalRooms.size());
        info.put("total", sessionOccupiedRooms.size() + globalRooms.size());

        return info;
    }

    // ==================== PRIVATE HELPER METHODS ====================

    private List<Room> loadRooms() {
        return roomService.getAllRooms().stream()
                .map(this::convertToRoom)
                .collect(Collectors.toList());
    }

    private Set<Object> initializeOccupiedRooms(Long userId, String academicYear, String semester) {
        sessionOccupiedRooms.clear();
        subjectRoomMappingService.clearMappings();

        Set<Object> globalOccupiedRooms = dataLoaderService.loadGlobalOccupiedRooms();
        return new HashSet<>(globalOccupiedRooms);
    }

    private List<TKBRequest> sortSubjectsByPeriods(List<TKBRequest> items) {
        List<TKBRequest> sorted = new ArrayList<>(items);
        sorted.sort((a, b) -> {
            if (a.getSotiet() == 60 && b.getSotiet() != 60)
                return -1;
            if (a.getSotiet() != 60 && b.getSotiet() == 60)
                return 1;
            return 0;
        });
        return sorted;
    }

    private TKBBatchItemResponse processSubject(TKBRequest tkbRequest,
            List<DataLoaderService.TKBTemplateRow> dataRows,
            List<Room> rooms, Set<Object> occupiedRooms) {

        int targetTotal = tkbRequest.getSotiet();

        List<DataLoaderService.TKBTemplateRow> pool = dataRows.stream()
                .filter(row -> toInt(row.getTotalPeriods()) == targetTotal)
                .collect(Collectors.toList());

        if (pool.isEmpty()) {
            throw new InvalidDataException(
                    "Không có Data cho " + targetTotal + " tiết (Môn: " + tkbRequest.getMa_mon() + ")");
        }

        int classes = Math.max(1, toInt(tkbRequest.getSolop(), 1));
        List<TKBRowResult> resultRows;
        int startingSlotIdx;

        if (targetTotal == 60) {
            startingSlotIdx = mapRegularSlotTo60PeriodSlot(sessionLastSlotIdx);
            resultRows = process60PeriodSubject(tkbRequest, pool, rooms, occupiedRooms, startingSlotIdx);
        } else {
            startingSlotIdx = (sessionLastSlotIdx + 1) % ROTATING_SLOTS.size();
            resultRows = processRegularSubject(tkbRequest, pool, rooms, occupiedRooms, startingSlotIdx, classes,
                    targetTotal);
        }

        if (!resultRows.isEmpty()) {
            int majorEndSlot = calculateMajorEndSlot(classes, targetTotal);
            sessionLastSlotIdx = (startingSlotIdx + majorEndSlot) % ROTATING_SLOTS.size();
        }

        return TKBBatchItemResponse.builder()
                .input(tkbRequest)
                .rows(resultRows)
                .build();
    }

    private int calculateAH(DataLoaderService.TKBTemplateRow row) {
        int L = row.getPeriodLength();
        List<Integer> weekSchedule = row.getWeekSchedule();
        int xCount = 0;
        for (Integer week : weekSchedule) {
            if (week != null && week == 1) {
                xCount++;
            }
        }
        return L * xCount;
    }

    private List<String> normalizeSlots(DataLoaderService.TKBTemplateRow row) {
        List<String> weeks = new ArrayList<>();
        List<Integer> weekSchedule = row.getWeekSchedule();

        for (Integer week : weekSchedule) {
            if (week != null && week == 1) {
                weeks.add("X");
            } else {
                weeks.add("");
            }
        }

        while (weeks.size() < 18) {
            weeks.add("");
        }
        if (weeks.size() > 18) {
            weeks = weeks.subList(0, 18);
        }

        return weeks;
    }

    private TKBRowResult emitRow(int cls, TKBRequest payload, DataLoaderService.TKBTemplateRow row, int aiBefore,
            String roomCode, String maPhong) {
        int L = row.getPeriodLength();
        Integer thu = row.getDayOfWeek();
        Integer kip = row.getKip();
        Integer tietBd = row.getStartPeriod();
        String keyN = row.getId();
        List<String> weeks = normalizeSlots(row);
        int AH = calculateAH(row);
        int aj = aiBefore - AH;

        return TKBRowResult.builder()
                .lop(cls)
                .maMon(payload.getMa_mon())
                .tenMon(payload.getTen_mon())
                .kip(kip)
                .thu(thu)
                .tietBd(tietBd)
                .L(L)
                .phong(maPhong)
                .AH(AH)
                .AI(aiBefore)
                .AJ(aj)
                .N(keyN)
                .O_to_AG(weeks)
                .templateDatabaseId(row.getDatabaseId())
                .studentYear(payload.getStudent_year())
                .heDacThu(payload.getHe_dac_thu())
                .nganh(payload.getNganh())
                .siSoMotLop(payload.getSiso_mot_lop())
                .academicYear(payload.getAcademicYear())
                .semester(payload.getSemester())
                .subjectDatabaseId(findSubjectId(payload))
                .build();
    }

    private Long findSubjectId(TKBRequest payload) {
        try {
            // Normalize semester
            String normalizedSemester = normalizeSemesterString(payload.getSemester());
            
            // Find matching subjects and take first one
            List<Subject> subjects = subjectRepository.findAllBySubjectCodeAndSemesterAndAcademicYear(
                payload.getMa_mon(), 
                normalizedSemester, 
                payload.getAcademicYear()
            );
            
            if (subjects.isEmpty()) {
                System.out.println("⚠️ Subject not found - Code: " + payload.getMa_mon() + 
                    ", Semester: " + normalizedSemester + 
                    ", AcademicYear: " + payload.getAcademicYear());
                return null;
            }
            
            return subjects.get(0).getId();
        } catch (Exception e) {
            System.err.println("❌ Error finding subject: " + e.getMessage());
            return null;
        }
    }

    private String normalizeSemesterString(String semester) {
        // Không làm gì cả, trả về nguyên giá trị từ frontend
        return semester != null ? semester : "HK1";
    }

    private int calculateMajorEndSlot(int classes, int targetTotal) {
        if (classes <= 0)
            return -1;

        int lastClassSlotIdx;
        if (targetTotal == 14) {
            lastClassSlotIdx = (classes - 1) / 4;
        } else {
            lastClassSlotIdx = (classes - 1) / 2;
        }

        return lastClassSlotIdx % ROTATING_SLOTS.size();
    }

    private int mapRegularSlotTo60PeriodSlot(int regularSlotIdx) {
        int pairIndex = (regularSlotIdx / 2) + 1;
        int slot60Index = (pairIndex * 4) % ROTATING_SLOTS_60.size();
        return slot60Index;
    }

    private List<TKBRowResult> processRegularSubject(
            TKBRequest tkbRequest,
            List<DataLoaderService.TKBTemplateRow> pool,
            List<Room> rooms,
            Set<Object> occupiedRooms,
            int startingSlotIdx,
            int classes,
            int targetTotal) {

        List<TKBRowResult> resultRows = new ArrayList<>();
        int idx = 0;

        for (int cls = 1; cls <= classes; cls++) {
            String classRoomCode = null;
            String classRoomMaPhong = null;

            int slotIdx;
            if (targetTotal == 14) {
                slotIdx = (startingSlotIdx + (cls - 1) / 4) % ROTATING_SLOTS.size();
            } else {
                slotIdx = (startingSlotIdx + (cls - 1) / 2) % ROTATING_SLOTS.size();
            }

            TimetableSlot targetSlot = ROTATING_SLOTS.get(slotIdx);
            Set<Integer> targetKips = targetSlot.getKipSet();

            int ai = targetTotal;
            int guard = 0;

            while (ai > 0 && guard < 10000) {
                DataLoaderService.TKBTemplateRow row = null;
                int attempts = 0;

                while (attempts < pool.size()) {
                    DataLoaderService.TKBTemplateRow candidate = pool.get(idx);
                    idx = (idx + 1) % pool.size();

                    Integer rowThu = candidate.getDayOfWeek();
                    Integer rowKip = candidate.getKip();

                    if (rowThu.equals(targetSlot.getThu()) && targetKips.contains(rowKip)) {
                        row = candidate;
                        break;
                    }
                    attempts++;
                }

                if (row == null) {
                    row = pool.get(idx);
                    idx = (idx + 1) % pool.size();
                }

                int ah = calculateAH(row);
                if (ah <= 0) {
                    guard++;
                    continue;
                }

                if (classRoomCode == null) {
                    RoomAssignment assignment = assignRoomForClass(tkbRequest, row, rooms, occupiedRooms);
                    if (assignment != null) {
                        classRoomCode = assignment.getRoomCode();
                        classRoomMaPhong = assignment.getMaPhong();
                    }
                }

                Integer currentTietBd = row.getStartPeriod();
                String rowRoomCode = (currentTietBd != null && currentTietBd == 12) ? null : classRoomCode;
                String rowRoomMaPhong = (currentTietBd != null && currentTietBd == 12) ? null : classRoomMaPhong;

                TKBRowResult resultRow = emitRow(cls, tkbRequest, row, ai, rowRoomCode, rowRoomMaPhong);
                resultRows.add(resultRow);

                ai -= ah;
                guard++;
            }

            if (ai > 0) {
                break;
            }
        }

        return resultRows;
    }

    private List<TKBRowResult> process60PeriodSubject(
            TKBRequest tkbRequest,
            List<DataLoaderService.TKBTemplateRow> pool,
            List<Room> rooms,
            Set<Object> occupiedRooms,
            int startingSlotIdx) {

        List<TKBRowResult> resultRows = new ArrayList<>();

        int classes = Math.max(1, toInt(tkbRequest.getSolop(), 1));

        Map<String, List<DataLoaderService.TKBTemplateRow>> groups = pool.stream()
                .collect(Collectors.groupingBy(row -> row.getDayOfWeek() + "_" + row.getKip()));

        for (int cls = 1; cls <= classes; cls++) {
            int slotIdx = (startingSlotIdx + (cls - 1)) % ROTATING_SLOTS_60.size();
            DayPairSlot dayPairSlot = ROTATING_SLOTS_60.get(slotIdx);

            Integer targetKip = dayPairSlot.getKip();

            String classRoomCode = null;
            String classRoomMaPhong = null;

            for (Integer currentDay : dayPairSlot.getDays()) {
                String groupKey = currentDay + "_" + targetKip;
                List<DataLoaderService.TKBTemplateRow> groupRows = groups.get(groupKey);

                if (groupRows == null || groupRows.isEmpty()) {
                    continue;
                }

                for (DataLoaderService.TKBTemplateRow row : groupRows) {
                    if (classRoomCode == null) {
                        RoomAssignment assignment = assignRoomForClass(tkbRequest, row, rooms, occupiedRooms);
                        if (assignment != null) {
                            classRoomCode = assignment.getRoomCode();
                            classRoomMaPhong = assignment.getMaPhong();

                            markRoomOccupiedForDays(classRoomMaPhong, dayPairSlot.getDays(), targetKip, occupiedRooms);
                        }
                    }

                    int ah = calculateAH(row);
                    Integer currentTietBd = row.getStartPeriod();
                    String rowRoomCode = (currentTietBd != null && currentTietBd == 12) ? null : classRoomCode;
                    String rowRoomMaPhong = (currentTietBd != null && currentTietBd == 12) ? null : classRoomMaPhong;

                    TKBRowResult resultRow = emitRow(cls, tkbRequest, row, ah, rowRoomCode, rowRoomMaPhong);
                    resultRows.add(resultRow);
                }
            }
        }

        return resultRows;
    }

    private int toInt(Object value, int defaultValue) {
        if (value == null)
            return defaultValue;
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            String str = value.toString().trim();
            if (str.isEmpty())
                return defaultValue;
            return (int) Double.parseDouble(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private int toInt(Object value) {
        return toInt(value, 0);
    }

    private RoomAssignment assignRoomForClass(TKBRequest tkbRequest,
            DataLoaderService.TKBTemplateRow row,
            List<Room> rooms, Set<Object> occupiedRooms) {

        Integer tietBd = row.getStartPeriod();
        Integer rowThu = row.getDayOfWeek();
        Integer rowKip = row.getKip();

        if (tietBd == null || tietBd == 12 || rowThu == null || rowKip == null) {
            return null;
        }

        RoomPickResult roomResult = roomService.pickRoom(
                rooms,
                tkbRequest.getSiso_mot_lop(),
                occupiedRooms,
                rowThu,
                rowKip,
                tkbRequest.getSubject_type(),
                tkbRequest.getStudent_year(),
                tkbRequest.getHe_dac_thu(),
                null,
                tkbRequest.getNganh(),
                tkbRequest.getMa_mon());

        if (!roomResult.hasRoom()) {
            return null;
        }

        String occupationKey = roomResult.getMaPhong() + "|" + rowThu + "|" + rowKip;
        occupiedRooms.add(occupationKey);
        sessionOccupiedRooms.add(occupationKey);

        return new RoomAssignment(roomResult.getRoomCode(), roomResult.getMaPhong());
    }

    private void markRoomOccupiedForDays(String maPhong, List<Integer> days,
            Integer kip, Set<Object> occupiedRooms) {
        for (Integer day : days) {
            String occupationKey = maPhong + "|" + day + "|" + kip;
            occupiedRooms.add(occupationKey);
            sessionOccupiedRooms.add(occupationKey);
        }
    }

    private Room convertToRoom(RoomResponse roomResponse) {
        return Room.builder()
                .id(roomResponse.getId())
                .name(roomResponse.getName())
                .capacity(roomResponse.getCapacity())
                .building(roomResponse.getBuilding())
                .type(roomResponse.getType())
                .status(roomResponse.getStatus())
                .note(roomResponse.getNote())
                .build();
    }

    private static class RoomAssignment {
        private final String roomCode;
        private final String maPhong;

        public RoomAssignment(String roomCode, String maPhong) {
            this.roomCode = roomCode;
            this.maPhong = maPhong;
        }

        public String getRoomCode() {
            return roomCode;
        }

        public String getMaPhong() {
            return maPhong;
        }
    }
}