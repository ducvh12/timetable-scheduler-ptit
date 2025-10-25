package com.ptit.schedule.service;

import com.ptit.schedule.entity.MajorBuildingPreference;
import com.ptit.schedule.repository.MajorBuildingPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class    MajorBuildingPreferenceService {

    private final MajorBuildingPreferenceRepository repository;

    public List<String> getPreferredBuildingsForMajor(String nganh) {
        if (nganh == null || nganh.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return repository.findByNganhAndIsActiveTrueOrderByPriorityLevelAsc(nganh)
                .stream()
                .map(MajorBuildingPreference::getPreferredBuilding)
                .collect(Collectors.toList());
    }

    public MajorBuildingPreference createOrUpdatePreference(String nganh, String building,
            Integer priorityLevel, String notes) {
        Optional<MajorBuildingPreference> existing = repository.findByNganhAndPreferredBuildingAndIsActiveTrue(nganh,
                building);

        if (existing.isPresent()) {
            MajorBuildingPreference pref = existing.get();
            pref.setPriorityLevel(priorityLevel);
            pref.setNotes(notes);
            pref.setUpdatedAt(LocalDateTime.now());
            log.info("Updated preference for major {} building {} to priority {}",
                    nganh, building, priorityLevel);
            return repository.save(pref);
        }

        MajorBuildingPreference newPref = repository.save(MajorBuildingPreference.builder()
                .nganh(nganh)
                .preferredBuilding(building)
                .priorityLevel(priorityLevel)
                .notes(notes)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());

        log.info("Created new preference for major {} building {} with priority {}",
                nganh, building, priorityLevel);
        return newPref;
    }

    public void deactivatePreference(String nganh, String building) {
        repository.findByNganhAndPreferredBuildingAndIsActiveTrue(nganh, building)
                .ifPresent(pref -> {
                    pref.setIsActive(false);
                    pref.setUpdatedAt(LocalDateTime.now());
                    repository.save(pref);
                    log.info("Deactivated preference for major {} building {}", nganh, building);
                });
    }

    public List<MajorBuildingPreference> getAllActivePreferences() {
        return repository.findByIsActiveTrueOrderByNganhAscPriorityLevelAsc();
    }

    public List<String> getAllActiveMajors() {
        return repository.findDistinctActiveMajors();
    }
}
