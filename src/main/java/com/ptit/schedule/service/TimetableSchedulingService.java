package com.ptit.schedule.service;

import com.ptit.schedule.dto.*;
import com.ptit.schedule.model.RoomData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core TKB scheduling service - implements exact Python logic for timetable generation
 * WITH room assignment (matching Python _pick_room logic)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TimetableSchedulingService {

    private final DataLoaderService dataLoaderService;
    private final RoomService roomService;
    
    // TEMPORARY session storage - cleared when user generates new TKB without saving
    private Set<Object> sessionOccupiedRooms = new HashSet<>();

    // Rotating slots exactly matching Python
    private static final List<TimetableSlot> ROTATING_SLOTS = Arrays.asList(
        new TimetableSlot(2, "sang"), new TimetableSlot(3, "chieu"),
        new TimetableSlot(4, "sang"), new TimetableSlot(5, "chieu"), 
        new TimetableSlot(6, "sang"), new TimetableSlot(7, "chieu"),
        new TimetableSlot(2, "chieu"), new TimetableSlot(3, "sang"),
        new TimetableSlot(4, "chieu"), new TimetableSlot(5, "sang"),
        new TimetableSlot(6, "chieu"), new TimetableSlot(7, "sang")
    );

    // Rotating slots for 60-period subjects (paired consecutive days with same kip)
    // 12 slots: 3 pairs of days × 4 kips = 12 combinations
    private static final List<DayPairSlot> ROTATING_SLOTS_60 = Arrays.asList(
        new DayPairSlot(2, 3, 1),   // Thứ 2-3, Kíp 1
        new DayPairSlot(2, 3, 2),   // Thứ 2-3, Kíp 2
        new DayPairSlot(4, 5, 3),   // Thứ 4-5, Kíp 3
        new DayPairSlot(4, 5, 4),   // Thứ 4-5, Kíp 4
        new DayPairSlot(6, 7, 1),   // Thứ 6-7, Kíp 1
        new DayPairSlot(6, 7, 2),   // Thứ 6-7, Kíp 2
        new DayPairSlot(2, 3, 3),   // Thứ 2-3, Kíp 3
        new DayPairSlot(2, 3, 4),   // Thứ 2-3, Kíp 4
        new DayPairSlot(4, 5, 1),   // Thứ 4-5, Kíp 1
        new DayPairSlot(4, 5, 2),   // Thứ 4-5, Kíp 2
        new DayPairSlot(6, 7, 3),   // Thứ 6-7, Kíp 3
        new DayPairSlot(6, 7, 4)    // Thứ 6-7, Kíp 4
    );

    // Global state for batch processing (like Python)
    private int lastSlotIdx = -1;  // PERMANENT: Only updated when user confirms
    private int sessionLastSlotIdx = -1;  // TEMPORARY: Updated during generation

    /**
     * Initialize service - load lastSlotIdx from file
     */
    @PostConstruct
    public void init() {
        log.info("Initializing TimetableSchedulingService...");
        // Load lastSlotIdx from persistent storage
        lastSlotIdx = dataLoaderService.loadLastSlotIdx();
        sessionLastSlotIdx = lastSlotIdx;
        log.info("Loaded lastSlotIdx from file: {}", lastSlotIdx);
    }

    /**
     * Simulate Excel Flow Batch - exact Python logic with room assignment
     */
    public TKBBatchResponse simulateExcelFlowBatch(TKBBatchRequest request) {
        try {
            // Load template data once
            List<DataLoaderService.TKBTemplateRow> dataRows = dataLoaderService.loadTemplateData();
            if (dataRows.isEmpty()) {
                return TKBBatchResponse.builder()
                    .items(Collections.emptyList())
                    .note("Template data empty or not exists")
                    .build();
            }
            
            // Load rooms data for room assignment
            List<RoomData> rooms = dataLoaderService.loadRoomsData();
            
            // CLEAR session occupied rooms when starting new TKB generation
            sessionOccupiedRooms.clear();
            
            // Load global occupied rooms (PERMANENT - already confirmed by user)
            Set<Object> globalOccupiedRooms = dataLoaderService.loadGlobalOccupiedRooms();
            log.info("Loaded {} global occupied rooms (confirmed)", globalOccupiedRooms.size());
            
            // Start with global occupied rooms, will add session rooms during generation
            Set<Object> occupiedRooms = new HashSet<>(globalOccupiedRooms);

            List<TKBBatchItemResponse> itemsOut = new ArrayList<>();
            int totalRows = 0;
            
            // Use PERMANENT lastSlotIdx as starting point for this generation
            sessionLastSlotIdx = lastSlotIdx;

            // SORT: Process 60-period subjects FIRST, then others
            List<TKBRequest> sortedItems = new ArrayList<>(request.getItems());
            sortedItems.sort((a, b) -> {
                int aTotal = a.getSotiet();
                int bTotal = b.getSotiet();
                // 60-period subjects first (descending), then others (ascending)
                if (aTotal == 60 && bTotal != 60) return -1; // a first
                if (aTotal != 60 && bTotal == 60) return 1;  // b first
                return Integer.compare(aTotal, bTotal); // same priority: sort by total
            });
            log.info("Sorted {} subjects with 60-period subjects prioritized first", sortedItems.size());

            for (TKBRequest tkbRequest : sortedItems) {
                int targetTotal = tkbRequest.getSotiet();
                log.info("Processing subject: {} with {} periods", tkbRequest.getMa_mon(), targetTotal);

                // Filter template data by total periods (exact Python logic)
                List<DataLoaderService.TKBTemplateRow> pool = dataRows.stream()
                    .filter(row -> toInt(row.getTotalPeriods()) == targetTotal)
                    .collect(Collectors.toList());

                if (pool.isEmpty()) {
                    itemsOut.add(TKBBatchItemResponse.builder()
                        .input(tkbRequest)
                        .rows(Collections.emptyList())
                        .note("Không có Data cho " + targetTotal + " tiết")
                        .build());
                    continue;
                }

                List<TKBRowResult> resultRows;
                int classes;
                int startingSlotIdx;

                // BRANCH: Special handling for 60-period subjects vs regular subjects
                if (targetTotal == 60) {
                    log.info("Using SPECIAL 60-period logic for {}", tkbRequest.getMa_mon());
                    classes = Math.max(1, toInt(tkbRequest.getSolop(), 1));
                    
                    // Map from regular slot to 60-period slot
                    startingSlotIdx = mapRegularSlotTo60PeriodSlot(sessionLastSlotIdx);
                    log.info("Mapped regular slot {} to 60-period slot {}", sessionLastSlotIdx, startingSlotIdx);
                    
                    resultRows = process60PeriodSubject(tkbRequest, pool, rooms, occupiedRooms, startingSlotIdx);
                } else {
                    // REGULAR PROCESSING
                    classes = Math.max(1, toInt(tkbRequest.getSolop(), 1));
                    
                    // Calculate starting slot for regular subjects
                    startingSlotIdx = (sessionLastSlotIdx + 1) % ROTATING_SLOTS.size();
                    
                    log.info("Using REGULAR logic: {} classes for {}", classes, tkbRequest.getMa_mon());
                    resultRows = processRegularSubject(tkbRequest, pool, rooms, occupiedRooms, startingSlotIdx, classes, targetTotal);
                }

                // Common result handling
                if (!resultRows.isEmpty()) {
                    itemsOut.add(TKBBatchItemResponse.builder()
                        .input(tkbRequest)
                        .rows(resultRows)
                        .build());
                    totalRows += resultRows.size();
                    
                    // Calculate end slot of this major (exact Python logic)
                    // Update TEMPORARY sessionLastSlotIdx (will be committed later)
                    int majorEndSlot = calculateMajorEndSlot(classes, targetTotal);
                    sessionLastSlotIdx = (startingSlotIdx + majorEndSlot) % ROTATING_SLOTS.size();
                }
            }


            // DO NOT save to global yet - only stored in sessionOccupiedRooms
            // Will be saved to global when user clicks "Thêm vào kết quả"
            log.info("Generated TKB using {} rooms (temporary, not saved yet)", sessionOccupiedRooms.size());
            log.info("Global occupied rooms: {} (permanent)", globalOccupiedRooms.size());
            log.info("Session lastSlotIdx: {} (temporary, not committed yet)", sessionLastSlotIdx);

            return TKBBatchResponse.builder()
                .items(itemsOut)
                .totalRows(totalRows)
                .lastSlotIdx(sessionLastSlotIdx)  // Return temporary value
                .occupiedRoomsCount(sessionOccupiedRooms.size()) // Return session count
                .build();
                
        } catch (Exception e) {
            log.error("Error in simulateExcelFlowBatch: {}", e.getMessage(), e);
            return TKBBatchResponse.builder()
                .items(Collections.emptyList())
                .error(e.getClass().getSimpleName() + ": " + e.getMessage())
                .build();
        }
    }
    
    /**
     * Commit session occupied rooms to global (permanent storage)
     * Called when user clicks "Thêm vào kết quả"
     */
    public void commitSessionToGlobal() {
        if (sessionOccupiedRooms.isEmpty()) {
            log.warn("No session occupied rooms to commit");
            return;
        }
        
        // Load current global
        Set<Object> globalOccupied = dataLoaderService.loadGlobalOccupiedRooms();
        int beforeCount = globalOccupied.size();
        
        // Add session rooms to global
        globalOccupied.addAll(sessionOccupiedRooms);
        int afterCount = globalOccupied.size();
        int addedCount = afterCount - beforeCount;
        
        // Save to JSON
        dataLoaderService.saveGlobalOccupiedRooms(globalOccupied);
        
        // COMMIT sessionLastSlotIdx to permanent lastSlotIdx
        lastSlotIdx = sessionLastSlotIdx;
        
        // SAVE lastSlotIdx to persistent storage (file)
        dataLoaderService.saveLastSlotIdx(lastSlotIdx);
        
        log.info("Committed {} new rooms to global. Total global rooms: {}", addedCount, afterCount);
        log.info("Committed and saved lastSlotIdx to file: {}", lastSlotIdx);
        
        // Clear session after commit
        sessionOccupiedRooms.clear();
    }

    /**
     * Calculate AH value (exact Python _row_AH logic)
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
     * Normalize slots (exact Python _normalize_slots logic)
     */
    private List<String> normalizeSlots(DataLoaderService.TKBTemplateRow row) {
        // Convert week schedule to string format matching Python
        List<String> weeks = new ArrayList<>();
        List<Integer> weekSchedule = row.getWeekSchedule();
        
        for (Integer week : weekSchedule) {
            if (week != null && week == 1) {
                weeks.add("X");
            } else {
                weeks.add("");
            }
        }
        
        // Ensure exactly 18 weeks
        while (weeks.size() < 18) {
            weeks.add("");
        }
        if (weeks.size() > 18) {
            weeks = weeks.subList(0, 18);
        }
        
        return weeks;
    }

    /**
     * Emit row result (exact Python _emit_row logic) with room info
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
            .phong(maPhong) // Ma phong (room ID) for display
            .AH(AH)
            .AI(aiBefore)
            .AJ(aj)
            .N(keyN)
            .oToAg(weeks)
            .studentYear(payload.getStudent_year())
            .heDacThu(payload.getHe_dac_thu())
            .nganh(payload.getNganh())
            .build();
    }

    /**
     * Calculate major end slot (exact Python logic)
     */
    private int calculateMajorEndSlot(int classes, int targetTotal) {
        if (classes <= 0) return -1;
        
        int lastClassSlotIdx;
        if (targetTotal == 14) {
            // 4 classes per slot
            lastClassSlotIdx = (classes - 1) / 4;
        } else {
            // 2 classes per slot  
            lastClassSlotIdx = (classes - 1) / 2;
        }
        
        return lastClassSlotIdx % ROTATING_SLOTS.size();
    }

    /**
     * Map regular slot index to 60-period slot index
     * When transitioning from regular subjects to 60-period subjects,
     * we need to find the next available pair of consecutive days.
     * 
     * Regular slots: 12 individual day slots
     * 60-period slots: 12 paired-day slots (3 day-pairs × 4 kips)
     * 
     * @param regularSlotIdx current slot index from regular subject scheduling
     * @return appropriate starting slot index for 60-period subject
     */
    private int mapRegularSlotTo60PeriodSlot(int regularSlotIdx) {
        // Calculate which day-pair the regular slot belongs to (every 2 regular slots = 1 pair)
        // Then move to the next pair and start at its first kip
        int pairIndex = (regularSlotIdx / 2) + 1; // Next pair after current
        int slot60Index = (pairIndex * 4) % ROTATING_SLOTS_60.size(); // First kip of that pair
        return slot60Index;
    }

    /**
     * Process regular subjects (non-60-period) with standard logic
     * Extracted from original simulateExcelFlowBatch loop
     */
    private List<TKBRowResult> processRegularSubject(
            TKBRequest tkbRequest,
            List<DataLoaderService.TKBTemplateRow> pool,
            List<RoomData> rooms,
            Set<Object> occupiedRooms,
            int startingSlotIdx,
            int classes,
            int targetTotal) {
        
        List<TKBRowResult> resultRows = new ArrayList<>();
        int idx = 0;
        
        log.info("Processing regular subject: {} classes", classes);
        
        for (int cls = 1; cls <= classes; cls++) {
            // Room assignment: ONE room per class (Python strategy)
            String classRoomCode = null;
            String classRoomMaPhong = null;
            
            // Calculate slot based on starting_slot_idx + class offset (exact Python logic)
            int slotIdx;
            if (targetTotal == 14) {
                // 4 classes per slot for 14-period subjects
                slotIdx = (startingSlotIdx + (cls - 1) / 4) % ROTATING_SLOTS.size();
            } else {
                // 2 classes per slot for other subjects
                slotIdx = (startingSlotIdx + (cls - 1) / 2) % ROTATING_SLOTS.size();
            }

            TimetableSlot targetSlot = ROTATING_SLOTS.get(slotIdx);
            Set<Integer> targetKips = targetSlot.getKipSet();
            
            int ai = targetTotal;
            int guard = 0;
            
            while (ai > 0 && guard < 10000) {
                // Find matching row (exact Python logic)
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
                
                // Fallback if no match found
                if (row == null) {
                    row = pool.get(idx);
                    idx = (idx + 1) % pool.size();
                }
                
                int ah = calculateAH(row);
                if (ah <= 0) {
                    guard++;
                    continue;
                }
                
                // Pick room ONCE per class (on first valid session only)
                if (classRoomCode == null) {
                    Integer tietBd = row.getStartPeriod();
                    Integer rowThu = row.getDayOfWeek();
                    Integer rowKip = row.getKip();
                    
                    // Skip room assignment if tiet_bd = 12
                    if (tietBd != null && tietBd != 12 && rowThu != null && rowKip != null) {
                        // Calculate siso per class for capacity check
                        Integer sisoPerClass;
                        if (tkbRequest.getHe_dac_thu() != null && !tkbRequest.getHe_dac_thu().trim().isEmpty()) {
                            // Special system: siso / solop
                            sisoPerClass = tkbRequest.getSiso() / tkbRequest.getSolop();
                        } else {
                            // Regular system: use siso_mot_lop
                            sisoPerClass = tkbRequest.getSiso_mot_lop();
                        }
                        
                        // Call pickRoom
                        RoomService.RoomPickResult roomResult = roomService.pickRoom(
                            rooms, 
                            sisoPerClass, 
                            occupiedRooms, 
                            rowThu, 
                            rowKip,
                            tkbRequest.getSubject_type(),
                            tkbRequest.getStudent_year(),
                            tkbRequest.getHe_dac_thu(),
                            null // week_schedule can be added later
                        );
                        
                        if (roomResult.hasRoom()) {
                            classRoomCode = roomResult.getRoomCode();
                            classRoomMaPhong = roomResult.getMaPhong();
                            
                            // Mark room as occupied in BOTH session and working set
                            String occupationKey = classRoomCode + "|" + rowThu + "|" + rowKip;
                            occupiedRooms.add(occupationKey); // Working set for this generation
                            sessionOccupiedRooms.add(occupationKey); // Session storage (temporary)
                        }
                    }
                }
                
                // Create result row with room info (exact Python _emit_row logic)
                // For rows with tiet_bd = 12, don't assign room
                Integer currentTietBd = row.getStartPeriod();
                String rowRoomCode = (currentTietBd != null && currentTietBd == 12) ? null : classRoomCode;
                String rowRoomMaPhong = (currentTietBd != null && currentTietBd == 12) ? null : classRoomMaPhong;
                
                TKBRowResult resultRow = emitRow(cls, tkbRequest, row, ai, rowRoomCode, rowRoomMaPhong);
                resultRows.add(resultRow);
                
                // Subtract AH and increment guard - this creates ONE row per iteration
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
     * Process 60-period subject with special paired-day logic
     * Groups data by (thu, kip), pairs consecutive days with same kip
     * Emits 4 rows per class (2 rows × 2 days)
     */
    private List<TKBRowResult> process60PeriodSubject(
            TKBRequest tkbRequest,
            List<DataLoaderService.TKBTemplateRow> pool,
            List<RoomData> rooms,
            Set<Object> occupiedRooms,
            int startingSlotIdx) {
        
        List<TKBRowResult> resultRows = new ArrayList<>();
        
        // Calculate number of classes (exact Python logic)
        int classes = Math.max(1, toInt(tkbRequest.getSolop(), 1));
        log.info("Processing 60-period subject with {} classes", classes);
        
        // Group data by (thu, kip)
        Map<String, List<DataLoaderService.TKBTemplateRow>> groups = pool.stream()
            .collect(Collectors.groupingBy(row -> 
                row.getDayOfWeek() + "_" + row.getKip()
            ));
        
        log.info("Grouped 60-period data into {} groups", groups.size());
        
        // Loop through all classes
        for (int cls = 1; cls <= classes; cls++) {
            log.info("Processing 60-period class {}/{}", cls, classes);
            
            // Get rotating slot for this class
            int slotIdx = (startingSlotIdx + (cls - 1)) % ROTATING_SLOTS_60.size();
            DayPairSlot dayPairSlot = ROTATING_SLOTS_60.get(slotIdx);
            
            // Get the specific kip for this slot
            Integer targetKip = dayPairSlot.getKip();
            
            log.info("Class {} using slot: days {}-{} with kip {}", 
                cls, dayPairSlot.getDay1(), dayPairSlot.getDay2(), targetKip);
            
            // Assign ONE room for this class (both days)
            String classRoomCode = null;
            String classRoomMaPhong = null;
            
            // Process each paired day with the SAME single kip
            for (Integer currentDay : dayPairSlot.getDays()) {
                String groupKey = currentDay + "_" + targetKip;
                List<DataLoaderService.TKBTemplateRow> groupRows = groups.get(groupKey);
                
                if (groupRows == null || groupRows.isEmpty()) {
                    log.warn("No data for day {} kip {}", currentDay, targetKip);
                    continue;
                }
                
                log.info("Processing group {} with {} rows for class {}", groupKey, groupRows.size(), cls);
                
                // Process all rows in this group (should be ~2 rows = 30 periods)
                for (DataLoaderService.TKBTemplateRow row : groupRows) {
                    // Pick room ONCE (on first valid row only)
                    if (classRoomCode == null) {
                        Integer tietBd = row.getStartPeriod();
                        Integer rowThu = row.getDayOfWeek();
                        Integer rowKip = row.getKip();
                        
                        if (tietBd != null && tietBd != 12 && rowThu != null && rowKip != null) {
                            Integer sisoPerClass;
                            if (tkbRequest.getHe_dac_thu() != null && !tkbRequest.getHe_dac_thu().trim().isEmpty()) {
                                sisoPerClass = tkbRequest.getSiso() / tkbRequest.getSolop();
                            } else {
                                sisoPerClass = tkbRequest.getSiso_mot_lop();
                            }
                            
                            RoomService.RoomPickResult roomResult = roomService.pickRoom(
                                rooms, 
                                sisoPerClass, 
                                occupiedRooms, 
                                rowThu, 
                                rowKip,
                                tkbRequest.getSubject_type(),
                                tkbRequest.getStudent_year(),
                                tkbRequest.getHe_dac_thu(),
                                null
                            );
                            
                            if (roomResult.hasRoom()) {
                                classRoomCode = roomResult.getRoomCode();
                                classRoomMaPhong = roomResult.getMaPhong();
                                
                                log.info("Assigned room {} for class {}", classRoomCode, cls);
                            }
                        }
                    }
                    
                    // Mark room as occupied for BOTH days with the same kip
                    if (classRoomCode != null) {
                        for (Integer day : dayPairSlot.getDays()) {
                            String occupationKey = classRoomCode + "|" + day + "|" + targetKip;
                            occupiedRooms.add(occupationKey);
                            sessionOccupiedRooms.add(occupationKey);
                        }
                    }
                    
                    // Emit row with current class number
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
     * Safe integer conversion (exact Python _to_int logic)
     */
    private int toInt(Object value, int defaultValue) {
        if (value == null) return defaultValue;
        try {
            if (value instanceof Number) {
                return ((Number) value).intValue();
            }
            String str = value.toString().trim();
            if (str.isEmpty()) return defaultValue;
            return (int) Double.parseDouble(str);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private int toInt(Object value) {
        return toInt(value, 0);
    }

    /**
     * Reset global state
     */
    public void resetState() {
        lastSlotIdx = -1;
        log.info("Reset TKB scheduling state");
    }
    
    /**
     * Reset both session and global occupied rooms
     * Called when user clicks "Reset phòng đã sử dụng" button
     */
    public void resetOccupiedRooms() {
        // Clear session
        int sessionCount = sessionOccupiedRooms.size();
        sessionOccupiedRooms.clear();
        
        // Clear global storage
        Set<Object> emptySet = new HashSet<>();
        dataLoaderService.saveGlobalOccupiedRooms(emptySet);
        
        // Reset BOTH lastSlotIdx variables
        lastSlotIdx = -1;
        sessionLastSlotIdx = -1;
        
        log.info("Reset occupied rooms - Cleared {} session rooms, global storage, and lastSlotIdx", sessionCount);
    }
    
    /**
     * Get current occupied rooms count (for display)
     */
    public Map<String, Integer> getOccupiedRoomsInfo() {
        Set<Object> globalRooms = dataLoaderService.loadGlobalOccupiedRooms();
        
        Map<String, Integer> info = new HashMap<>();
        info.put("session", sessionOccupiedRooms.size());
        info.put("global", globalRooms.size());
        info.put("total", sessionOccupiedRooms.size() + globalRooms.size());
        
        return info;
    }
}