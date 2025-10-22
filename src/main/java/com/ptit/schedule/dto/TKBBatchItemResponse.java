package com.ptit.schedule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response for single item in TKB batch
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TKBBatchItemResponse {
    private TKBRequest input;
    private List<TKBRowResult> rows;
    private String note;
}