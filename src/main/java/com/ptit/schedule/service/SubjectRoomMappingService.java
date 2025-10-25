package com.ptit.schedule.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class SubjectRoomMappingService {

    private final Map<String, String> subjectRoomMap = new ConcurrentHashMap<>();

    public void setSubjectRoom(String maMon, String roomCode) {
        subjectRoomMap.put(maMon, roomCode);
        log.info("Mapped subject {} to room {}", maMon, roomCode);
    }

    public String getSubjectRoom(String maMon) {
        return subjectRoomMap.get(maMon);
    }

    public Map<String, String> getAllMappings() {
        return new HashMap<>(subjectRoomMap);
    }

    public void clearMappings() {
        subjectRoomMap.clear();
        log.info("Cleared all subject-room mappings");
    }

    public void clearSubject(String maMon) {
        subjectRoomMap.remove(maMon);
        log.info("Cleared mapping for subject {}", maMon);
    }
}
