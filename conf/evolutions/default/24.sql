# --- !Ups

DROP TABLE IF EXISTS ab_tests;

CREATE TABLE ab_tests (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

DROP TABLE IF EXISTS ab_test_members;

CREATE TABLE ab_test_members (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    event_id BIGINT(20) NOT NULL,
    ab_test_id BIGINT(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

DROP TABLE IF EXISTS ab_test_attributes;

CREATE TABLE ab_test_attributes (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

DROP TABLE IF EXISTS ab_test_member_attributes;

CREATE TABLE ab_test_member_attributes (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    ab_test_member_id  BIGINT(20) NOT NULL,
    ab_test_attribute_id  BIGINT(20) NOT NULL,
    value  VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

INSERT INTO ab_test_attributes(name, created_at, updated_at) VALUES("template_id", NOW(), NOW());
INSERT INTO ab_test_attributes(name, created_at, updated_at) VALUES("state_header", NOW(), NOW());

# --- !Downs

DROP TABLE IF EXISTS ab_tests;
DROP TABLE IF EXISTS ab_test_members;
DROP TABLE IF EXISTS ab_test_attributes;
DROP TABLE IF EXISTS ab_test_member_attributes;