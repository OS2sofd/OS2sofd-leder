CREATE TABLE feature_settings (
   id              BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
   setting_key     VARCHAR(255) NOT NULL UNIQUE,
   setting_value   VARCHAR(4096) NULL
);
