package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.ConflictResult;
import com.ptit.schedule.dto.ScheduleEntry;
import com.ptit.schedule.service.ScheduleConflictDetectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ScheduleConflictDetectionServiceImpl implements ScheduleConflictDetectionService {

    // Helper class to pair ScheduleEntry with its specific TimeSlot
    private static class ScheduleEntryWithTimeSlot {
        final ScheduleEntry entry;
        final ScheduleEntry.TimeSlot timeSlot;
        
        ScheduleEntryWithTimeSlot(ScheduleEntry entry, ScheduleEntry.TimeSlot timeSlot) {
            this.entry = entry;
            this.timeSlot = timeSlot;
        }
    }

    @Override
    public ConflictResult detectConflicts(List<ScheduleEntry> scheduleEntries) {
        List<ConflictResult.RoomConflict> roomConflicts = detectRoomConflicts(scheduleEntries);
        List<ConflictResult.TeacherConflict> teacherConflicts = detectTeacherConflicts(scheduleEntries);

        // Group conflicts by pattern (same time, same room/teacher but different weeks)
        List<ConflictResult.RoomConflict> groupedRoomConflicts = groupRoomConflictsByPattern(roomConflicts);
        List<ConflictResult.TeacherConflict> groupedTeacherConflicts = groupTeacherConflictsByPattern(teacherConflicts);

        return ConflictResult.builder()
                .roomConflicts(groupedRoomConflicts)
                .teacherConflicts(groupedTeacherConflicts)
                .totalConflicts(groupedRoomConflicts.size() + groupedTeacherConflicts.size())
                .build();
    }

    @Override
    public List<ConflictResult.RoomConflict> detectRoomConflicts(List<ScheduleEntry> scheduleEntries) {
        List<ConflictResult.RoomConflict> conflicts = new ArrayList<>();

        Map<String, Map<String, List<ScheduleEntryWithTimeSlot>>> roomTimeMap = new HashMap<>();

        for (ScheduleEntry entry : scheduleEntries) {
            if (entry.getTimeSlots() == null) continue;

            if (isOnlineClass(entry)) {
                continue;
            }

            for (ScheduleEntry.TimeSlot timeSlot : entry.getTimeSlots()) {
                String room = entry.getRoom();
                String timeKey = timeSlot.getSlotKey();
                roomTimeMap
                        .computeIfAbsent(room, k -> new HashMap<>())
                        .computeIfAbsent(timeKey, k -> new ArrayList<>())
                        .add(new ScheduleEntryWithTimeSlot(entry, timeSlot));
            }
        }

        for (Map.Entry<String, Map<String, List<ScheduleEntryWithTimeSlot>>> roomEntry : roomTimeMap.entrySet()) {
            String room = roomEntry.getKey();
            
            for (Map.Entry<String, List<ScheduleEntryWithTimeSlot>> timeEntry : roomEntry.getValue().entrySet()) {
                String timeSlotKey = timeEntry.getKey();
                List<ScheduleEntryWithTimeSlot> entriesAtTime = timeEntry.getValue();

                if (entriesAtTime.size() > 1) {
                    for (int i = 0; i < entriesAtTime.size(); i++) {
                        ScheduleEntryWithTimeSlot entry = entriesAtTime.get(i);
                    }

                    List<ScheduleEntryWithTimeSlot> uniqueEntries = removeDuplicateEntriesWithTimeSlot(entriesAtTime);

                    if (uniqueEntries.size() > 1) {
                        // Tạo TimeSlot đại diện từ tất cả conflicts thay vì chỉ lấy của entry đầu tiên
                        ScheduleEntry.TimeSlot representativeTimeSlot = createRepresentativeTimeSlot(timeSlotKey, uniqueEntries);
                        
                        List<ScheduleEntry> conflictingSchedules = uniqueEntries.stream()
                                .map(ewt -> ewt.entry)
                                .collect(Collectors.toList());

                        ConflictResult.RoomConflict conflict = ConflictResult.RoomConflict.builder()
                                .room(room)
                                .timeSlot(representativeTimeSlot)
                                .conflictingSchedules(conflictingSchedules)
                                .build();
                        conflicts.add(conflict);
                    }
                }
            }
        }

        return conflicts;
    }

    @Override
    public List<ConflictResult.TeacherConflict> detectTeacherConflicts(List<ScheduleEntry> scheduleEntries) {
        List<ConflictResult.TeacherConflict> conflicts = new ArrayList<>();

        Map<String, Map<String, List<ScheduleEntryWithTimeSlot>>> teacherTimeMap = new HashMap<>();

        for (ScheduleEntry entry : scheduleEntries) {
            if (entry.getTimeSlots() == null) continue;

            for (ScheduleEntry.TimeSlot timeSlot : entry.getTimeSlots()) {
                String teacherId = entry.getTeacherId();
                String timeKey = timeSlot.getSlotKey();

                teacherTimeMap
                        .computeIfAbsent(teacherId, k -> new HashMap<>())
                        .computeIfAbsent(timeKey, k -> new ArrayList<>())
                        .add(new ScheduleEntryWithTimeSlot(entry, timeSlot));
            }
        }

        // Find conflicts (same teacher, same time, different subjects/rooms)
        for (Map.Entry<String, Map<String, List<ScheduleEntryWithTimeSlot>>> teacherEntry : teacherTimeMap.entrySet()) {
            String teacherId = teacherEntry.getKey();
            
            for (Map.Entry<String, List<ScheduleEntryWithTimeSlot>> timeEntry : teacherEntry.getValue().entrySet()) {
                List<ScheduleEntryWithTimeSlot> entriesAtTime = timeEntry.getValue();

                if (entriesAtTime.size() > 1) {
                    // Remove duplicates (same subject, same room)
                    List<ScheduleEntryWithTimeSlot> uniqueEntries = removeDuplicateEntriesWithTimeSlot(entriesAtTime);
                    
                    if (uniqueEntries.size() > 1) {
                        ScheduleEntryWithTimeSlot firstEntryWithTime = uniqueEntries.get(0);
                        ScheduleEntry firstEntry = firstEntryWithTime.entry;
                        ScheduleEntry.TimeSlot conflictTimeSlot = firstEntryWithTime.timeSlot;
                        
                        List<ScheduleEntry> conflictingSchedules = uniqueEntries.stream()
                                .map(ewt -> ewt.entry)
                                .collect(Collectors.toList());
                        
                        ConflictResult.TeacherConflict conflict = ConflictResult.TeacherConflict.builder()
                                .teacherId(teacherId)
                                .teacherName(firstEntry.getTeacherName())
                                .timeSlot(conflictTimeSlot)
                                .conflictingSchedules(conflictingSchedules)
                                .build();
                        conflicts.add(conflict);
                    }
                }
            }
        }

        return conflicts;
    }

    private List<ScheduleEntryWithTimeSlot> removeDuplicateEntriesWithTimeSlot(List<ScheduleEntryWithTimeSlot> entries) {
        Map<String, ScheduleEntryWithTimeSlot> uniqueMap = new LinkedHashMap<>();
        
        for (ScheduleEntryWithTimeSlot entryWithTime : entries) {
            ScheduleEntry entry = entryWithTime.entry;
            String key = entry.getSubjectCode() + "-" + entry.getRoom() + "-" + entry.getTeacherId();
            uniqueMap.put(key, entryWithTime);
        }
        
        return new ArrayList<>(uniqueMap.values());
    }

    private ScheduleEntry.TimeSlot createRepresentativeTimeSlot(String slotKey, List<ScheduleEntryWithTimeSlot> entries) {
        // Parse SlotKey: "Tuần 1-Thứ 5-1-1-2"
        // parts[0]="Tuần", parts[1]="1", parts[2]="Thứ", parts[3]="5", parts[4]="1", parts[5]="1", parts[6]="2"
        String[] parts = slotKey.split("-");
        
        if (parts.length >= 7) {
            String date = parts[0] + " " + parts[1]; // "Tuần 1"
            String dayOfWeek = parts[2] + " " + parts[3]; // "Thứ 5"
            String shift = parts[4];
            String startPeriod = parts[5]; 
            String numberOfPeriods = parts[6];
            
            return ScheduleEntry.TimeSlot.builder()
                    .date(date)
                    .dayOfWeek(dayOfWeek)
                    .shift(shift)
                    .startPeriod(startPeriod)
                    .numberOfPeriods(numberOfPeriods)
                    .build();
        }
        
        // Fallback: lấy từ entry đầu tiên
        return entries.get(0).timeSlot;
    }

    private boolean isOnlineClass(ScheduleEntry entry) {
        if (entry.getRoom() == null) return false;
        
        String room = entry.getRoom().toLowerCase().trim();
        String building = entry.getBuilding().toLowerCase().trim();
        // Check if room/building contains "online" keyword
        return room.contains("online") || room.contains("trực tuyến") || room.contains("zoom") || room.contains("meet")
                || room.contains("lms") || building.contains("lms");
    }

    private List<ConflictResult.RoomConflict> groupRoomConflictsByPattern(List<ConflictResult.RoomConflict> conflicts) {
        Map<String, List<ConflictResult.RoomConflict>> groupedMap = new HashMap<>();
        
        for (ConflictResult.RoomConflict conflict : conflicts) {
            String key = conflict.getConflictKey();
            groupedMap.computeIfAbsent(key, k -> new ArrayList<>()).add(conflict);
        }
        
        List<ConflictResult.RoomConflict> result = new ArrayList<>();
        for (List<ConflictResult.RoomConflict> group : groupedMap.values()) {
            if (group.size() == 1) {
                // Single conflict, no grouping needed
                result.add(group.get(0));
            } else {
                // Multiple conflicts with same pattern, group them
                ConflictResult.RoomConflict representative = group.get(0);
                Set<String> allWeeks = new LinkedHashSet<>();
                Set<ScheduleEntry> allSchedules = new LinkedHashSet<>();
                
                for (ConflictResult.RoomConflict conflict : group) {
                    // Extract week from timeSlot.date (e.g., "Tuần 1" -> "1")
                    String week = extractWeekNumber(conflict.getTimeSlot().getDate());
                    if (week != null) {
                        allWeeks.add(week);
                    }
                    allSchedules.addAll(conflict.getConflictingSchedules());
                }
                
                ConflictResult.RoomConflict groupedConflict = ConflictResult.RoomConflict.builder()
                        .room(representative.getRoom())
                        .timeSlot(representative.getTimeSlot())
                        .conflictingSchedules(new ArrayList<>(allSchedules))
                        .conflictWeeks(new ArrayList<>(allWeeks))
                        .build();
                        
                result.add(groupedConflict);
            }
        }
        
        return result;
    }

    private List<ConflictResult.TeacherConflict> groupTeacherConflictsByPattern(List<ConflictResult.TeacherConflict> conflicts) {
        Map<String, List<ConflictResult.TeacherConflict>> groupedMap = new HashMap<>();
        
        for (ConflictResult.TeacherConflict conflict : conflicts) {
            String key = conflict.getConflictKey();
            groupedMap.computeIfAbsent(key, k -> new ArrayList<>()).add(conflict);
        }
        
        List<ConflictResult.TeacherConflict> result = new ArrayList<>();
        for (List<ConflictResult.TeacherConflict> group : groupedMap.values()) {
            if (group.size() == 1) {
                // Single conflict, no grouping needed
                result.add(group.get(0));
            } else {
                // Multiple conflicts with same pattern, group them
                ConflictResult.TeacherConflict representative = group.get(0);
                Set<String> allWeeks = new LinkedHashSet<>();
                Set<ScheduleEntry> allSchedules = new LinkedHashSet<>();
                
                for (ConflictResult.TeacherConflict conflict : group) {
                    // Extract week from timeSlot.date (e.g., "Tuần 1" -> "1")
                    String week = extractWeekNumber(conflict.getTimeSlot().getDate());
                    if (week != null) {
                        allWeeks.add(week);
                    }
                    allSchedules.addAll(conflict.getConflictingSchedules());
                }
                
                ConflictResult.TeacherConflict groupedConflict = ConflictResult.TeacherConflict.builder()
                        .teacherId(representative.getTeacherId())
                        .teacherName(representative.getTeacherName())
                        .timeSlot(representative.getTimeSlot())
                        .conflictingSchedules(new ArrayList<>(allSchedules))
                        .conflictWeeks(new ArrayList<>(allWeeks))
                        .build();
                        
                result.add(groupedConflict);
            }
        }
        
        return result;
    }

    private String extractWeekNumber(String date) {
        if (date == null) return null;
        
        // Pattern: "Tuần X" where X is the week number
        if (date.startsWith("Tuần ")) {
            return date.substring(5).trim();
        }
        
        return date;
    }
}