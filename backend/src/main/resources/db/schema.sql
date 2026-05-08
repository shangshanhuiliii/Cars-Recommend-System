CREATE TABLE IF NOT EXISTS app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(64),
    phone VARCHAR(32),
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT ck_app_user_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE TABLE IF NOT EXISTS admin (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'ADMIN',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_admin_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS car_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    brand VARCHAR(64) NOT NULL,
    series VARCHAR(64) NOT NULL,
    model_name VARCHAR(128) NOT NULL,
    guide_price DECIMAL(12, 2) NOT NULL,
    body_type VARCHAR(16) NOT NULL,
    energy_type VARCHAR(16) NOT NULL,
    seats INT NOT NULL,
    launch_year INT,
    image_url VARCHAR(512),
    sales_volume INT NOT NULL DEFAULT 0,
    user_rating DECIMAL(3, 2) NOT NULL DEFAULT 0.00,
    audit_status VARCHAR(16) NOT NULL DEFAULT 'APPROVED',
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_car_model_body_type CHECK (body_type IN ('SUV', '轿车', 'MPV')),
    CONSTRAINT ck_car_model_energy_type CHECK (energy_type IN ('燃油', '纯电', '插混', '增程')),
    CONSTRAINT ck_car_model_seats CHECK (seats BETWEEN 2 AND 9),
    CONSTRAINT ck_car_model_user_rating CHECK (user_rating >= 0 AND user_rating <= 5),
    CONSTRAINT ck_car_model_audit_status CHECK (audit_status IN ('APPROVED', 'PENDING', 'REJECTED'))
);

CREATE INDEX idx_car_model_brand ON car_model (brand);
CREATE INDEX idx_car_model_body_type ON car_model (body_type);
CREATE INDEX idx_car_model_energy_type ON car_model (energy_type);
CREATE INDEX idx_car_model_guide_price ON car_model (guide_price);

CREATE TABLE IF NOT EXISTS car_image_asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    car_id BIGINT NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    size_bytes BIGINT NOT NULL,
    width INT NOT NULL,
    height INT NOT NULL,
    public_url VARCHAR(512) NOT NULL,
    storage_path VARCHAR(1000) NOT NULL,
    checksum CHAR(64) NOT NULL,
    audit_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500),
    created_by_admin_id BIGINT NOT NULL,
    reviewed_by_admin_id BIGINT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    review_time TIMESTAMP,
    CONSTRAINT fk_car_image_asset_car_id FOREIGN KEY (car_id) REFERENCES car_model (id),
    CONSTRAINT fk_car_image_asset_created_by_admin_id FOREIGN KEY (created_by_admin_id) REFERENCES admin (id),
    CONSTRAINT fk_car_image_asset_reviewed_by_admin_id FOREIGN KEY (reviewed_by_admin_id) REFERENCES admin (id),
    CONSTRAINT uk_car_image_asset_stored_filename UNIQUE (stored_filename),
    CONSTRAINT ck_car_image_asset_content_type CHECK (content_type IN ('image/jpeg', 'image/png')),
    CONSTRAINT ck_car_image_asset_size CHECK (size_bytes > 0),
    CONSTRAINT ck_car_image_asset_dimension CHECK (width > 0 AND height > 0),
    CONSTRAINT ck_car_image_asset_audit_status CHECK (audit_status IN ('APPROVED', 'PENDING', 'REJECTED'))
);

CREATE INDEX idx_car_image_asset_car_id ON car_image_asset (car_id);
CREATE INDEX idx_car_image_asset_audit_status ON car_image_asset (audit_status);
CREATE INDEX idx_car_image_asset_car_status ON car_image_asset (car_id, audit_status);

CREATE TABLE IF NOT EXISTS car_param (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    car_id BIGINT NOT NULL,
    length_mm INT NOT NULL,
    width_mm INT NOT NULL,
    height_mm INT NOT NULL,
    wheelbase_mm INT NOT NULL,
    fuel_consumption DECIMAL(5, 2),
    electric_consumption DECIMAL(5, 2),
    electric_range_km INT,
    total_range_km INT,
    acceleration_100 DECIMAL(4, 1),
    airbag_count INT NOT NULL DEFAULT 0,
    has_abs BOOLEAN NOT NULL DEFAULT FALSE,
    has_esp BOOLEAN NOT NULL DEFAULT FALSE,
    has_active_brake BOOLEAN NOT NULL DEFAULT FALSE,
    has_lane_keep BOOLEAN NOT NULL DEFAULT FALSE,
    has_adaptive_cruise BOOLEAN NOT NULL DEFAULT FALSE,
    has_blind_spot BOOLEAN NOT NULL DEFAULT FALSE,
    has_reverse_camera BOOLEAN NOT NULL DEFAULT FALSE,
    has_360_camera BOOLEAN NOT NULL DEFAULT FALSE,
    has_ota BOOLEAN NOT NULL DEFAULT FALSE,
    has_voice_control BOOLEAN NOT NULL DEFAULT FALSE,
    has_auto_parking BOOLEAN NOT NULL DEFAULT FALSE,
    screen_size DECIMAL(4, 2),
    assist_drive_level VARCHAR(16),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_car_param_car_id UNIQUE (car_id),
    CONSTRAINT fk_car_param_car_id FOREIGN KEY (car_id) REFERENCES car_model (id),
    CONSTRAINT ck_car_param_length CHECK (length_mm > 0),
    CONSTRAINT ck_car_param_width CHECK (width_mm > 0),
    CONSTRAINT ck_car_param_height CHECK (height_mm > 0),
    CONSTRAINT ck_car_param_wheelbase CHECK (wheelbase_mm > 0)
);

CREATE TABLE IF NOT EXISTS car_feature_score (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    car_id BIGINT NOT NULL,
    space_score DECIMAL(5, 2) NOT NULL,
    safety_score DECIMAL(5, 2) NOT NULL,
    energy_score DECIMAL(5, 2) NOT NULL,
    intelligence_score DECIMAL(5, 2) NOT NULL,
    comfort_score DECIMAL(5, 2) NOT NULL,
    power_score DECIMAL(5, 2) NOT NULL,
    reputation_score DECIMAL(5, 2) NOT NULL,
    popularity_score DECIMAL(5, 2) NOT NULL,
    score_version VARCHAR(32) NOT NULL,
    calculated_time TIMESTAMP NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_car_feature_score_car_id UNIQUE (car_id),
    CONSTRAINT fk_car_feature_score_car_id FOREIGN KEY (car_id) REFERENCES car_model (id),
    CONSTRAINT ck_car_feature_score_space CHECK (space_score BETWEEN 0 AND 100),
    CONSTRAINT ck_car_feature_score_safety CHECK (safety_score BETWEEN 0 AND 100),
    CONSTRAINT ck_car_feature_score_energy CHECK (energy_score BETWEEN 0 AND 100),
    CONSTRAINT ck_car_feature_score_intelligence CHECK (intelligence_score BETWEEN 0 AND 100),
    CONSTRAINT ck_car_feature_score_comfort CHECK (comfort_score BETWEEN 0 AND 100),
    CONSTRAINT ck_car_feature_score_power CHECK (power_score BETWEEN 0 AND 100),
    CONSTRAINT ck_car_feature_score_reputation CHECK (reputation_score BETWEEN 0 AND 100),
    CONSTRAINT ck_car_feature_score_popularity CHECK (popularity_score BETWEEN 0 AND 100)
);

CREATE TABLE IF NOT EXISTS user_demand (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    raw_text VARCHAR(1000),
    budget_min DECIMAL(12, 2),
    budget_max DECIMAL(12, 2),
    body_types JSON,
    energy_types JSON,
    min_seats INT,
    scenes JSON,
    factor_weights JSON,
    excluded_brands JSON,
    excluded_car_ids JSON,
    profile_text VARCHAR(500),
    weight_price DECIMAL(6, 4) NOT NULL DEFAULT 0.0000,
    weight_space DECIMAL(6, 4) NOT NULL DEFAULT 0.0000,
    weight_safety DECIMAL(6, 4) NOT NULL DEFAULT 0.0000,
    weight_energy DECIMAL(6, 4) NOT NULL DEFAULT 0.0000,
    weight_intelligence DECIMAL(6, 4) NOT NULL DEFAULT 0.0000,
    weight_comfort DECIMAL(6, 4) NOT NULL DEFAULT 0.0000,
    weight_power DECIMAL(6, 4) NOT NULL DEFAULT 0.0000,
    weight_reputation DECIMAL(6, 4) NOT NULL DEFAULT 0.0000,
    weight_popularity DECIMAL(6, 4) NOT NULL DEFAULT 0.0000,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_demand_user_id FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT ck_user_demand_min_seats CHECK (min_seats IS NULL OR min_seats BETWEEN 2 AND 9)
);

CREATE INDEX idx_user_demand_user_id ON user_demand (user_id);

CREATE TABLE IF NOT EXISTS recommend_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    demand_id BIGINT NOT NULL,
    profile_text VARCHAR(500) NOT NULL,
    weight_snapshot JSON NOT NULL,
    fallback_message VARCHAR(500),
    recommend_status VARCHAR(16) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recommend_record_user_id FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_recommend_record_demand_id FOREIGN KEY (demand_id) REFERENCES user_demand (id),
    CONSTRAINT ck_recommend_record_status CHECK (recommend_status IN ('SUCCESS', 'FALLBACK', 'EMPTY'))
);

CREATE INDEX idx_recommend_record_user_id ON recommend_record (user_id);
CREATE INDEX idx_recommend_record_demand_id ON recommend_record (demand_id);

CREATE TABLE IF NOT EXISTS recommend_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    record_id BIGINT NOT NULL,
    car_id BIGINT NOT NULL,
    rank_no INT NOT NULL,
    total_score DECIMAL(6, 2) NOT NULL,
    price_score DECIMAL(6, 2) NOT NULL,
    space_score DECIMAL(5, 2) NOT NULL,
    safety_score DECIMAL(5, 2) NOT NULL,
    energy_score DECIMAL(5, 2) NOT NULL,
    intelligence_score DECIMAL(5, 2) NOT NULL,
    comfort_score DECIMAL(5, 2) NOT NULL,
    power_score DECIMAL(5, 2) NOT NULL,
    reputation_score DECIMAL(5, 2) NOT NULL,
    popularity_score DECIMAL(5, 2) NOT NULL,
    tags JSON NOT NULL,
    match_level VARCHAR(32) NOT NULL,
    reason_text VARCHAR(1000) NOT NULL,
    weakness_text VARCHAR(1000) NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recommend_item_record_id FOREIGN KEY (record_id) REFERENCES recommend_record (id),
    CONSTRAINT fk_recommend_item_car_id FOREIGN KEY (car_id) REFERENCES car_model (id),
    CONSTRAINT ck_recommend_item_match_level CHECK (match_level IN ('STRICT', 'RELAX_BUDGET', 'RELAX_BODY_TYPE', 'RELAX_ENERGY_TYPE', 'SIMILAR_RECOMMEND')),
    CONSTRAINT ck_recommend_item_total_score CHECK (total_score BETWEEN 0 AND 100),
    CONSTRAINT ck_recommend_item_price_score CHECK (price_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_recommend_item_record_id ON recommend_item (record_id);
CREATE INDEX idx_recommend_item_car_id ON recommend_item (car_id);

CREATE TABLE IF NOT EXISTS user_favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    car_id BIGINT NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_favorite_user_id FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_user_favorite_car_id FOREIGN KEY (car_id) REFERENCES car_model (id),
    CONSTRAINT uk_user_favorite_user_car UNIQUE (user_id, car_id)
);

CREATE INDEX idx_user_favorite_user_id ON user_favorite (user_id);
CREATE INDEX idx_user_favorite_car_id ON user_favorite (car_id);

CREATE TABLE IF NOT EXISTS user_compare_car (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    car_id BIGINT NOT NULL,
    sort_no INT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_compare_car_user_id FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_user_compare_car_car_id FOREIGN KEY (car_id) REFERENCES car_model (id),
    CONSTRAINT uk_user_compare_car UNIQUE (user_id, car_id)
);

CREATE INDEX idx_user_compare_user ON user_compare_car (user_id, deleted, sort_no, update_time);
CREATE INDEX idx_user_compare_car_id ON user_compare_car (car_id);

CREATE TABLE IF NOT EXISTS recommend_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    record_id BIGINT NOT NULL,
    satisfaction_score INT NOT NULL,
    satisfaction_level VARCHAR(16) NOT NULL,
    reason_tags JSON,
    comment VARCHAR(500),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_recommend_feedback_user_id FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_recommend_feedback_record_id FOREIGN KEY (record_id) REFERENCES recommend_record (id),
    CONSTRAINT uk_recommend_feedback_user_record UNIQUE (user_id, record_id),
    CONSTRAINT ck_recommend_feedback_score CHECK (satisfaction_score BETWEEN 1 AND 5),
    CONSTRAINT ck_recommend_feedback_level CHECK (satisfaction_level IN ('SATISFIED', 'NEUTRAL', 'DISSATISFIED'))
);

CREATE INDEX idx_recommend_feedback_user_id ON recommend_feedback (user_id);
CREATE INDEX idx_recommend_feedback_record_id ON recommend_feedback (record_id);
