package com.ptit.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a single TKB row result - exact mapping to Python _emit_row result
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TKBRowResult {
    private Integer lop; // Class number
    @JsonProperty("ma_mon")
    private String maMon; // Subject code
    @JsonProperty("ten_mon") 
    private String tenMon; // Subject name
    private Integer kip; // Session (1-4)
    private Integer thu; // Day of week (2-7)
    @JsonProperty("tiet_bd")
    private Integer tietBd; // Starting period
    private Integer L; // L value from template
    private String phong; // Room code (ma_phong)
    private Integer AH; // Calculated AH value
    private Integer AI; // AI value (remaining periods)
    private Integer AJ; // AJ value (AI - AH)
    private String N; // Key N from template
    @JsonProperty("O_to_AG")
    private List<String> oToAg; // Week schedule (18 weeks)
    @JsonProperty("student_year")
    private String studentYear; // Student year (e.g., "2024", "2022")
    @JsonProperty("he_dac_thu")
    private String heDacThu; // Special system (e.g., "CLC", "CTTT")
    private String nganh; // Major
}