# --- !Ups

CREATE TABLE IF NOT EXISTS revenues (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    session_id BIGINT(20) NOT NULL,
    total_revenue DECIMAL(9,2) NOT NULL DEFAULT 0.0,
    con_f BIGINT(20) NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

INSERT INTO attributes(name, created_at, updated_at) VALUES('git_hash', NOW(), NOW());

# --- !Downs

DROP TABLE IF EXISTS revenues;

DELETE FROM attributes WHERE name='git_hash';
