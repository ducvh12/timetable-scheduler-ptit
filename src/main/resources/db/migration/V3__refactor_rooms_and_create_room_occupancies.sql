-- =============================================
-- File: V3__refactor_rooms_and_create_room_occupancies.sql
-- Description: Refactor rooms table columns and create room_occupancies table
-- Author: System
-- Date: 2024
-- =============================================

-- Step 1: Populate name from phong if name is empty
UPDATE rooms 
SET name = phong 
WHERE (name IS NULL OR name = '') 
  AND phong IS NOT NULL 
  AND phong != '';

-- Step 2: Make sure name is NOT NULL
ALTER TABLE rooms 
    MODIFY COLUMN name VARCHAR(10) NOT NULL COMMENT 'Số phòng (renamed from phong)';

-- Step 3: Drop old phong column (MySQL 5.7+ compatible)
ALTER TABLE rooms 
    DROP COLUMN phong;

-- Step 4: Create room_occupancies table
CREATE TABLE IF NOT EXISTS room_occupancies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Auto-increment ID',
    room_id BIGINT NOT NULL COMMENT 'Foreign key to rooms table',
    semester_id BIGINT NOT NULL COMMENT 'Foreign key to semesters table',
    day_of_week INT NOT NULL COMMENT 'Thứ trong tuần (2-7)',
    period INT NOT NULL COMMENT 'Tiết học (1-6)',
    unique_key VARCHAR(50) NOT NULL COMMENT 'Format: roomCode|dayOfWeek|period (e.g., 404-A2|5|1)',
    note VARCHAR(500) COMMENT 'Ghi chú về lịch chiếm phòng này',

    -- Foreign key constraints
    CONSTRAINT fk_room_occupancy_room FOREIGN KEY (room_id) 
        REFERENCES rooms(id) ON DELETE CASCADE,
    CONSTRAINT fk_room_occupancy_semester FOREIGN KEY (semester_id) 
        REFERENCES semesters(id) ON DELETE CASCADE,
    
    -- Unique constraint: one room can only be occupied once per semester per time slot
    CONSTRAINT uk_room_semester_time UNIQUE (room_id, semester_id, day_of_week, period),
    
    -- Check constraints
    CONSTRAINT chk_day_of_week CHECK (day_of_week BETWEEN 2 AND 7),
    CONSTRAINT chk_period CHECK (period BETWEEN 1 AND 6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Bảng lưu trữ thông tin phòng đã được chiếm theo học kỳ';

-- Step 3: Create indexes for performance
CREATE INDEX idx_room_occupancy_semester ON room_occupancies(semester_id);
CREATE INDEX idx_room_occupancy_room ON room_occupancies(room_id);
CREATE INDEX idx_room_occupancy_unique_key ON room_occupancies(unique_key);
CREATE INDEX idx_room_occupancy_time ON room_occupancies(day_of_week, period);

-- Step 4: Add comments to indexes
ALTER TABLE room_occupancies 
    COMMENT = 'Bảng quản lý phòng đã chiếm theo từng học kỳ, thay thế global_occupied_rooms.json';
