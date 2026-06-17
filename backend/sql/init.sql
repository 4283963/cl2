CREATE DATABASE IF NOT EXISTS smart_agri
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE smart_agri;

CREATE TABLE IF NOT EXISTS machine (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_id VARCHAR(64) NOT NULL UNIQUE,
    machine_name VARCHAR(128),
    work_width DOUBLE DEFAULT 3.0,
    today_area DOUBLE DEFAULT 0.0,
    current_lat DOUBLE,
    current_lng DOUBLE,
    current_work_type VARCHAR(32),
    status VARCHAR(16) DEFAULT 'OFFLINE',
    updated_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS track_point (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    machine_id VARCHAR(64) NOT NULL,
    latitude DOUBLE NOT NULL,
    longitude DOUBLE NOT NULL,
    work_type VARCHAR(32),
    speed DOUBLE DEFAULT 0.0,
    heading DOUBLE DEFAULT 0.0,
    recorded_at DATETIME NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_machine_date (machine_id, recorded_at)
);

INSERT INTO machine (machine_id, machine_name, work_width, current_lat, current_lng, current_work_type, status, updated_at)
VALUES
    ('BD-001', '东方红-1号', 3.0, 34.2658, 108.9541, 'SOWING', 'ONLINE', NOW()),
    ('BD-002', '雷沃-2号', 4.5, 34.2710, 108.9620, 'HARVESTING', 'ONLINE', NOW()),
    ('BD-003', '中联-3号', 3.5, 34.2580, 108.9480, 'PLOWING', 'ONLINE', NOW()),
    ('BD-004', '沃得-4号', 2.8, 34.2750, 108.9700, 'SOWING', 'ONLINE', NOW()),
    ('BD-005', '久保田-5号', 3.2, 34.2600, 108.9550, 'HARVESTING', 'OFFLINE', NOW());
