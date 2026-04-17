-- ================================================
-- UdjatTrack Time-Series Database Schema Creation
-- Target Database: MySQL 8.0+
-- Dedicated for high-frequency telemetry records
-- ================================================

CREATE DATABASE IF NOT EXISTS udjattrack_ts;
USE udjattrack_ts;

DROP TABLE IF EXISTS telemetry_records;

-- To optimize MySQL for Time-Series, we partition the data by month 
-- (Assuming timestamp is the partitioning key, but keeping it simple for the application default)
CREATE TABLE telemetry_records (
    telemetry_id BINARY(16) NOT NULL,
    trip_id BINARY(16) NOT NULL,
    speed DOUBLE,
    location VARCHAR(255),
    driver_state VARCHAR(20),
    details JSON,
    timestamp DATETIME(6) NOT NULL,
    PRIMARY KEY (telemetry_id)
) ENGINE=InnoDB;

-- Optional: Add indexes for time-series quick retrieval
CREATE INDEX idx_telemetry_trip_time ON telemetry_records (trip_id, timestamp DESC);
CREATE INDEX idx_telemetry_timestamp ON telemetry_records (timestamp DESC);
