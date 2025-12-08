package com.ptit.schedule.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.schedule.entity.Semester;
import com.ptit.schedule.entity.TKBTemplate;
import com.ptit.schedule.repository.SemesterRepository;
import com.ptit.schedule.repository.TKBTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class DataLoaderService {

    private final TKBTemplateRepository tkbTemplateRepository;
    private final SemesterRepository semesterRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, List<TKBTemplateRow>> templateDataCache = new HashMap<>();

    /**
     * Load template data for specific semester from database
     * @param semester Học kỳ (VD: "HK1 2024-2025")
     * @return List of template rows
     */
    public List<TKBTemplateRow> loadTemplateData(String semester) {
        // Check cache
        if (templateDataCache.containsKey(semester)) {
            log.info("📦 Returning cached template data for {}: {} rows", semester, templateDataCache.get(semester).size());
            return templateDataCache.get(semester);
        }

        try {
            // Parse semester string to extract semesterName and academicYear
            // VD: "HK1 2024-2025" -> semesterName="HK1", academicYear="2024-2025"
            String[] parts = parseSemester(semester);
            String semesterName = parts[0];
            String academicYear = parts[1];
            
            log.info("🔍 Loading template data from database for {} {}...", semesterName, academicYear);
            
            // Find or create semester entity
            Semester semesterEntity = findOrCreateSemester(semesterName, academicYear);
            
            // Query database
            List<TKBTemplate> entities = tkbTemplateRepository.findBySemesterOrderByRowOrderAsc(semesterEntity);
            
            if (entities.isEmpty()) {
                log.warn("⚠️ No template data found in database for {} {}", semesterName, academicYear);
                return new ArrayList<>();
            }

            log.info("✅ Found {} templates in database", entities.size());

            // Convert entities to TKBTemplateRow
            List<TKBTemplateRow> templateData = new ArrayList<>();
            for (TKBTemplate entity : entities) {
                TKBTemplateRow row = convertEntityToRow(entity);
                if (row != null) {
                    templateData.add(row);
                }
            }

            // Cache the result
            templateDataCache.put(semester, templateData);
            
            // Debug: Log template distribution by day and kip
            Map<String, Long> distribution = templateData.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    row -> "T" + row.getDayOfWeek() + "-K" + row.getKip(),
                    java.util.stream.Collectors.counting()
                ));
            log.info("✅ Loaded and cached {} template rows for {}", templateData.size(), semester);
            log.info("📊 Template distribution: {}", distribution);
            return templateData;

        } catch (Exception e) {
            log.error("❌ Error loading template data from database for {}: {}", semester, e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Parse semester string to extract semesterName and academicYear
     * VD: "HK1 2024-2025" -> ["HK1", "2024-2025"]
     *     "Học kỳ 2 2024-2025" -> ["HK2", "2024-2025"]
     */
    private String[] parseSemester(String semester) {
        if (semester == null || semester.isEmpty()) {
            return new String[]{"", ""};
        }
        
        // Normalize first
        String normalized = normalizeSemester(semester);
        // Split by underscore: "HK1_2024-2025" -> ["HK1", "2024-2025"]
        String[] parts = normalized.split("_", 2);
        
        if (parts.length == 2) {
            return new String[]{parts[0], parts[1]};
        } else if (parts.length == 1) {
            return new String[]{parts[0], ""};
        }
        return new String[]{"", ""};
    }

    /**
     * Generate filename from semester
     * Normalize semester name to standard format
     * VD: "Học kỳ 1 2024-2025" -> "real_HK1_2024-2025.json"
     */
    private String generateFilename(String semester) {
        if (semester == null || semester.isEmpty()) {
            return "real.json"; // fallback to default
        }
        
        // Normalize semester name
        String normalized = normalizeSemester(semester);
        return "real_" + normalized + ".json";
    }
    
    /**
     * Normalize semester name to avoid Unicode issues
     * VD: "Học kỳ 1 2024-2025" -> "HK1_2024-2025"
     *     "Học kỳ 2 2024-2025" -> "HK2_2024-2025"
     *     "HK1 2024-2025" -> "HK1_2024-2025"
     *     "aaa 2024-2025" -> "aaa_2024-2025"
     *     "aaaa - 2024-2025" -> "aaaa_2024-2025"
     */
    private String normalizeSemester(String semester) {
        if (semester == null) return "";
        
        // Remove extra spaces and normalize
        semester = semester.trim().replaceAll("\\s+", " ");
        
        // Convert "Học kỳ X" to "HKX"
        semester = semester.replaceAll("(?i)h[oọôớờ][cçć]\\s*k[yỳýỵỹỷ]\\s*(\\d)", "HK$1");
        
        // Remove standalone dash with spaces (VD: "aaaa - 2024-2025" -> "aaaa 2024-2025")
        semester = semester.replaceAll("\\s+-\\s+", " ");
        
        // Replace spaces with underscores (nhưng giữ nguyên dấu - trong năm học)
        // VD: "HK1 2024-2025" -> "HK1_2024-2025"
        semester = semester.replace(" ", "_");
        
        return semester;
    }

    /**
     * Find Semester entity (không tự động tạo mới)
     */
    private Semester findOrCreateSemester(String semesterName, String academicYear) {
        return semesterRepository.findBySemesterNameAndAcademicYear(semesterName, academicYear)
            .orElseThrow(() -> new RuntimeException(
                "Không tìm thấy học kỳ: " + semesterName + " " + academicYear + 
                ". Vui lòng tạo học kỳ trước khi import template."));
    }

    /**
     * Convert TKBTemplate entity to TKBTemplateRow
     */
    private TKBTemplateRow convertEntityToRow(TKBTemplate entity) {
        try {
            // Parse weekSchedule JSON string to List<Integer>
            List<Integer> weekSchedule = objectMapper.readValue(
                entity.getWeekSchedule(), 
                objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class)
            );
            
            return new TKBTemplateRow(
                entity.getId(), // Add database ID
                entity.getTotalPeriods(),
                entity.getDayOfWeek(),
                entity.getKip(),
                entity.getStartPeriod(),
                entity.getPeriodLength(),
                entity.getTemplateId(),
                weekSchedule,
                entity.getTotalUsed()
            );
        } catch (Exception e) {
            log.error("Error converting entity to row: {}", e.getMessage());
            return null;
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
                    null, // No database ID for parsed rows
                    totalPeriods, dayOfWeek, kip, startPeriod, periodLength,
                    id, weekSchedule, totalUsed);

        } catch (Exception e) {
            log.warn("Error parsing template row: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Load template data without semester (deprecated, uses default)
     * @deprecated Use loadTemplateData(String semester) instead
     */
    @Deprecated
    public List<TKBTemplateRow> loadTemplateData() {
        log.warn("⚠️ loadTemplateData() called without semester parameter. Using default 'real.json'");
        return loadTemplateData(""); // Empty string will use real.json as fallback
    }
    
    public List<TKBTemplateRow> getTemplateByPeriods(int totalPeriods) {
        log.warn("⚠️ getTemplateByPeriods() called without semester. Using default.");
        List<TKBTemplateRow> allData = loadTemplateData();
        return allData.stream()
                .filter(row -> row.getTotalPeriods() == totalPeriods)
                .toList();
    }
    
    public List<TKBTemplateRow> getTemplateByPeriods(int totalPeriods, String semester) {
        List<TKBTemplateRow> allData = loadTemplateData(semester);
        return allData.stream()
                .filter(row -> row.getTotalPeriods() == totalPeriods)
                .toList();
    }

    public static class TKBTemplateRow {
        private final Long databaseId; // ID từ database
        private final Integer totalPeriods;
        private final Integer dayOfWeek;
        private final Integer kip;
        private final Integer startPeriod;
        private final Integer periodLength;
        private final String id;
        private final List<Integer> weekSchedule;
        private final Integer totalUsed;

        public TKBTemplateRow(Long databaseId, Integer totalPeriods, Integer dayOfWeek, Integer kip,
                Integer startPeriod, Integer periodLength, String id,
                List<Integer> weekSchedule, Integer totalUsed) {
            this.databaseId = databaseId;
            this.totalPeriods = totalPeriods;
            this.dayOfWeek = dayOfWeek;
            this.kip = kip;
            this.startPeriod = startPeriod;
            this.periodLength = periodLength;
            this.id = id;
            this.weekSchedule = weekSchedule;
            this.totalUsed = totalUsed;
        }

        public Long getDatabaseId() {
            return databaseId;
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
     * Load global occupied rooms from global_occupied_rooms.json
     */
    public Set<Object> loadGlobalOccupiedRooms() {
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

            log.info("Loaded {} occupied room entries", occupiedRooms.size());
            return occupiedRooms;
        } catch (Exception e) {
            log.error("Error loading global occupied rooms", e);
            return new HashSet<>();
        }
    }

    /**
     * Save global occupied rooms to global_occupied_rooms.json
     * Saves to both target/classes and src/main/resources for persistence
     */
    public void saveGlobalOccupiedRooms(Set<Object> occupiedRooms) {
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

            log.info("Successfully saved global occupied rooms");
        } catch (Exception e) {
            log.error("Error saving global occupied rooms", e);
        }
    }





    /**
     * Import data from Excel file and save to JSON file with semester name
     * @param file Excel file
     * @param semester Học kỳ (VD: "HK1 2024-2025")
     * @return Filename of saved JSON
     */
    public String importDataFromExcel(org.springframework.web.multipart.MultipartFile file, String semester) {
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
            org.apache.poi.ss.usermodel.FormulaEvaluator formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
            
            // Read all rows from Excel
            for (org.apache.poi.ss.usermodel.Row row : sheet) {
                if (row == null) {
                    continue;
                }
                
                List<Object> rowData = new ArrayList<>();
                // Get the last column number from the row to include all cells
                int lastColumn = Math.max(row.getLastCellNum(), 0);
                
                for (int cn = 0; cn < lastColumn; cn++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(cn, org.apache.poi.ss.usermodel.Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    
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

            // Parse semester to get semesterName and academicYear
            String[] parts = parseSemester(semester);
            String semesterName = parts[0];
            String academicYear = parts[1];
            
            log.info("Saving templates to database for {} {}...", semesterName, academicYear);
            
            // Find or create semester entity
            Semester semesterEntity = findOrCreateSemester(semesterName, academicYear);
            
            // Convert Excel data to entities and save to database
            List<TKBTemplate> entities = new ArrayList<>();
            int rowOrder = 0;
            
            // Skip header row (index 0)
            for (int i = 1; i < dataArray.size(); i++) {
                List<Object> row = dataArray.get(i);
                if (row.size() >= 24) {
                    try {
                        // Parse row data
                        int totalPeriods = (Integer) row.get(0);
                        int dayOfWeek = (Integer) row.get(1);
                        int kip = (Integer) row.get(2);
                        int startPeriod = (Integer) row.get(3);
                        int periodLength = (Integer) row.get(4);
                        String templateId = row.get(5).toString();
                        
                        // Parse week schedule (columns 6-23)
                        List<Integer> weekSchedule = new ArrayList<>();
                        for (int j = 6; j < 24; j++) {
                            Object weekCell = row.get(j);
                            if (weekCell != null && ("x".equalsIgnoreCase(weekCell.toString()) || "X".equalsIgnoreCase(weekCell.toString()))) {
                                weekSchedule.add(1);
                            } else {
                                weekSchedule.add(0);
                            }
                        }
                        
                        // Calculate totalUsed
                        int totalUsed = weekSchedule.stream().mapToInt(Integer::intValue).sum() * periodLength;
                        
                        // Convert weekSchedule to JSON string
                        String weekScheduleJson = objectMapper.writeValueAsString(weekSchedule);
                        
                        // Create entity
                        TKBTemplate entity = TKBTemplate.builder()
                            .templateId(templateId)
                            .totalPeriods(totalPeriods)
                            .dayOfWeek(dayOfWeek)
                            .kip(kip)
                            .startPeriod(startPeriod)
                            .periodLength(periodLength)
                            .weekSchedule(weekScheduleJson)
                            .totalUsed(totalUsed)
                            .semester(semesterEntity)
                            .rowOrder(rowOrder++)
                            .build();
                        
                        entities.add(entity);
                    } catch (Exception e) {
                        log.warn("⚠️ Failed to parse row {}: {}", i, e.getMessage());
                    }
                }
            }
            
            // Delete old templates for this semester before inserting new ones
            try {
                log.info("🗑️ Deleting old templates for {} {}...", semesterName, academicYear);
                List<TKBTemplate> oldTemplates = tkbTemplateRepository.findBySemesterOrderByRowOrderAsc(semesterEntity);
                if (!oldTemplates.isEmpty()) {
                    tkbTemplateRepository.deleteAll(oldTemplates);
                    log.info("✅ Deleted {} old templates", oldTemplates.size());
                }
            } catch (Exception e) {
                log.warn("⚠️ Failed to delete old templates: {}", e.getMessage());
            }
            
            // Save all entities to database
            try {
                List<TKBTemplate> savedEntities = tkbTemplateRepository.saveAll(entities);
                log.info("✅ Saved {} templates to database", savedEntities.size());
                
                // Clear ALL cache entries for this semester (multiple possible keys)
                String semesterKey = semesterName + " " + academicYear; // VD: "HK2 2024-2025"
                templateDataCache.remove(semester); // Original key from parameter
                templateDataCache.remove(semesterKey); // Normalized key
                
                // Clear all cache entries to be safe
                templateDataCache.clear();
                log.info("🗑️ Cleared all template cache");
                
                // Debug: Log new IDs from saved entities
                List<Long> newIds = savedEntities.stream().map(TKBTemplate::getId).limit(5).toList();
                log.info("📝 New template IDs (first 5): {}", newIds);
                
                log.info("✅ Successfully imported {} templates for {} {}", savedEntities.size(), semesterName, academicYear);
                return semesterName + " " + academicYear;
                
            } catch (Exception e) {
                log.error("❌ Failed to save to database: {}", e.getMessage());
                throw new RuntimeException("Failed to save to database: " + e.getMessage(), e);
            }

        } catch (Exception e) {
            log.error("❌ Error importing data from Excel", e);
            throw new RuntimeException("Failed to import data from Excel: " + e.getMessage(), e);
        }
    }
}
