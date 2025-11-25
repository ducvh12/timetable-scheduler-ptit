package com.ptit.schedule.service;

import com.ptit.schedule.dto.*;
import com.ptit.schedule.entity.Room;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service xử lý việc tạo thời khóa biểu tự động với phân bổ phòng học
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimetableSchedulingService {

    private final DataLoaderService dataLoaderService;
    private final RoomService roomService;
    private final SubjectRoomMappingService subjectRoomMappingService;

    private Set<Object> sessionOccupiedRooms = new HashSet<>();

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
            new DayPairSlot(6, 7, 4)
    );

    private int lastSlotIdx = -1;
    private int sessionLastSlotIdx = -1;

    /**
     * Khởi tạo service và load trạng thái từ file
     */
    @PostConstruct
    public void init() {
        log.info("Initializing TimetableSchedulingService...");
        lastSlotIdx = dataLoaderService.loadLastSlotIdx();
        sessionLastSlotIdx = lastSlotIdx;
        log.info("Loaded lastSlotIdx from file: {}", lastSlotIdx);
    }

    /**
     * Tạo thời khóa biểu cho danh sách môn học
     */
    public TKBBatchResponse simulateExcelFlowBatch(TKBBatchRequest request) {
        try {
            List<DataLoaderService.TKBTemplateRow> dataRows = dataLoaderService.loadTemplateData();
            if (dataRows.isEmpty()) {
                return buildEmptyResponse("Template data empty or not exists");
            }

            List<Room> rooms = loadRooms();
            Set<Object> occupiedRooms = initializeOccupiedRooms();
            
            List<TKBBatchItemResponse> itemsOut = new ArrayList<>();
            int totalRows = 0;
            int totalClasses = 0;
            
            sessionLastSlotIdx = lastSlotIdx;
            List<TKBRequest> sortedItems = sortSubjectsByPeriods(request.getItems());
            
            log.info("Processing {} subjects in original order (from frontend processingOrder)", sortedItems.size());

            for (TKBRequest tkbRequest : sortedItems) {
                TKBBatchItemResponse itemResponse = processSubject(tkbRequest, dataRows, rooms, occupiedRooms);
                itemsOut.add(itemResponse);
                
                if (!itemResponse.getRows().isEmpty()) {
                    totalRows += itemResponse.getRows().size();
                    totalClasses++;
                }
            }

            log.info("Generated TKB using {} rooms (temporary, not saved yet)", sessionOccupiedRooms.size());
            log.info("Session lastSlotIdx: {} (temporary, not committed yet)", sessionLastSlotIdx);

            return TKBBatchResponse.builder()
                    .items(itemsOut)
                    .totalRows(totalRows)
                    .totalClasses(totalClasses)
                    .lastSlotIdx(sessionLastSlotIdx)
                    .occupiedRoomsCount(sessionOccupiedRooms.size())
                    .build();

        } catch (Exception e) {
            log.error("Error in simulateExcelFlowBatch: {}", e.getMessage(), e);
            return buildEmptyResponse(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private TKBBatchResponse buildEmptyResponse(String errorMessage) {
        return TKBBatchResponse.builder()
                .items(Collections.emptyList())
                .error(errorMessage)
                .build();
    }

    private List<Room> loadRooms() {
        return roomService.getAllRooms().stream()
                .map(this::convertToRoom)
                .collect(Collectors.toList());
    }

    private Set<Object> initializeOccupiedRooms() {
        sessionOccupiedRooms.clear();
        subjectRoomMappingService.clearMappings();
        
        Set<Object> globalOccupiedRooms = dataLoaderService.loadGlobalOccupiedRooms();
        log.info("Loaded {} global occupied rooms (confirmed)", globalOccupiedRooms.size());
        
        return new HashSet<>(globalOccupiedRooms);
    }

    private List<TKBRequest> sortSubjectsByPeriods(List<TKBRequest> items) {
        List<TKBRequest> sorted = new ArrayList<>(items);
        sorted.sort((a, b) -> {
            if (a.getSotiet() == 60 && b.getSotiet() != 60) return -1;
            if (a.getSotiet() != 60 && b.getSotiet() == 60) return 1;
            return 0;
        });
        return sorted;
    }

    private TKBBatchItemResponse processSubject(TKBRequest tkbRequest, 
            List<DataLoaderService.TKBTemplateRow> dataRows,
            List<Room> rooms, Set<Object> occupiedRooms) {
        
        int targetTotal = tkbRequest.getSotiet();
        log.info("Processing subject: {} with {} periods", tkbRequest.getMa_mon(), targetTotal);

        List<DataLoaderService.TKBTemplateRow> pool = dataRows.stream()
                .filter(row -> toInt(row.getTotalPeriods()) == targetTotal)
                .collect(Collectors.toList());

        if (pool.isEmpty()) {
            return TKBBatchItemResponse.builder()
                    .input(tkbRequest)
                    .rows(Collections.emptyList())
                    .note("Không có Data cho " + targetTotal + " tiết")
                    .build();
        }

        int classes = Math.max(1, toInt(tkbRequest.getSolop(), 1));
        List<TKBRowResult> resultRows;
        int startingSlotIdx;

        if (targetTotal == 60) {
            log.info("Using SPECIAL 60-period logic for {}", tkbRequest.getMa_mon());
            startingSlotIdx = mapRegularSlotTo60PeriodSlot(sessionLastSlotIdx);
            resultRows = process60PeriodSubject(tkbRequest, pool, rooms, occupiedRooms, startingSlotIdx);
        } else {
            log.info("Using REGULAR logic: {} classes for {}", classes, tkbRequest.getMa_mon());
            startingSlotIdx = (sessionLastSlotIdx + 1) % ROTATING_SLOTS.size();
            resultRows = processRegularSubject(tkbRequest, pool, rooms, occupiedRooms, startingSlotIdx, classes, targetTotal);
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

    /**
     * Lưu dữ liệu phòng đã sử dụng từ session vào global storage
     */
    public void commitSessionToGlobal() {
        if (sessionOccupiedRooms.isEmpty()) {
            log.warn("No session occupied rooms to commit");
            return;
        }

        Set<Object> globalOccupied = dataLoaderService.loadGlobalOccupiedRooms();
        int beforeCount = globalOccupied.size();

        globalOccupied.addAll(sessionOccupiedRooms);
        int afterCount = globalOccupied.size();
        int addedCount = afterCount - beforeCount;

        dataLoaderService.saveGlobalOccupiedRooms(globalOccupied);
        lastSlotIdx = sessionLastSlotIdx;
        dataLoaderService.saveLastSlotIdx(lastSlotIdx);

        log.info("Committed {} new rooms to global. Total global rooms: {}", addedCount, afterCount);
        log.info("Committed and saved lastSlotIdx to file: {}", lastSlotIdx);

        sessionOccupiedRooms.clear();
    }

    /**
     * Tính tổng số tiết dạy trong một row
     */
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

    /**
     * Chuẩn hóa lịch tuần thành 18 tuần
     */
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

    /**
     * Tạo một hàng kết quả thời khóa biểu
     */
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
                .studentYear(payload.getStudent_year())
                .heDacThu(payload.getHe_dac_thu())
                .nganh(payload.getNganh())
                .academicYear(payload.getAcademicYear())
                .semester(payload.getSemester())
                .build();
    }

    /**
     * Tính slot kết thúc của môn học
     */
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

    /**
     * Chuyển đổi slot index từ môn thường sang môn 60 tiết
     */
    private int mapRegularSlotTo60PeriodSlot(int regularSlotIdx) {
        int pairIndex = (regularSlotIdx / 2) + 1;
        int slot60Index = (pairIndex * 4) % ROTATING_SLOTS_60.size();
        return slot60Index;
    }

    /**
     * Xử lý môn học thường (không phải 60 tiết)
     */
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

        log.info("Processing regular subject: {} classes", classes);

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
                log.warn("Not enough data to schedule all classes (remaining: {})", ai);
                break;
            }
        }

        return resultRows;
    }

    /**
     * Xử lý môn 60 tiết với logic ghép cặp ngày liên tiếp
     */
    private List<TKBRowResult> process60PeriodSubject(
            TKBRequest tkbRequest,
            List<DataLoaderService.TKBTemplateRow> pool,
            List<Room> rooms,
            Set<Object> occupiedRooms,
            int startingSlotIdx) {

        List<TKBRowResult> resultRows = new ArrayList<>();

        int classes = Math.max(1, toInt(tkbRequest.getSolop(), 1));
        log.info("Processing 60-period subject with {} classes", classes);

        Map<String, List<DataLoaderService.TKBTemplateRow>> groups = pool.stream()
                .collect(Collectors.groupingBy(row -> row.getDayOfWeek() + "_" + row.getKip()));

        log.info("Grouped 60-period data into {} groups", groups.size());

        for (int cls = 1; cls <= classes; cls++) {
            log.info("Processing 60-period class {}/{}", cls, classes);

            int slotIdx = (startingSlotIdx + (cls - 1)) % ROTATING_SLOTS_60.size();
            DayPairSlot dayPairSlot = ROTATING_SLOTS_60.get(slotIdx);

            Integer targetKip = dayPairSlot.getKip();

            log.info("Class {} using slot: days {}-{} with kip {}",
                    cls, dayPairSlot.getDay1(), dayPairSlot.getDay2(), targetKip);

            String classRoomCode = null;
            String classRoomMaPhong = null;

            for (Integer currentDay : dayPairSlot.getDays()) {
                String groupKey = currentDay + "_" + targetKip;
                List<DataLoaderService.TKBTemplateRow> groupRows = groups.get(groupKey);

                if (groupRows == null || groupRows.isEmpty()) {
                    log.warn("No data for day {} kip {}", currentDay, targetKip);
                    continue;
                }

                log.info("Processing group {} with {} rows for class {}", groupKey, groupRows.size(), cls);

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

        log.info("Generated {} rows for 60-period subject with {} classes", resultRows.size(), classes);
        return resultRows;
    }

    /**
     * Chuyển đổi giá trị về kiểu integer an toàn
     */
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

    /**
     * Reset trạng thái global
     */
    public void resetState() {
        lastSlotIdx = -1;
        log.info("Reset TKB scheduling state");
    }

    /**
     * Reset danh sách phòng đã sử dụng
     */
    public void resetOccupiedRooms() {
        int sessionCount = sessionOccupiedRooms.size();
        sessionOccupiedRooms.clear();

        Set<Object> emptySet = new HashSet<>();
        dataLoaderService.saveGlobalOccupiedRooms(emptySet);

        lastSlotIdx = -1;
        sessionLastSlotIdx = -1;

        log.info("Reset occupied rooms - Cleared {} session rooms, global storage, and lastSlotIdx", sessionCount);
    }

    /**
     * Reset chỉ số slot về -1
     */
    public void resetLastSlotIdx() {
        lastSlotIdx = -1;
        sessionLastSlotIdx = -1;

        dataLoaderService.saveLastSlotIdx(-1);

        log.info("Reset lastSlotIdx to -1 and saved to file");
    }

    /**
     * Lấy thông tin số lượng phòng đã sử dụng
     */
    public Map<String, Integer> getOccupiedRoomsInfo() {
        Set<Object> globalRooms = dataLoaderService.loadGlobalOccupiedRooms();

        Map<String, Integer> info = new HashMap<>();
        info.put("session", sessionOccupiedRooms.size());
        info.put("global", globalRooms.size());
        info.put("total", sessionOccupiedRooms.size() + globalRooms.size());

        return info;
    }

    /**
     * Gán phòng học cho một lớp học
     */
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
                tkbRequest.getMa_mon()
        );

        if (!roomResult.hasRoom()) {
            return null;
        }

        log.info("Assigned room {} in building {} for subject {} (major: {}, preferred: {})",
                roomResult.getRoomCode(), roomResult.getBuilding(),
                tkbRequest.getMa_mon(), tkbRequest.getNganh(),
                roomResult.isPreferredBuilding() ? "YES" : "NO");

        String occupationKey = roomResult.getMaPhong() + "|" + rowThu + "|" + rowKip;
        occupiedRooms.add(occupationKey);
        sessionOccupiedRooms.add(occupationKey);

        return new RoomAssignment(roomResult.getRoomCode(), roomResult.getMaPhong());
    }

    /**
     * Đánh dấu phòng đã sử dụng cho nhiều ngày
     */
    private void markRoomOccupiedForDays(String maPhong, List<Integer> days, 
            Integer kip, Set<Object> occupiedRooms) {
        for (Integer day : days) {
            String occupationKey = maPhong + "|" + day + "|" + kip;
            occupiedRooms.add(occupationKey);
            sessionOccupiedRooms.add(occupationKey);
        }
    }

    /**
     * Chuyển đổi RoomResponse sang Room Entity
     */
    private Room convertToRoom(RoomResponse roomResponse) {
        return Room.builder()
                .id(roomResponse.getId())
                .phong(roomResponse.getPhong())
                .capacity(roomResponse.getCapacity())
                .day(roomResponse.getDay())
                .type(roomResponse.getType())
                .status(roomResponse.getStatus())
                .note(roomResponse.getNote())
                .build();
    }

    /**
     * Class lưu thông tin phòng được gán
     */
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