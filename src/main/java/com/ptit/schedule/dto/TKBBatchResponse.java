package com.ptit.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for TKB batch processing
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TKBBatchResponse {
    private List<TKBBatchItemResponse> items;
    private Integer totalRows;
    private Integer lastSlotIdx;
    private Integer occupiedRoomsCount; // Count of rooms used in this batch
    private String note;
    private String error;
}