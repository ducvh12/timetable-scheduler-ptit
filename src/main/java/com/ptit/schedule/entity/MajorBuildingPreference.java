package com.ptit.schedule.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "major_building_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MajorBuildingPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nganh", nullable = false)
    private String nganh;

    @Column(name = "preferred_building", nullable = false)
    private String preferredBuilding;

    @Column(name = "priority_level", nullable = false)
    private Integer priorityLevel; // 1 = highest priority

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "notes")
    private String notes;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
