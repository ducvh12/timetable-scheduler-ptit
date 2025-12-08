package com.ptit.schedule.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.schedule.entity.Room;
import com.ptit.schedule.entity.RoomOccupancy;
import com.ptit.schedule.entity.Semester;
import com.ptit.schedule.repository.RoomOccupancyRepository;
import com.ptit.schedule.repository.RoomRepository;
import com.ptit.schedule.repository.SemesterRepository;
import com.ptit.schedule.utils.RoomOccupancyUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataLoaderService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RoomOccupancyRepository roomOccupancyRepository;
    private final RoomRepository roomRepository;
    private final SemesterRepository semesterRepository;

    private List<TKBTemplateRow> templateData = null;
    private Long currentSemesterId = null; // Current active semester for room operations

    public List<TKBTemplateRow> loadTemplateData() {
        if (templateData != null) {
            log.info("Returning cached template data: {} rows", templateData.size());
            return templateData;
        }

        try {
            log.info("Loading template data from real.json...");
            ClassPathResource resource = new ClassPathResource("real.json");
            JsonNode root = objectMapper.readTree(resource.getInputStream());
            JsonNode dataArray = root.get("Data");

            if (dataArray == null) {
                log.warn("No 'Data' array found in real.json");
                templateData = new ArrayList<>();
                return templateData;
            }

            log.info("Found Data array with {} elements", dataArray.size());

            templateData = new ArrayList<>();

            // Skip header row (index 0)
            for (int i = 1; i < dataArray.size(); i++) {
                JsonNode row = dataArray.get(i);
                if (row.isArray() && row.size() >= 24) {
                    TKBTemplateRow templateRow = parseTemplateRow(row);
                    if (templateRow != null) {
                        templateData.add(templateRow);
                    }
                }
            }

            log.info("Loaded {} template rows from real.json", templateData.size());
            return templateData;

        } catch (Exception e) {
            log.error("Error loading template data from real.json: {}", e.getMessage(), e);
            templateData = new ArrayList<>();
            return templateData;
        }
    }

    private TKBTemplateRow parseTemplateRow(JsonNode row) {
        try {
            // Parse basic info: [totalPeriods, dayOfWeek, kip, startPeriod, periodLength,
            // id, ...]
            int totalPeriods = row.get(0).asInt();
            int dayOfWeek = row.get(1).asInt();
            int kip = row.get(2).asInt();
            int startPeriod = row.get(3).asInt();
            int periodLength = row.get(4).asInt();
            String id = row.get(5).asText();

            // Parse week schedule (columns 6-23, weeks 1-18) - Python logic
            List<Integer> weekSchedule = new ArrayList<>();
            for (int i = 6; i < 24; i++) {
                JsonNode weekNode = row.get(i);
                if (weekNode.isTextual() && ("x".equals(weekNode.asText()) || "X".equals(weekNode.asText()))) {
                    weekSchedule.add(1);
                } else {
                    weekSchedule.add(0);
                }
            }

            // Calculate total used periods
            int totalUsed = weekSchedule.stream().mapToInt(Integer::intValue).sum() * periodLength;

            return new TKBTemplateRow(
                    totalPeriods, dayOfWeek, kip, startPeriod, periodLength,
                    id, weekSchedule, totalUsed);

        } catch (Exception e) {
            log.warn("Error parsing template row: {}", e.getMessage());
            return null;
        }
    }

    public List<TKBTemplateRow> getTemplateByPeriods(int totalPeriods) {
        List<TKBTemplateRow> allData = loadTemplateData();
        return allData.stream()
                .filter(row -> row.getTotalPeriods() == totalPeriods)
                .toList();
    }

    public static class TKBTemplateRow {
        private final Integer totalPeriods;
        private final Integer dayOfWeek;
        private final Integer kip;
        private final Integer startPeriod;
        private final Integer periodLength;
        private final String id;
        private final List<Integer> weekSchedule;
        private final Integer totalUsed;

        public TKBTemplateRow(Integer totalPeriods, Integer dayOfWeek, Integer kip,
                Integer startPeriod, Integer periodLength, String id,
                List<Integer> weekSchedule, Integer totalUsed) {
            this.totalPeriods = totalPeriods;
            this.dayOfWeek = dayOfWeek;
            this.kip = kip;
            this.startPeriod = startPeriod;
            this.periodLength = periodLength;
            this.id = id;
            this.weekSchedule = weekSchedule;
            this.totalUsed = totalUsed;
        }

        // Getters
        public Integer getTotalPeriods() {
            return totalPeriods;
        }

        public Integer getDayOfWeek() {
            return dayOfWeek;
        }

        public Integer getKip() {
            return kip;
        }

        public Integer getStartPeriod() {
            return startPeriod;
        }

        public Integer getPeriodLength() {
            return periodLength;
        }

        public String getId() {
            return id;
        }

        public List<Integer> getWeekSchedule() {
            return weekSchedule;
        }

        public Integer getTotalUsed() {
            return totalUsed;
        }
    }

    /**
     * Set current active semester for room operations
     * 
     * @param semesterId Semester ID
     */
    public void setCurrentSemesterId(Long semesterId) {
        this.currentSemesterId = semesterId;
        log.info("Current semester ID set to: {}", semesterId);
    }

    /**
     * Get current active semester ID
     * 
     * @return Current semester ID or null if not set
     */
    public Long getCurrentSemesterId() {
        return this.currentSemesterId;
    }

    /**
     * Load global occupied rooms from database for current semester
     * Returns Set<Object> for backward compatibility with existing code
     * Each entry format: "404-A2|5|1" (roomCode|dayOfWeek|period)
     */
    public Set<Object> loadGlobalOccupiedRooms() {
        try {
            if (currentSemesterId == null) {
                log.warn("Current semester ID not set, attempting to load from JSON as fallback");
                return loadGlobalOccupiedRoomsFromJson();
            }

            log.info("Loading occupied rooms from database for semester ID: {}", currentSemesterId);
            Set<String> occupiedKeys = roomOccupancyRepository.findOccupiedKeysBySemesterId(currentSemesterId);

            // Convert Set<String> to Set<Object> for backward compatibility
            Set<Object> result = new HashSet<>(occupiedKeys);
            log.info("Loaded {} occupied room entries from database", result.size());
            return result;
        } catch (Exception e) {
            log.error("Error loading occupied rooms from database, falling back to JSON", e);
            return loadGlobalOccupiedRoomsFromJson();
        }
    }

    /**
     * Load global occupied rooms from global_occupied_rooms.json (legacy/fallback
     * method)
     * 
     * @deprecated Use database method instead
     */
    @Deprecated
    private Set<Object> loadGlobalOccupiedRoomsFromJson() {
        try {
            log.info("Loading global occupied rooms from global_occupied_rooms.json...");
            ClassPathResource resource = new ClassPathResource("global_occupied_rooms.json");

            if (!resource.exists()) {
                log.info("global_occupied_rooms.json not found, returning empty set");
                return new HashSet<>();
            }

            JsonNode root = objectMapper.readTree(resource.getInputStream());

            if (!root.isArray()) {
                log.warn("global_occupied_rooms.json is not an array");
                return new HashSet<>();
            }

            Set<Object> occupiedRooms = new HashSet<>();
            for (JsonNode node : root) {
                if (node.isTextual()) {
                    occupiedRooms.add(node.asText());
                }
            }

            log.info("Loaded {} occupied room entries from JSON", occupiedRooms.size());
            return occupiedRooms;
        } catch (Exception e) {
            log.error("Error loading global occupied rooms from JSON", e);
            return new HashSet<>();
        }
    }

    /**
     * Save global occupied rooms to database for current semester
     * Clears existing occupancies and saves new ones
     * 
     * @param occupiedRooms Set of room occupancy keys (format: "404-A2|5|1")
     */
    @Transactional
    public void saveGlobalOccupiedRooms(Set<Object> occupiedRooms) {
        try {
            if (currentSemesterId == null) {
                log.warn("Current semester ID not set, falling back to JSON save");
                saveGlobalOccupiedRoomsToJson(occupiedRooms);
                return;
            }

            log.info("Saving {} occupied room entries to database for semester ID: {}",
                    occupiedRooms.size(), currentSemesterId);

            // Get semester entity
            Semester semester = semesterRepository.findById(Objects.requireNonNull(currentSemesterId))
                    .orElseThrow(() -> new RuntimeException("Semester not found: " + currentSemesterId));

            // Delete existing occupancies for this semester
            roomOccupancyRepository.deleteBySemesterId(currentSemesterId);
            log.info("Cleared existing occupancies for semester ID: {}", currentSemesterId);

            // Convert and save new occupancies
            List<RoomOccupancy> newOccupancies = new ArrayList<>();
            Map<String, Room> roomCache = new HashMap<>(); // Cache to avoid repeated DB queries

            for (Object obj : occupiedRooms) {
                if (obj == null)
                    continue;

                String uniqueKey = obj.toString();
                if (!RoomOccupancyUtils.isValidUniqueKey(uniqueKey)) {
                    log.warn("Invalid unique key format: {}", uniqueKey);
                    continue;
                }

                // Parse unique key: "404-A2|5|1"
                String roomCode = RoomOccupancyUtils.extractRoomCode(uniqueKey);
                Integer dayOfWeek = RoomOccupancyUtils.extractDayOfWeek(uniqueKey);
                Integer period = RoomOccupancyUtils.extractPeriod(uniqueKey);

                if (roomCode == null || dayOfWeek == null || period == null) {
                    log.warn("Failed to parse unique key: {}", uniqueKey);
                    continue;
                }

                // Parse room code: "404-A2"
                String roomName = RoomOccupancyUtils.extractRoomName(roomCode);
                String building = RoomOccupancyUtils.extractBuilding(roomCode);

                if (roomName == null || building == null) {
                    log.warn("Failed to parse room code: {}", roomCode);
                    continue;
                }

                // Get or cache room
                Room room = roomCache.get(roomCode);
                if (room == null) {
                    room = roomRepository.findByNameAndBuilding(roomName, building)
                            .orElse(null);
                    if (room == null) {
                        log.warn("Room not found: {} in building {}", roomName, building);
                        continue;
                    }
                    roomCache.put(roomCode, room);
                }

                // Create RoomOccupancy entity
                RoomOccupancy occupancy = RoomOccupancy.builder()
                        .room(room)
                        .semester(semester)
                        .dayOfWeek(dayOfWeek)
                        .period(period)
                        .uniqueKey(uniqueKey)
                        .build();

                newOccupancies.add(occupancy);
            }

            // Batch save
            if (!newOccupancies.isEmpty()) {
                roomOccupancyRepository.saveAll(newOccupancies);
                log.info("Successfully saved {} room occupancies to database", newOccupancies.size());
            } else {
                log.warn("No valid occupancies to save");
            }

        } catch (Exception e) {
            log.error("Error saving occupied rooms to database, falling back to JSON", e);
            saveGlobalOccupiedRoomsToJson(occupiedRooms);
        }
    }

    /**
     * Save global occupied rooms to global_occupied_rooms.json (legacy/fallback
     * method)
     * 
     * @deprecated Use database method instead
     */
    @Deprecated
    private void saveGlobalOccupiedRoomsToJson(Set<Object> occupiedRooms) {
        try {
            log.info("Saving {} occupied room entries to global_occupied_rooms.json", occupiedRooms.size());

            // Convert set to list for JSON array
            List<String> occupiedList = new ArrayList<>();
            for (Object obj : occupiedRooms) {
                if (obj != null) {
                    occupiedList.add(obj.toString());
                }
            }

            // Try to save to both target/classes and src/main/resources
            try {
                // Save to target/classes (for current runtime)
                ClassPathResource resource = new ClassPathResource("global_occupied_rooms.json");
                String targetPath = resource.getFile().getAbsolutePath();
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(targetPath), occupiedList);
                log.info("Saved to target: {}", targetPath);

                // Also save to src/main/resources (for persistence across rebuilds)
                String projectRoot = System.getProperty("user.dir");
                String srcPath = projectRoot + "/src/main/resources/global_occupied_rooms.json";
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(srcPath), occupiedList);
                log.info("Saved to source: {}", srcPath);

            } catch (Exception e) {
                // If above fails, just save to working directory as fallback
                String fallbackPath = System.getProperty("user.dir") + "/global_occupied_rooms.json";
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(fallbackPath), occupiedList);
                log.info("Saved to fallback location: {}", fallbackPath);
            }

            log.info("Successfully saved global occupied rooms to JSON");
        } catch (Exception e) {
            log.error("Error saving global occupied rooms to JSON", e);
        }
    }

    /**
     * Import data from Excel file and overwrite real.json
     */
    public void importDataFromExcel(org.springframework.web.multipart.MultipartFile file) {
        try {
            log.info("Importing data from Excel file: {}", file.getOriginalFilename());

            // Parse Excel file using Apache POI
            org.apache.poi.ss.usermodel.Workbook workbook;
            String filename = file.getOriginalFilename();
            if (filename != null && filename.endsWith(".xlsx")) {
                workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream());
            } else {
                workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook(file.getInputStream());
            }

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            List<List<Object>> dataArray = new ArrayList<>();

            // Create FormulaEvaluator to evaluate formulas
            org.apache.poi.ss.usermodel.FormulaEvaluator formulaEvaluator = workbook.getCreationHelper()
                    .createFormulaEvaluator();

            // Read all rows from Excel
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                if (row == null) {
                    continue;
                }

                List<Object> rowData = new ArrayList<>();
                // Get the last column number from the row to include all cells
                int lastColumn = Math.max(row.getLastCellNum(), 0);

                for (int cn = 0; cn < lastColumn; cn++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(cn,
                            org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);

                    if (cell == null) {
                        rowData.add(null);
                        continue;
                    }

                    // Get the cell type, evaluating formulas to their result type
                    org.apache.poi.ss.usermodel.CellType cellType = cell.getCellType();

                    // If it's a formula, evaluate it to get the result type
                    if (cellType == org.apache.poi.ss.usermodel.CellType.FORMULA) {
                        cellType = cell.getCachedFormulaResultType();
                    }

                    switch (cellType) {
                        case STRING:
                            rowData.add(cell.getStringCellValue());
                            break;
                        case NUMERIC:
                            if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                                rowData.add(cell.getDateCellValue().toString());
                            } else {
                                // Check if it's an integer
                                double numValue = cell.getNumericCellValue();
                                if (numValue == (int) numValue) {
                                    rowData.add((int) numValue);
                                } else {
                                    rowData.add(numValue);
                                }
                            }
                            break;
                        case BOOLEAN:
                            rowData.add(cell.getBooleanCellValue());
                            break;
                        case BLANK:
                            rowData.add(null);
                            break;
                        case FORMULA:
                            // This should not happen as we handle FORMULA above
                            // But as a fallback, try to evaluate the formula
                            try {
                                org.apache.poi.ss.usermodel.CellValue cellValue = formulaEvaluator.evaluate(cell);
                                switch (cellValue.getCellType()) {
                                    case STRING:
                                        rowData.add(cellValue.getStringValue());
                                        break;
                                    case NUMERIC:
                                        double evalNumValue = cellValue.getNumberValue();
                                        if (evalNumValue == (int) evalNumValue) {
                                            rowData.add((int) evalNumValue);
                                        } else {
                                            rowData.add(evalNumValue);
                                        }
                                        break;
                                    case BOOLEAN:
                                        rowData.add(cellValue.getBooleanValue());
                                        break;
                                    default:
                                        rowData.add(null);
                                }
                            } catch (Exception e) {
                                log.warn("Failed to evaluate formula in cell, using null", e);
                                rowData.add(null);
                            }
                            break;
                        default:
                            rowData.add(null);
                    }
                }
                dataArray.add(rowData);
            }

            workbook.close();

            log.info("Parsed {} rows from Excel", dataArray.size());

            // Create JSON structure
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("Data", dataArray);

            // Save to real.json (both target and source)
            try {
                // Save to target/classes (for current runtime)
                ClassPathResource resource = new ClassPathResource("real.json");
                String targetPath = resource.getFile().getAbsolutePath();
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(targetPath), jsonData);
                log.info("Saved imported data to target: {}", targetPath);

                // Also save to src/main/resources (for persistence)
                String projectRoot = System.getProperty("user.dir");
                String srcPath = projectRoot + "/src/main/resources/real.json";
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(srcPath), jsonData);
                log.info("Saved imported data to source: {}", srcPath);

            } catch (Exception e) {
                // Fallback: save to working directory
                String fallbackPath = System.getProperty("user.dir") + "/real.json";
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(fallbackPath), jsonData);
                log.info("Saved imported data to fallback location: {}", fallbackPath);
            }

            // Clear cached template data and immediately reload from the new file
            templateData = null;
            log.info("Cache cleared, reloading template data from updated real.json...");

            // Force reload the new data immediately
            loadTemplateData();
            log.info("Successfully imported data from Excel, updated real.json, and reloaded {} rows",
                    templateData.size());

        } catch (Exception e) {
            log.error("Error importing data from Excel", e);
            throw new RuntimeException("Failed to import data from Excel: " + e.getMessage(), e);
        }
    }
}
