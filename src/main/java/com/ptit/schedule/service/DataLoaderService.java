package com.ptit.schedule.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ptit.schedule.model.RoomData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@Slf4j
public class DataLoaderService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<TKBTemplateRow> templateData = null;

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
            // Parse basic info: [totalPeriods, dayOfWeek, kip, startPeriod, periodLength, id, ...]
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
                id, weekSchedule, totalUsed
            );

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
        public Integer getTotalPeriods() { return totalPeriods; }
        public Integer getDayOfWeek() { return dayOfWeek; }
        public Integer getKip() { return kip; }
        public Integer getStartPeriod() { return startPeriod; }
        public Integer getPeriodLength() { return periodLength; }
        public String getId() { return id; }
        public List<Integer> getWeekSchedule() { return weekSchedule; }
        public Integer getTotalUsed() { return totalUsed; }
    }
    
    /**
     * Load rooms data from rooms.json
     */
    public List<RoomData> loadRoomsData() {
        try {
            log.info("Loading rooms data from rooms.json...");
            ClassPathResource resource = new ClassPathResource("rooms.json");
            JsonNode root = objectMapper.readTree(resource.getInputStream());
            
            if (!root.isArray()) {
                log.warn("rooms.json is not an array");
                return new ArrayList<>();
            }
            
            List<RoomData> rooms = new ArrayList<>();
            for (JsonNode node : root) {
                RoomData room = RoomData.builder()
                    .phong(node.has("phong") ? node.get("phong").asText() : null)
                    .ma_phong(node.has("ma_phong") ? node.get("ma_phong").asText() : null)
                    .capacity(node.has("capacity") ? node.get("capacity").asInt() : null)
                    .type(node.has("type") ? node.get("type").asText() : "general")
                    .note(node.has("note") ? node.get("note").asText() : "")
                    .build();
                rooms.add(room);
            }
            
            log.info("Loaded {} rooms from rooms.json", rooms.size());
            return rooms;
        } catch (Exception e) {
            log.error("Error loading rooms data", e);
            return new ArrayList<>();
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
     * Load lastSlotIdx from last_slot_idx.json
     * Returns -1 if file doesn't exist or on error
     */
    public int loadLastSlotIdx() {
        try {
            ClassPathResource resource = new ClassPathResource("last_slot_idx.json");
            if (resource.exists()) {
                Map<String, Integer> data = objectMapper.readValue(resource.getInputStream(), 
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Integer>>() {});
                int lastSlotIdx = data.getOrDefault("lastSlotIdx", -1);
                log.info("Loaded lastSlotIdx: {}", lastSlotIdx);
                return lastSlotIdx;
            }
        } catch (Exception e) {
            log.warn("Could not load last_slot_idx.json, using default -1", e);
        }
        return -1;
    }
    
    /**
     * Save lastSlotIdx to last_slot_idx.json
     * Saves to both target/classes and src/main/resources for persistence
     */
    public void saveLastSlotIdx(int lastSlotIdx) {
        try {
            log.info("Saving lastSlotIdx: {} to last_slot_idx.json", lastSlotIdx);
            
            Map<String, Integer> data = new HashMap<>();
            data.put("lastSlotIdx", lastSlotIdx);
            
            // Try to save to both target/classes and src/main/resources
            try {
                // Save to target/classes (for current runtime)
                ClassPathResource resource = new ClassPathResource("last_slot_idx.json");
                String targetPath = resource.getFile().getAbsolutePath();
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(targetPath), data);
                log.info("Saved lastSlotIdx to target: {}", targetPath);
                
                // Also save to src/main/resources (for persistence across rebuilds)
                String projectRoot = System.getProperty("user.dir");
                String srcPath = projectRoot + "/src/main/resources/last_slot_idx.json";
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(srcPath), data);
                log.info("Saved lastSlotIdx to source: {}", srcPath);
                
            } catch (Exception e) {
                // If above fails, just save to working directory as fallback
                String fallbackPath = System.getProperty("user.dir") + "/last_slot_idx.json";
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(new java.io.File(fallbackPath), data);
                log.info("Saved lastSlotIdx to fallback location: {}", fallbackPath);
            }
            
            log.info("Successfully saved lastSlotIdx");
        } catch (Exception e) {
            log.error("Error saving lastSlotIdx", e);
        }
    }
}
