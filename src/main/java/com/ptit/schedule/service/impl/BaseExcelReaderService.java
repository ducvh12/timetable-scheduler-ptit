package com.ptit.schedule.service.impl;

import com.ptit.schedule.exception.FileProcessingException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Base class for Excel reading services.
 * Provides common utilities for parsing Excel files.
 */
public abstract class BaseExcelReaderService {

    /**
     * Validate Excel file format (.xlsx only)
     */
    protected void validateExcelFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new FileProcessingException("File không được để trống");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".xlsx")) {
            throw new FileProcessingException("File phải có định dạng .xlsx");
        }
    }

    /**
     * Open workbook from MultipartFile
     */
    protected Workbook openWorkbook(MultipartFile file) throws IOException {
        validateExcelFile(file);
        InputStream is = file.getInputStream();
        return new XSSFWorkbook(is);
    }

    /**
     * Get cell value as String, return empty string if null
     */
    protected String getCellValue(Row row, int colIndex, DataFormatter formatter) {
        Cell cell = row.getCell(colIndex);
        return cell != null ? formatter.formatCellValue(cell).trim() : "";
    }

    /**
     * Parse integer safely, return 0 if invalid
     */
    protected int parseIntSafe(String value) {
        try {
            return (value == null || value.isBlank()) ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Parse double safely, return 0.0 if invalid
     */
    protected double parseDoubleSafe(String value) {
        try {
            return (value == null || value.isBlank()) ? 0.0 : Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Check if row is empty (all cells are blank)
     */
    protected boolean isRowEmpty(Row row) {
        if (row == null) return true;

        for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = cell.toString().trim();
                if (!value.isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Validate minimum column count
     */
    protected void validateColumnCount(Row row, int minColumns, String sheetName) {
        if (row == null || row.getLastCellNum() < minColumns) {
            throw new FileProcessingException(
                    String.format("Sheet '%s' thiếu cột. Cần ít nhất %d cột.", sheetName, minColumns)
            );
        }
    }
}