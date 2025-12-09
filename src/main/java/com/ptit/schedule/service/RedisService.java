package com.ptit.schedule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Service quản lý lastSlotIdx trong Redis
 * Key format: "last_slot_idx:{userId}:{academicYear}:{semester}"
 * Value: Integer (lastSlotIdx)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String LAST_SLOT_PREFIX = "last_slot_idx:";

    /**
     * Tạo Redis key cho lastSlotIdx
     */
    private String buildLastSlotKey(Long userId, String academicYear, String semester) {
        return LAST_SLOT_PREFIX + userId + ":" + academicYear + ":" + semester;
    }

    /**
     * Load lastSlotIdx từ Redis
     * Nếu chưa có key thì tạo mới với giá trị -1
     */
    public int loadLastSlotIdx(Long userId, String academicYear, String semester) {
        try {
            String key = buildLastSlotKey(userId, academicYear, semester);
            Object value = redisTemplate.opsForValue().get(key);
            
            if (value == null) {
                log.info("No lastSlotIdx found for key: {}, creating new with value -1", key);
                // Tạo mới key với giá trị -1
                redisTemplate.opsForValue().set(key, -1);
                return -1;
            }

            int lastSlotIdx = Integer.parseInt(value.toString());
            log.info("Loaded lastSlotIdx: {} from Redis key: {}", lastSlotIdx, key);
            return lastSlotIdx;
            
        } catch (Exception e) {
            log.error("Error loading lastSlotIdx from Redis", e);
            return -1;
        }
    }

    /**
     * Save lastSlotIdx vào Redis
     */
    public void saveLastSlotIdx(Long userId, String academicYear, String semester, int lastSlotIdx) {
        try {
            String key = buildLastSlotKey(userId, academicYear, semester);
            redisTemplate.opsForValue().set(key, lastSlotIdx);
            log.info("Saved lastSlotIdx: {} to Redis key: {}", lastSlotIdx, key);
        } catch (Exception e) {
            log.error("Error saving lastSlotIdx to Redis", e);
        }
    }

    /**
     * Xóa lastSlotIdx
     */
    public void clearLastSlotIdx(Long userId, String academicYear, String semester) {
        try {
            String key = buildLastSlotKey(userId, academicYear, semester);
            redisTemplate.delete(key);
            log.info("Cleared lastSlotIdx for key: {}", key);
        } catch (Exception e) {
            log.error("Error clearing lastSlotIdx from Redis", e);
        }
    }


}
