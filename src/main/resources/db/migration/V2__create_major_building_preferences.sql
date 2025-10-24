CREATE TABLE IF NOT EXISTS major_building_preferences (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nganh VARCHAR(100) NOT NULL,
    preferred_building VARCHAR(10) NOT NULL,
    priority_level INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    notes VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_nganh_active (nganh, is_active),
    INDEX idx_priority (priority_level),
    UNIQUE KEY uk_nganh_building_active (nganh, preferred_building, is_active)
);

-- Insert sample data
INSERT INTO major_building_preferences (nganh, preferred_building, priority_level, notes) VALUES
('Công nghệ thông tin', 'A2', 1, 'Tòa chính, gần phòng lab'),
('Công nghệ thông tin', 'A3', 2, 'Dự phòng'),
('An toàn thông tin', 'A3', 1, 'Gần lab ATTT'),
('An toàn thông tin', 'A2', 2, 'Dự phòng'),
('Kỹ thuật điện tử viễn thông', 'A1', 1, 'Khu vực chuyên ngành');
