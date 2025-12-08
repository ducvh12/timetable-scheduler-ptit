-- =============================================
-- File: V4__fix_room_types.sql
-- Description: Fix room types to match RoomType enum values
-- Author: System
-- Date: 2024
-- =============================================

-- Map room types from JSON import to match RoomType enum
-- JSON values: "year2024", "nt", "english", "clc", "general"
-- Enum values: KHOA_2024, NGOC_TRUC, ENGLISH_CLASS, CLC, GENERAL

UPDATE rooms SET type = 'KHOA_2024' WHERE type = 'year2024';
UPDATE rooms SET type = 'NGOC_TRUC' WHERE type = 'nt';
UPDATE rooms SET type = 'ENGLISH_CLASS' WHERE type = 'english';
UPDATE rooms SET type = 'CLC' WHERE type = 'clc';
UPDATE rooms SET type = 'GENERAL' WHERE type = 'general';
