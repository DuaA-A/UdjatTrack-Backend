-- ================================================
-- UdjatTrack Database Schema Creation Script
-- Target Database: MySQL 8.0+
-- ================================================




-- SET FOREIGN_KEY_CHECKS = 0;

-- 1. Users & Inheritance
DROP TABLE IF EXISTS super_managers;
DROP TABLE IF EXISTS fleet_managers;
DROP TABLE IF EXISTS drivers;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    user_id UUID NOT NULL,
    user_type VARCHAR(31) NOT NULL,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id)
) ;

CREATE TABLE super_managers (
    user_id UUID NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_super_managers_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ;

CREATE TABLE fleet_managers (
    user_id UUID NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    subscription_plan VARCHAR(100),
    verification_status VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_fleet_managers_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
) ;

CREATE TABLE drivers (
    user_id UUID NOT NULL,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    is_idle BOOLEAN DEFAULT TRUE,
    fleet_manager_id UUID NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_drivers_user FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_drivers_fleet FOREIGN KEY (fleet_manager_id) REFERENCES fleet_managers (user_id)
) ;

-- 2. Auth Tokens
DROP TABLE IF EXISTS refresh_tokens;
DROP TABLE IF EXISTS otp_tokens;

CREATE TABLE refresh_tokens (
    token_id UUID NOT NULL,
    user_id UUID NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    device_info VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (token_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ;

CREATE TABLE otp_tokens (
    otp_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (otp_id)
) ;

-- 3. Assets
DROP TABLE IF EXISTS vehicles;
DROP TABLE IF EXISTS dependents;

CREATE TABLE vehicles (
    vehicle_id UUID NOT NULL,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    model VARCHAR(100) NOT NULL,
    manufacture_year INT,
    is_idle BOOLEAN DEFAULT TRUE,
    is_working BOOLEAN DEFAULT TRUE,
    is_deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    fleet_manager_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (vehicle_id),
    CONSTRAINT fk_vehicles_fleet FOREIGN KEY (fleet_manager_id) REFERENCES fleet_managers (user_id)
) ;

CREATE TABLE dependents (
    dependent_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    relation VARCHAR(50),
    PRIMARY KEY (dependent_id),
    CONSTRAINT fk_dependents_driver FOREIGN KEY (driver_id) REFERENCES drivers (user_id)
) ;

-- 4. Trips & Logs
DROP TABLE IF EXISTS trip_logs;
DROP TABLE IF EXISTS trip_states;
DROP TABLE IF EXISTS trips;

CREATE TABLE trips (
    trip_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    source VARCHAR(255) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    scheduled_start_time TIMESTAMP,
    scheduled_end_time TIMESTAMP,
    trip_state VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (trip_id),
    CONSTRAINT fk_trips_driver FOREIGN KEY (driver_id) REFERENCES drivers (user_id),
    CONSTRAINT fk_trips_vehicle FOREIGN KEY (vehicle_id) REFERENCES vehicles (vehicle_id)
) ;

CREATE TABLE trip_states (
    state_id UUID NOT NULL,
    trip_id UUID NOT NULL UNIQUE,
    driver_state VARCHAR(20),
    trip_progress_state VARCHAR(20),
    latitude VARCHAR(50),
    longitude VARCHAR(50),
    last_updated_at TIMESTAMP,
    PRIMARY KEY (state_id),
    CONSTRAINT fk_trip_states_trip FOREIGN KEY (trip_id) REFERENCES trips (trip_id)
) ;

CREATE TABLE trip_logs (
    log_id UUID NOT NULL,
    trip_id UUID NOT NULL UNIQUE,
    actual_start_time TIMESTAMP,
    actual_end_time TIMESTAMP,
    total_duration DOUBLE PRECISION,
    total_break_time DOUBLE PRECISION,
    PRIMARY KEY (log_id),
    CONSTRAINT fk_trip_logs_trip FOREIGN KEY (trip_id) REFERENCES trips (trip_id)
) ;

-- 5. Tracking / Telemetry
DROP TABLE IF EXISTS alerts;
DROP TABLE IF EXISTS incidents;
DROP TABLE IF EXISTS event_records;
DROP TABLE IF EXISTS telemetry_records;

CREATE TABLE alerts (
    alert_id UUID NOT NULL,
    trip_log_id UUID NOT NULL,
    alert_type VARCHAR(30) NOT NULL,
    severity VARCHAR(15) NOT NULL,
    acknowledged BOOLEAN DEFAULT FALSE,
    message TEXT,
    timestamp TIMESTAMP NOT NULL,
    PRIMARY KEY (alert_id),
    CONSTRAINT fk_alerts_log FOREIGN KEY (trip_log_id) REFERENCES trip_logs (log_id)
) ;

CREATE TABLE incidents (
    incident_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    severity VARCHAR(15) NOT NULL,
    incident_type VARCHAR(40) NOT NULL,
    location VARCHAR(255),
    payload JSONB,
    reported_at TIMESTAMP NOT NULL,
    PRIMARY KEY (incident_id),
    CONSTRAINT fk_incidents_trip FOREIGN KEY (trip_id) REFERENCES trips (trip_id)
) ;

CREATE TABLE event_records (
    event_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    severity VARCHAR(15),
    payload JSONB,
    timestamp TIMESTAMP NOT NULL,
    PRIMARY KEY (event_id),
    CONSTRAINT fk_event_records_trip FOREIGN KEY (trip_id) REFERENCES trips (trip_id)
) ;



-- 6. Issues (SOS / Maintenance)
DROP TABLE IF EXISTS maintenance_requests;
DROP TABLE IF EXISTS sos_requests;
DROP TABLE IF EXISTS issue_requests;

CREATE TABLE issue_requests (
    issue_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    category VARCHAR(20) NOT NULL,
    payload JSONB,
    triggered_at TIMESTAMP NOT NULL,
    reported_at TIMESTAMP,
    PRIMARY KEY (issue_id),
    CONSTRAINT fk_issue_requests_trip FOREIGN KEY (trip_id) REFERENCES trips (trip_id)
) ;

CREATE TABLE sos_requests (
    issue_id UUID NOT NULL,
    location VARCHAR(255),
    is_auto_triggered BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (issue_id),
    CONSTRAINT fk_sos_requests_issue FOREIGN KEY (issue_id) REFERENCES issue_requests (issue_id) ON DELETE CASCADE
) ;

CREATE TABLE maintenance_requests (
    issue_id UUID NOT NULL,
    maintenance_type VARCHAR(40) NOT NULL,
    description TEXT,
    PRIMARY KEY (issue_id),
    CONSTRAINT fk_maintenance_requests_issue FOREIGN KEY (issue_id) REFERENCES issue_requests (issue_id) ON DELETE CASCADE
) ;

-- 7. Notifications
DROP TABLE IF EXISTS alert_triggered_notifications;
DROP TABLE IF EXISTS trip_assigned_notifications;
DROP TABLE IF EXISTS notifications;

CREATE TABLE notifications (
    notification_id UUID NOT NULL,
    user_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    notification_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (user_id)
) ;

CREATE TABLE alert_triggered_notifications (
    notification_id UUID NOT NULL,
    alert_id UUID NOT NULL,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_alert_notif FOREIGN KEY (notification_id) REFERENCES notifications (notification_id) ON DELETE CASCADE
) ;

CREATE TABLE trip_assigned_notifications (
    notification_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    scheduled_start_time TIMESTAMP NOT NULL,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_trip_notif FOREIGN KEY (notification_id) REFERENCES notifications (notification_id) ON DELETE CASCADE
) ;

-- SET FOREIGN_KEY_CHECKS = 1;

-- ================================================
-- UdjatTrack Seed Data
-- Run once after first app startup
-- ================================================

-- Clear existing test data
DELETE FROM refresh_tokens;
DELETE FROM otp_tokens;
DELETE FROM fleet_managers;
DELETE FROM users;

-- ================================================
-- 1. Super Manager
-- Password: Admin1234!
-- ================================================
INSERT INTO users (
    user_type, user_id, name, email, password,
    role, is_deleted, created_at
) VALUES (
    'SUPER_MANAGER',
    gen_random_uuid(),
    'Super Admin',
    'admin@udjattrack.com',
    '$2a$12$CG5tIyECF/3C.I4n9Sqfd.bzFYUJjV8120lMysyWaQPqGZIOOa7aO',
    'ROLE_SUPER_MANAGER',
    FALSE,
    CURRENT_TIMESTAMP
);

-- ================================================
-- 2. Fleet Manager (pre-approved for testing)
-- Password: Fleet1234!
-- ================================================


INSERT INTO users (
    user_type, user_id, name, email, password,
    role, is_deleted, created_at
) VALUES (
    'FLEET_MANAGER',
    '49e2cdde-97ec-4050-9c73-4a2c923d8495',
    'Fleet Manager One',
    'fleet@test.com',
    '$2a$12$CG5tIyECF/3C.I4n9Sqfd.bzFYUJjV8120lMysyWaQPqGZIOOa7aO',
    'ROLE_FLEET_MANAGER',
    FALSE,
    CURRENT_TIMESTAMP
);

INSERT INTO fleet_managers (
    user_id, company_name, subscription_plan, verification_status
) VALUES (
    '49e2cdde-97ec-4050-9c73-4a2c923d8495',
    'Test Company',
    'BASIC',
    'VERIFIED'
);

-- ================================================
-- 3. Driver
-- Password: Driver1234!
-- ================================================


INSERT INTO users (
    user_type, user_id, name, email, password,
    role, is_deleted, created_at
) VALUES (
    'DRIVER',
    '0b5d7f74-a884-4b22-8a16-33ac4be0eecd',
    'Ahmed Hassan',
    'driver@test.com',
    '$2a$12$CG5tIyECF/3C.I4n9Sqfd.bzFYUJjV8120lMysyWaQPqGZIOOa7aO',
    'ROLE_DRIVER',
    FALSE,
    CURRENT_TIMESTAMP
);

-- 8. TimescaleDB Telemetry Tracking
DROP TABLE IF EXISTS telemetry_records CASCADE;

CREATE TABLE telemetry_records (
    telemetry_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    speed DOUBLE PRECISION,
    location VARCHAR(255),
    driver_state VARCHAR(20),
    details JSONB,
    timestamp TIMESTAMP NOT NULL
);

-- Elevate the telemetry table to a TimescaleDB Hypertable
-- Partitions data by 'timestamp' dynamically for massive time-series scale
SELECT create_hypertable('telemetry_records', 'timestamp');

CREATE INDEX idx_telemetry_trip_time ON telemetry_records (trip_id, timestamp DESC);
