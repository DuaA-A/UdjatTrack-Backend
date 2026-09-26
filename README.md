# UdjatTrack: Fleet and Driver Monitoring Platform
**Project Overview & Technical Report**

## 1. Project Overview
**UdjatTrack** is a comprehensive Fleet and Driver Monitoring Platform designed to manage fleets, track vehicles, and monitor driver safety in real-time. The system provides a robust backend infrastructure to support complex operations such as real-time telemetry tracking, trip lifecycle management, driver state monitoring (including fatigue and drowsiness detection), and instant alert dispatching.

The platform caters to three primary roles:
- **SUPER_MANAGER**: Platform administrators who manage fleet manager accounts and subscriptions.
- **FLEET_MANAGER**: Managers who oversee their company's drivers, vehicles, trips, and handle real-time alerts.
- **DRIVER**: End-users operating the vehicles, tracked via a mobile application that feeds real-time data back to the system.

## 2. Technology Stack
The backend is built with modern, high-performance, and scalable technologies:

- **Core Language**: Java 21
- **Framework**: Spring Boot 3.2.3
- **Primary Database**: PostgreSQL (Relational Data)
- **Time-Series Database**: TimescaleDB (Extension on PostgreSQL for Telemetry)
- **Authentication & Security**: Spring Security, JWT (JSON Web Tokens) with Refresh Token Rotation, OTP for password resets.
- **Real-Time Communication**: WebSockets (STOMP over SockJS)
- **API Documentation**: OpenAPI 3 / Swagger (`springdoc`)
- **Data Mapping & Boilerplate**: MapStruct, Lombok
- **External Integrations**: Resend API (Email dispatching), OpenPDF (Report generation)
- **Testing**: JUnit, Spring Boot Test, H2 In-Memory Database

## 3. Backend Architecture & Implementation Details

### 3.1. Database Design & Optimization
The database schema is highly optimized for both relational integrity and high-frequency time-series data insertion.

- **Role-Based Entity Inheritance**: Implements a clean relational model for users where `users` acts as a base table, and `super_managers`, `fleet_managers`, and `drivers` are strongly typed extensions.
- **Time-Series Hypertables**: Utilizes **TimescaleDB** for the `telemetry_records` table. This hypertable is partitioned by time, allowing the system to handle thousands of high-frequency telemetry inserts (speed, GPS location, driver state) per second without degrading read/write performance.
- **Polymorphic Issue Tracking**: Uses a polymorphic database design (JOINED inheritance strategy) for `issue_requests`. Specific issues like `incidents`, `sos_requests`, and `maintenance_requests` extend the base issue table, allowing scalable handling of different event types while sharing common logic.
- **JSONB Payloads**: Leverages PostgreSQL's `JSONB` for dynamic payloads in `alerts`, `event_records`, and `notifications`, providing schema flexibility for complex, evolving API data without requiring constant migrations.

### 3.2. Security & Authentication
- **Stateless Authentication**: Implemented using **JWT**. Access tokens have short lifespans (15 mins), while refresh tokens (7 days) are stored in the database, allowing for token revocation and secure, continuous sessions.
- **Role-Based Access Control (RBAC)**: Endpoints are strictly protected based on the user's role (Super Manager, Fleet Manager, Driver), ensuring data isolation between different fleets.
- **OTP Flows**: Secure password reset functionality utilizing time-limited, single-use OTPs sent via the Resend Email API.

### 3.3. Trip Lifecycle & State Machine
The backend orchestrates a strict state machine for Trip Management:
- **States**: `PLANNED` -> `ONGOING` -> `PAUSED` -> `RESUMED` -> `FINISHED` (or `CANCELLED`).
- The transition logic validates operations to prevent illegal state changes (e.g., preventing a trip from ending before it starts, or assigning a vehicle already on an active trip).
- **Trip Logs**: Automatically calculates actual start/end times, total durations, and break times, storing them in `trip_logs` for reporting and analytics.

### 3.4. Real-Time Telemetry & Alerting System
A core feature of the UdjatTrack backend is its ability to process live tracking data:
- **WebSockets**: Implemented STOMP over WebSockets to push live updates to the Fleet Manager's dashboard.
- **Driver State Monitoring**: The system processes incoming telemetry that includes the driver's state (`DROWSY`, `UNCONSCIOUS`, `HIGH_RISK`, `NORMAL`).
- **Event-Driven Alerts**: When risky behavior (like drowsiness or high blink rates) or an SOS is detected, the system immediately generates an `Alert`, logs an `EventRecord`, and dispatches a `Notification` to the relevant Fleet Manager in real-time.

### 3.5. System Performance & Quality Attributes
- **Scalability**: Separation of standard relational data (Users, Vehicles) from high-volume telemetry data (TimescaleDB) ensures the database can scale horizontally for tracking data.
- **Maintainability**: Extensive use of MapStruct for DTO-Entity mapping keeps the presentation layer decoupled from the persistence layer.
- **Extensibility**: The polymorphic design of Alerts and Issues allows new types of hardware sensors or monitoring events to be added with minimal database restructuring.

## 4. Key Features for CV Highlighting
If you are adding this to your CV, here are the strongest points to emphasize:
1. **Designed and developed a scalable fleet management REST API** using Java 21 and Spring Boot 3, handling complex domain logic for drivers, vehicles, and trips.
2. **Architected a high-throughput real-time tracking system** by integrating PostgreSQL with **TimescaleDB** hypertables, optimizing the ingestion and querying of live telemetry and GPS data.
3. **Implemented real-time bi-directional communication** using WebSockets (STOMP) to instantly push critical alerts (SOS, driver fatigue, incidents) to manager dashboards.
4. **Engineered a robust, stateless security layer** using JWT with refresh token rotation and Role-Based Access Control (RBAC) to enforce strict data isolation between multiple fleet companies.
5. **Utilized polymorphic database design** (JOINED inheritance) and JSONB columns to create a flexible, extensible issue-tracking and event-logging architecture.
