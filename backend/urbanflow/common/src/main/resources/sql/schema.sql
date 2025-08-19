--  users
DROP TABLE IF EXISTS `user_area_mapping`;
DROP TABLE IF EXISTS `users`;

CREATE TABLE `users` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT 'Internal unique identifier for the user record.',
  `account_number` VARCHAR(50) NOT NULL UNIQUE COMMENT 'User''s unique employee ID (工号), used as the UserID for login.',
  `user_name` VARCHAR(100) NOT NULL COMMENT 'The full name of the user, e.g., "Jack".',
  `department` VARCHAR(100) NULL COMMENT 'The department the user belongs to.',
  `email` VARCHAR(100) NOT NULL UNIQUE COMMENT 'User''s unique email, used for password reset notifications.',
  `phone_number` VARCHAR(20) NULL COMMENT 'User''s contact phone number.',
  `password` VARCHAR(255) NOT NULL COMMENT 'Hashed password using bcrypt for secure storage.',
  `role` VARCHAR(50) NOT NULL DEFAULT 'ROLE_USER' COMMENT 'User''s role for authorization (e.g., ROLE_USER, ROLE_ADMIN).',
  `enabled` BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'Administrator-controlled status. Maps to the "Status" dropdown (Enabled/Disabled).',
  `locked` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'System-controlled status for temporary lockouts due to failed login attempts.',
  `is_deleted` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Flag for soft delete. If TRUE, user is considered deleted and will not be queryable.',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Timestamp when the user record was created.',
  `updated_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Timestamp when the user record was last updated.'
);

--  password_resets
DROP TABLE IF EXISTS `password_resets`;

CREATE TABLE `password_resets` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY,
  `email` VARCHAR(100) NOT NULL COMMENT 'The email address receiving the reset link, links to the users table.',
  `token` VARCHAR(255) NOT NULL UNIQUE COMMENT 'The secure, unique token generated for the password reset link.',
  `expiry_date` TIMESTAMP NOT NULL COMMENT 'The timestamp when this token becomes invalid.',
  `used` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'A flag to indicate if the token has already been used.',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

--  traffic flow
DROP TABLE IF EXISTS `traffic_flow`;

CREATE TABLE traffic_flow (
    id INT AUTO_INCREMENT PRIMARY KEY,
    time_bucket DATETIME NOT NULL,
    junction_id VARCHAR(64) NOT NULL,
    flow_rate_hourly INT NOT NULL
);

-- congestion junction counts
DROP TABLE IF EXISTS `congestion_junction_counts`;

CREATE TABLE congestion_junction_counts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    time_bucket DATETIME NOT NULL,
    congested_junction_count INT NOT NULL
);

-- congested junctions ranking
DROP TABLE IF EXISTS `congested_times_ranking`;

CREATE TABLE congested_times_ranking (
    id INT AUTO_INCREMENT PRIMARY KEY,
    time_bucket DATETIME NOT NULL,
    junction_id VARCHAR(64) NOT NULL,
    junction_name VARCHAR(128) NOT NULL,
    congestion_times INT NOT NULL
);

-- congested durations ranking
DROP TABLE IF EXISTS `congestion_duration_ranking`;

CREATE TABLE congestion_duration_ranking (
    id INT AUTO_INCREMENT PRIMARY KEY,
    time_bucket DATETIME NOT NULL,
    junction_id VARCHAR(64) NOT NULL,
    junction_name VARCHAR(128) NOT NULL,
    total_congestion_duration_seconds FLOAT NOT NULL
);

-- user_area_mapping

CREATE TABLE user_area_mapping (
	id BIGINT AUTO_INCREMENT PRIMARY KEY,
	user_id BIGINT NOT NULL,
	area_name VARCHAR(50) NOT NULL,
	enabled BOOLEAN NOT NULL DEFAULT TRUE,
	created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	created_by BIGINT,
	UNIQUE KEY uk_area_name_enabled (area_name, enabled),

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    INDEX idx_user_id (user_id),
    INDEX idx_area_name (area_name),
    INDEX idx_enabled (enabled)
);

-- signal_control_logs
DROP TABLE IF EXISTS `signal_control_logs`;

CREATE TABLE signal_control_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_number VARCHAR(64) NOT NULL COMMENT 'Operator user account',
    junction_id VARCHAR(64) NOT NULL COMMENT 'Controlled signal light junction ID',
    light_index INT DEFAULT NULL COMMENT 'Controlled signal phase index',
    light_state VARCHAR(8) DEFAULT NULL COMMENT 'Signal light state (e.g., GrGr)',
    duration INT NOT NULL COMMENT 'Duration in seconds',
    operation_source VARCHAR(16) NOT NULL COMMENT 'Source: manual / ai',
    operation_result VARCHAR(20) NOT NULL COMMENT 'Result: SUCCESS / FAILURE',
    result_message VARCHAR(255) COMMENT 'Execution message or error detail',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Log creation time'
) COMMENT='Logs of signal light control operations';

-- user_permission_logs
DROP TABLE IF EXISTS `user_permission_logs`;

CREATE TABLE user_permission_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_number VARCHAR(50) NOT NULL COMMENT 'Admin account performing the action',
    target_account VARCHAR(50) NOT NULL COMMENT 'Affected user account',
    operation_type VARCHAR(20) NOT NULL COMMENT 'Type: CREATE / UPDATE / DELETE',
    operation_result VARCHAR(20) NOT NULL COMMENT 'Result: SUCCESS / FAILURE',
    result_message TEXT COMMENT 'Error or success message',
    operated_fields TEXT COMMENT 'Changed fields (e.g., JSON string or comma-separated)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Log creation time'
) COMMENT='User permission operation logs';

-- special_event_logs
DROP TABLE IF EXISTS `special_event_logs`;

CREATE TABLE special_event_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id BIGINT NOT NULL COMMENT 'ID of the scheduled special event',
    event_type VARCHAR(50) NOT NULL COMMENT 'Event type (e.g., BLOCK_LANE, ACCIDENT, EMERGENCY_VEHICLE)',
    lane_ids TEXT DEFAULT NULL COMMENT 'Affected lanes in JSON array format (nullable)',
    vehicle_id VARCHAR(64) DEFAULT NULL COMMENT 'Involved vehicle ID, for emergency vehicle scenarios',
    duration INT DEFAULT NULL COMMENT 'Event duration in seconds',
    operation_result VARCHAR(20) NOT NULL COMMENT 'Result: SUCCESS / FAILURE',
    result_message TEXT DEFAULT NULL COMMENT 'Response message from TraCI module or error detail',
    triggered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Actual time when event was triggered',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Log creation time'
) COMMENT='Logs of triggered special events';

-- auth_logs
DROP TABLE IF EXISTS `auth_logs`;

CREATE TABLE auth_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    account_number VARCHAR(50) NOT NULL COMMENT 'User account',
    operation_type VARCHAR(50) NOT NULL COMMENT 'Operation type: LOGIN / LOGOUT / RESET_PWD / VERIFY_TOKEN / FAIL',
    operation_result VARCHAR(20) NOT NULL COMMENT 'Result: SUCCESS / FAILURE',
    ip_address VARCHAR(50) COMMENT 'Client IP address',
    user_agent TEXT COMMENT 'Browser or device information',
    result_message TEXT COMMENT 'Failure reason or explanation',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT 'Log creation time'
) COMMENT='Authentication operation logs';

-- junction_regions
DROP TABLE IF EXISTS `junction_regions`;

CREATE TABLE junction_regions (
    area VARCHAR(10),
    junction_id VARCHAR(255),
    junction_name VARCHAR(255)
);

-- trafficLogic
DROP TABLE IF EXISTS trafficLogic;

CREATE TABLE trafficLogic (
    tls_id VARCHAR(255),
    junction_id VARCHAR(255),
    junction_x DOUBLE,
    junction_y DOUBLE,
    junction_shape TEXT
);

-- LANE TABLE
DROP TABLE IF EXISTS lane;

CREATE TABLE lane (
    lane_id VARCHAR(255),
    lane_shape TEXT,
    edge_id VARCHAR(255),
    edge_name VARCHAR(255)
);

-- emergency_vehicle_events
DROP TABLE IF EXISTS emergency_vehicle_events;

CREATE TABLE emergency_vehicle_events (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  event_id VARCHAR(64) NOT NULL UNIQUE COMMENT 'Unique ID of the scheduled event',
  event_type VARCHAR(32) NOT NULL COMMENT 'Type of event',
  vehicle_id VARCHAR(255) NOT NULL,
  vehicle_type VARCHAR(100) NOT NULL,
  organization VARCHAR(255) NOT NULL,
  trigger_time BIGINT NOT NULL,
  start_edge_id VARCHAR(255) NOT NULL,
  end_edge_id VARCHAR(255) NOT NULL,
  route_edges JSON NOT NULL,
  junctions_on_path JSON NOT NULL,
  signalized_junctions JSON NOT NULL,
  event_status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'event status (pending, triggered, failed, ignored, completed)',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='Scheduled emergency vehicle events';

--special_event_schedule
DROP TABLE IF EXISTS special_event_schedule;

CREATE TABLE special_event_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id VARCHAR(64) NOT NULL UNIQUE COMMENT 'Unique ID of the scheduled event',
    event_type VARCHAR(32) NOT NULL COMMENT 'Type of event (e.g., lane_closure, vehicle_stop)',
    trigger_time INT NOT NULL COMMENT 'Trigger time (simulation seconds)',
    duration INT NOT NULL COMMENT 'Duration of the event in seconds',
    lane_ids TEXT COMMENT 'Optional lane list in JSON array format',
    event_status ENUM('pending', 'triggered', 'finished', 'failed') DEFAULT 'pending' COMMENT 'Current event status',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='Scheduled Special Events';

-- junction_incoming_edges
DROP TABLE IF EXISTS junction_incoming_edges;

CREATE TABLE junction_incoming_edges (
    junction_id VARCHAR(255),
    junction_name VARCHAR(255),
    incoming_edge_id VARCHAR(255),
    incoming_edge_name VARCHAR(255),
    PRIMARY KEY (junction_id, incoming_edge_id)
);

-- junction_flow_relations
DROP TABLE IF EXISTS junction_flow_relations;

CREATE TABLE junction_flow_relations (
    junction_id VARCHAR(255),
    from_edge_id_1 VARCHAR(255),
    to_edge_id_1 VARCHAR(255),
    from_edge_id_2 VARCHAR(255),
    to_edge_id_2 VARCHAR(255),
    relationship_type VARCHAR(50)  -- 'Non-Conflicting' or 'Conflicting'
);