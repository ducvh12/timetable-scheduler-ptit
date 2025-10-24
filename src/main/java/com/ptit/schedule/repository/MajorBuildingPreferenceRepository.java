package com.ptit.schedule.repository;

import com.ptit.schedule.entity.MajorBuildingPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MajorBuildingPreferenceRepository extends JpaRepository<MajorBuildingPreference, Long> {

    List<MajorBuildingPreference> findByNganhAndIsActiveTrueOrderByPriorityLevelAsc(String nganh);

    List<MajorBuildingPreference> findByIsActiveTrueOrderByNganhAscPriorityLevelAsc();

    Optional<MajorBuildingPreference> findByNganhAndPreferredBuildingAndIsActiveTrue(String nganh, String building);

    @Query("SELECT DISTINCT m.nganh FROM MajorBuildingPreference m WHERE m.isActive = true ORDER BY m.nganh")
    List<String> findDistinctActiveMajors();
}
