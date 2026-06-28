-- ====================================================================================
-- UDJAT TRACK: FINAL DATABASE SCHEMA (v2.0)
-- Fully aligned with API Design, Class Diagram, and WebSocket Payloads
-- ====================================================================================

CREATE EXTENSION IF NOT EXISTS timescaledb;

-- ================================================
-- 1. USERS & ROLES
-- ================================================
CREATE TABLE users (
    user_id UUID NOT NULL,
    user_type VARCHAR(31) NOT NULL,
    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    is_read BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id)
);

CREATE TABLE super_managers (
    user_id UUID NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_super_managers_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE TABLE fleet_managers (
    user_id UUID NOT NULL,
    company_name VARCHAR(200) NOT NULL,
    subscription_plan VARCHAR(100),
    verification_status VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id),
    CONSTRAINT fk_fleet_managers_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE
);

CREATE TABLE drivers (
    user_id UUID NOT NULL,
    license_number VARCHAR(50) NOT NULL UNIQUE,
    phone_number VARCHAR(20),
    is_idle BOOLEAN DEFAULT TRUE,
    fleet_manager_id UUID NOT NULL,
    photo_url VARCHAR(500), -- Natively included for dashboard UI profile pictures
    PRIMARY KEY (user_id),
    CONSTRAINT fk_drivers_user FOREIGN KEY (user_id)
        REFERENCES users (user_id) ON DELETE CASCADE,
    CONSTRAINT fk_drivers_fleet FOREIGN KEY (fleet_manager_id)
        REFERENCES fleet_managers (user_id)
);

-- ================================================
-- 2. AUTHENTICATION
-- ================================================
CREATE TABLE refresh_tokens (
    token_id UUID NOT NULL,
    user_id UUID NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT FALSE,
    device_info VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (token_id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE otp_tokens (
    otp_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    otp_code VARCHAR(10) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (otp_id)
);
-- ================================================
-- 3. ASSETS (Vehicles & Dependents)
-- ================================================
CREATE TABLE vehicles (
    vehicle_id UUID NOT NULL,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    model VARCHAR(100) NOT NULL,
    manufacture_year INT,
    license_number VARCHAR(100), --Natively included for API parity
    is_idle BOOLEAN DEFAULT TRUE,
    is_working BOOLEAN DEFAULT TRUE,
    fleet_manager_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (vehicle_id),
    CONSTRAINT fk_vehicles_fleet FOREIGN KEY (fleet_manager_id)
        REFERENCES fleet_managers (user_id)
);

CREATE TABLE dependents (
    dependent_id UUID NOT NULL,
    driver_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    relation VARCHAR(50),
    PRIMARY KEY (dependent_id),
    CONSTRAINT fk_dependents_driver FOREIGN KEY (driver_id)
        REFERENCES drivers (user_id)
);

-- ================================================
-- 4. TRIPS
-- ================================================
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
    CONSTRAINT fk_trips_driver FOREIGN KEY (driver_id)
        REFERENCES drivers (user_id),
    CONSTRAINT fk_trips_vehicle FOREIGN KEY (vehicle_id)
        REFERENCES vehicles (vehicle_id)
);

CREATE TABLE trip_states (
    state_id UUID NOT NULL,
    trip_id UUID NOT NULL UNIQUE,
    -- Stores DriverState enum: DROWSY, UNCONSCIOUS, HIGH_RISK, NORMAL
    driver_state VARCHAR(30),
    trip_progress_state VARCHAR(20),
    latitude VARCHAR(50),
    longitude VARCHAR(50),
    last_updated_at TIMESTAMP,
    PRIMARY KEY (state_id),
    CONSTRAINT fk_trip_states_trip FOREIGN KEY (trip_id)
        REFERENCES trips (trip_id)
);

CREATE TABLE trip_logs (
    log_id UUID NOT NULL,
    trip_id UUID NOT NULL UNIQUE,
    actual_start_time TIMESTAMP,
    actual_end_time TIMESTAMP,
    total_duration DOUBLE PRECISION,
    total_break_time DOUBLE PRECISION,
    PRIMARY KEY (log_id),
    CONSTRAINT fk_trip_logs_trip FOREIGN KEY (trip_id)
        REFERENCES trips (trip_id)
);

-- ================================================
-- 5. ISSUE SYSTEM (Incidents, SOS, Maintenance)
-- ================================================
CREATE TABLE issue_requests (
    issue_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    category VARCHAR(20) NOT NULL,
    payload JSONB,
    triggered_at TIMESTAMP NOT NULL,
    PRIMARY KEY (issue_id),
    CONSTRAINT fk_issue_requests_trip FOREIGN KEY (trip_id)
        REFERENCES trips (trip_id)
);

CREATE TABLE incidents (
    issue_id UUID NOT NULL,
    severity VARCHAR(15) NOT NULL,
    incident_type VARCHAR(40) NOT NULL,
    location VARCHAR(255),
    PRIMARY KEY (issue_id),
    CONSTRAINT fk_incidents_issue FOREIGN KEY (issue_id)
        REFERENCES issue_requests (issue_id) ON DELETE CASCADE
);

CREATE TABLE sos_requests (
    issue_id UUID NOT NULL,
    location VARCHAR(255),
    is_auto_triggered BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (issue_id),
    CONSTRAINT fk_sos_requests_issue FOREIGN KEY (issue_id)
        REFERENCES issue_requests (issue_id) ON DELETE CASCADE
);

CREATE TABLE maintenance_requests (
    issue_id UUID NOT NULL,
    maintenance_type VARCHAR(40) NOT NULL,
    PRIMARY KEY (issue_id),
    CONSTRAINT fk_maintenance_requests_issue FOREIGN KEY (issue_id)
        REFERENCES issue_requests (issue_id) ON DELETE CASCADE
);

-- ================================================
-- 6. TRACKING & ALERTS
-- ================================================
CREATE TABLE alerts (
    alert_id UUID NOT NULL,
    trip_id UUID NOT NULL, --  FIXED: Links directly to the active trip
    alert_type VARCHAR(30) NOT NULL,
    severity VARCHAR(15) NOT NULL,
    acknowledged BOOLEAN DEFAULT FALSE,
    acked_at TIMESTAMP,
    message TEXT,
    timestamp TIMESTAMP NOT NULL,
    alertable_type VARCHAR(50) NOT NULL, --  ADDED: Polymorphic wrapper type (e.g., 'Incident', 'SOSRequest')
    alertable_id UUID NOT NULL,          --  ADDED: Polymorphic wrapper ID
    PRIMARY KEY (alert_id),
    CONSTRAINT fk_alerts_trip FOREIGN KEY (trip_id)
        REFERENCES trips (trip_id) ON DELETE CASCADE
);

CREATE TABLE event_records (
    event_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    severity VARCHAR(15),
    payload JSONB,
    timestamp TIMESTAMP NOT NULL,
    PRIMARY KEY (event_id),
    CONSTRAINT fk_event_records_trip FOREIGN KEY (trip_id)
        REFERENCES trips (trip_id)
);

-- ================================================
-- 7. NOTIFICATIONS
-- ================================================
CREATE TABLE notifications (
    notification_id UUID NOT NULL,
    user_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT,
    is_read BOOLEAN DEFAULT FALSE,
    notification_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    details JSONB, --  ADDED: Natively supports dynamic API payload data
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id)
        REFERENCES users (user_id)
);

CREATE TABLE alert_triggered_notifications (
    notification_id UUID NOT NULL,
    alert_id UUID NOT NULL,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_alert_notif FOREIGN KEY (notification_id)
        REFERENCES notifications (notification_id) ON DELETE CASCADE
);

CREATE TABLE trip_assigned_notifications (
    notification_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    scheduled_start_time TIMESTAMP NOT NULL,
    PRIMARY KEY (notification_id),
    CONSTRAINT fk_trip_notif FOREIGN KEY (notification_id)
        REFERENCES notifications (notification_id) ON DELETE CASCADE
);

-- ================================================
-- 8. TELEMETRY (TimescaleDB)
-- ================================================
DROP TABLE IF EXISTS telemetry_records CASCADE;

CREATE TABLE telemetry_records (
    telemetry_id UUID NOT NULL,
    trip_id UUID NOT NULL,
    speed DOUBLE PRECISION,
    location VARCHAR(255),
    -- Stores DriverState enum: DROWSY, UNCONSCIOUS, HIGH_RISK, NORMAL
    driver_state VARCHAR(30),
    details JSONB,
    timestamp TIMESTAMP NOT NULL
);

SELECT create_hypertable('telemetry_records', 'timestamp');

CREATE INDEX idx_telemetry_trip_time
    ON telemetry_records (trip_id, timestamp DESC);

SELECT * FROM timescaledb_information.hypertables;

SELECT * FROM users;
-- 1. Standardize Alert Acknowledgment naming
-- Renaming 'acked_at' to 'acknowledged_at' to match the updated Entity naming and code logic.
ALTER TABLE alerts ADD COLUMN IF NOT EXISTS acknowledged_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- 2. Enhance Real-Time Tracking
-- Adding 'current_speed' to 'trip_states' to support the dashboard's live map gauges.
ALTER TABLE trip_states ADD COLUMN IF NOT EXISTS current_speed DOUBLE PRECISION;

-- 3. Cleanup Legacy Fields (Safety Validation)
-- Ensuring NO 'description' columns remain in the polymorphic Issue tables.
ALTER TABLE incidents DROP COLUMN IF EXISTS description;
ALTER TABLE maintenance_requests DROP COLUMN IF EXISTS description;
ALTER TABLE sos_requests DROP COLUMN IF EXISTS description;
ALTER TABLE issue_requests DROP COLUMN IF EXISTS reported_at;

-- 4. Verification:
-- The 'alerts' table already has 'timestamp', no action needed.
-- The 'users' table already has 'is_read', etc.

-- 1. Remove deprecated 'description' fields from Issue tracking tables
-- Since IssueRequest uses the JOINED inheritance strategy, we check the child tables.
ALTER TABLE incidents DROP COLUMN IF EXISTS description;
ALTER TABLE maintenance_requests DROP COLUMN IF EXISTS description;
ALTER TABLE sos_requests DROP COLUMN IF EXISTS description;

-- 2. Cleanup 'alerts' table
-- We removed 'read_by_manager' in favor of the 'acknowledged' status.
ALTER TABLE alerts DROP COLUMN IF EXISTS read_by_manager;

-- 3. Ensure polymorphic Alertable IDs are standard UUIDs
-- If your previous schema used VARCHAR for IDs, ensure they are converted to UUID.
-- ALTER TABLE alerts ALTER COLUMN alertable_id TYPE UUID USING alertable_id::UUID;

-- 4. Optimization: Ensure payload columns use JSONB for faster indexing/querying
-- (IssueRequest and its children use JSONB, while EventRecord uses standard JSON)
ALTER TABLE issue_requests ALTER COLUMN payload TYPE JSONB USING payload::jsonb;
ALTER TABLE event_records ALTER COLUMN payload TYPE JSON USING payload::json;

-- 5. Indexing for High-Performance WebSocket Lookups
-- We frequently query alerts and telemetry by Trip ID for real-time broadcasts.
CREATE INDEX IF NOT EXISTS idx_alerts_trip_id ON alerts(trip_id);
CREATE INDEX IF NOT EXISTS idx_event_records_trip_id ON event_records(trip_id);
CREATE INDEX IF NOT EXISTS idx_trip_states_trip_id ON trip_states(trip_id);

-- 6. Add Audit Columns if missing (required by JpaAuditing)
TRUNCATE TABLE alerts CASCADE;
ALTER TABLE alerts DROP COLUMN IF EXISTS acked_at;
select * from issue_requests;

select * from trips

ALTER TABLE notifications DROP CONSTRAINT fk_notifications_user;
ALTER TABLE notifications
  ADD CONSTRAINT fk_notifications_user
  FOREIGN KEY (user_id)
  REFERENCES users(user_id)
  ON DELETE CASCADE;
