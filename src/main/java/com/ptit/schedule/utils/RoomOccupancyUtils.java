package com.ptit.schedule.utils;

import com.ptit.schedule.entity.Room;

/**
 * Utility class for building room occupancy unique keys
 * Compatible with old global_occupied_rooms.json format
 */
public class RoomOccupancyUtils {

    /**
     * Build unique key from room code (name-building format) and time slot
     * Format: "404-A2|5|1" (roomCode|dayOfWeek|period)
     * 
     * @param roomCode  Room code in format "name-building" (e.g., "404-A2")
     * @param dayOfWeek Day of week (2-7)
     * @param period    Period (1-6)
     * @return Unique key string
     */
    public static String buildUniqueKey(String roomCode, Integer dayOfWeek, Integer period) {
        return String.format("%s|%d|%d", roomCode, dayOfWeek, period);
    }

    /**
     * Build unique key from Room entity and time slot
     * 
     * @param room      Room entity
     * @param dayOfWeek Day of week (2-7)
     * @param period    Period (1-6)
     * @return Unique key string
     */
    public static String buildUniqueKey(Room room, Integer dayOfWeek, Integer period) {
        String roomCode = buildRoomCode(room);
        return buildUniqueKey(roomCode, dayOfWeek, period);
    }

    /**
     * Build room code from Room entity
     * Format: "name-building" (e.g., "404-A2")
     * 
     * @param room Room entity
     * @return Room code string
     */
    public static String buildRoomCode(Room room) {
        return String.format("%s-%s", room.getName(), room.getBuilding());
    }

    /**
     * Build room code from room name and building
     * Format: "name-building" (e.g., "404-A2")
     * 
     * @param name     Room name
     * @param building Building code
     * @return Room code string
     */
    public static String buildRoomCode(String name, String building) {
        return String.format("%s-%s", name, building);
    }

    /**
     * Parse unique key to extract components
     * Format: "404-A2|5|1" → [roomCode, dayOfWeek, period]
     * 
     * @param uniqueKey Unique key string
     * @return Array with [roomCode, dayOfWeek, period] or null if invalid
     */
    public static String[] parseUniqueKey(String uniqueKey) {
        if (uniqueKey == null || uniqueKey.isEmpty()) {
            return null;
        }

        String[] parts = uniqueKey.split("\\|");
        if (parts.length == 3) {
            return parts; // [roomCode, dayOfWeek, period]
        }

        return null;
    }

    /**
     * Extract room code from unique key
     * Format: "404-A2|5|1" → "404-A2"
     * 
     * @param uniqueKey Unique key string
     * @return Room code or null if invalid
     */
    public static String extractRoomCode(String uniqueKey) {
        String[] parts = parseUniqueKey(uniqueKey);
        return parts != null ? parts[0] : null;
    }

    /**
     * Extract day of week from unique key
     * Format: "404-A2|5|1" → 5
     * 
     * @param uniqueKey Unique key string
     * @return Day of week or null if invalid
     */
    public static Integer extractDayOfWeek(String uniqueKey) {
        String[] parts = parseUniqueKey(uniqueKey);
        if (parts != null) {
            try {
                return Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Extract period from unique key
     * Format: "404-A2|5|1" → 1
     * 
     * @param uniqueKey Unique key string
     * @return Period or null if invalid
     */
    public static Integer extractPeriod(String uniqueKey) {
        String[] parts = parseUniqueKey(uniqueKey);
        if (parts != null) {
            try {
                return Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Parse room code to extract name and building
     * Format: "404-A2" → ["404", "A2"]
     * 
     * @param roomCode Room code string
     * @return Array with [name, building] or null if invalid
     */
    public static String[] parseRoomCode(String roomCode) {
        if (roomCode == null || roomCode.isEmpty()) {
            return null;
        }

        String[] parts = roomCode.split("-");
        if (parts.length == 2) {
            return parts; // [name, building]
        }

        return null;
    }

    /**
     * Extract room name from room code
     * Format: "404-A2" → "404"
     * 
     * @param roomCode Room code string
     * @return Room name or null if invalid
     */
    public static String extractRoomName(String roomCode) {
        String[] parts = parseRoomCode(roomCode);
        return parts != null ? parts[0] : null;
    }

    /**
     * Extract building from room code
     * Format: "404-A2" → "A2"
     * 
     * @param roomCode Room code string
     * @return Building code or null if invalid
     */
    public static String extractBuilding(String roomCode) {
        String[] parts = parseRoomCode(roomCode);
        return parts != null ? parts[1] : null;
    }

    /**
     * Validate unique key format
     * 
     * @param uniqueKey Unique key to validate
     * @return true if valid format, false otherwise
     */
    public static boolean isValidUniqueKey(String uniqueKey) {
        String[] parts = parseUniqueKey(uniqueKey);
        if (parts == null) {
            return false;
        }

        try {
            int dayOfWeek = Integer.parseInt(parts[1]);
            int period = Integer.parseInt(parts[2]);

            // Validate ranges
            return dayOfWeek >= 2 && dayOfWeek <= 7 && period >= 1 && period <= 6;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
