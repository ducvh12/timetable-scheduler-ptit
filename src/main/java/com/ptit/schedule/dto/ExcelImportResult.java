package com.ptit.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO for Excel import result with success count and warnings
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportResult {
    
    /**
     * Number of subjects successfully imported
     */
    private int successCount;
    
    /**
     * Number of subjects skipped due to duplicates
     */
    private int skippedCount;
    
    /**
     * Total number of rows in Excel (excluding header)
     */
    private int totalRows;
    
    /**
     * List of warning messages (e.g., duplicate subjects)
     */
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
    
    /**
     * List of valid subjects (non-duplicates)
     */
    @Builder.Default
    private List<SubjectRequest> validSubjects = new ArrayList<>();
    
    /**
     * Check if import has any warnings
     */
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
    
    /**
     * Add a warning message
     */
    public void addWarning(String warning) {
        if (this.warnings == null) {
            this.warnings = new ArrayList<>();
        }
        this.warnings.add(warning);
    }
}
