CREATE TABLE text_pool (
   id                       BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT,
   text_key                 VARCHAR(255) NOT NULL,
   text_value               MEDIUMTEXT NOT NULL
);