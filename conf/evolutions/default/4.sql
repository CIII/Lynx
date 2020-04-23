# easiersolar schema change

# --- !Ups

RENAME TABLE arrivals TO arrivals_copy;

CREATE TABLE ab_tests (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    link VARCHAR(255) NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);


CREATE TABLE domains (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    domain VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE traffic_types (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE traffic_sources (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    traffic_type_id BIGINT(20) NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX traffic_type_index (traffic_type_id)
);

CREATE TABLE browsers (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    user_agent VARCHAR(255) NULL,
    ip VARCHAR(255) NULL,
    os VARCHAR(50) NULL,
    robot_id VARCHAR(50) NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE endpoints (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    ip_address VARCHAR(50) NULL,
    ip_normalized VARCHAR(50),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE browser_endpoints (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    browser_id BIGINT(20) NULL,
    endpoint_id BIGINT(20),
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX browser_index (browser_id),
    INDEX endpoint_index (endpoint_id)
);

CREATE TABLE event_types (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    event_category VARCHAR(50) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE urls (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  url VARCHAR(255) NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);

CREATE TABLE arrivals (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  arrival_id VARCHAR(255) NOT NULL,
  traffic_source_id BIGINT(20) NULL,
  arpxs_a_ref VARCHAR(255) NULL,
  arpxs_b VARCHAR(255) NULL,
  gclid VARCHAR(255) NULL,
  browser_id BIGINT(20) NULL,
  domain_id BIGINT(20) NULL,
  referer VARCHAR(255) NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX traffic_source_index (traffic_source_id),
  INDEX browser_index (browser_id),
  INDEX domain_index (domain_id)
);

CREATE TABLE events (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  event_type_id BIGINT(20) NOT NULL,
  arrival_id BIGINT(20) NOT NULL,
  url_id BIGINT(20) NOT NULL,
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX event_type_index (event_type_id),
  INDEX arrival_index (arrival_id),
  INDEX url_index (url_id)
);

CREATE TABLE forms (
  id BIGINT(20) NOT NULL AUTO_INCREMENT,
  arrival_id BIGINT(20) NOT NULL,
  event_id BIGINT(20) NULL,
  first_name VARCHAR(50) NULL,
  last_name VARCHAR(50) NULL,
  email VARCHAR(255) NULL,
  street VARCHAR(100),
  city VARCHAR(50) NULL,
  state VARCHAR(50) NULL,
  zip VARCHAR(10) NULL,
  property_ownership VARCHAR(10) NULL,
  electric_bill VARCHAR(255) NULL,
  electric_company VARCHAR(255) NULL,
  phone_home VARCHAR(100) NULL,
  leadid_token VARCHAR(255) NULL,
  domtok VARCHAR(255),
  ref VARCHAR(50),
  created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  INDEX arrival_index (arrival_id),
  INDEX event_index (event_id)
);

CREATE TABLE ab_arrivals (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    arrival_id BIGINT(20) NOT NULL,
    ab_test_id BIGINT(20) NOT NULL,
    arrival_form_id BIGINT(20) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX arrival_index (arrival_id),
    INDEX ab_test_index (ab_test_id)
);

INSERT INTO event_types (name, event_category) VALUES
  ('LP CTC', 'CTC Event'),
  ('Form Step 1', 'Form Step Submit'),
  ('Form Step 2', 'Form Step Submit'),
  ('Form Step 3', 'Form Step Submit'),
  ('Form Complete', 'Form Submit'),
  ('LP Content Engage', 'Engage'),
  ('Page Closed', 'Engage');

INSERT INTO domains (domain) VALUES
  ('easiersolar.com');

INSERT INTO urls (url) VALUES
  ('easiersolar.com');

INSERT INTO arrivals (arrival_id, arpxs_a_ref, arpxs_b, domain_id)
SELECT arrival_id, arpxs_a_ref, arpxs_b, 1 FROM arrivals_copy;

INSERT INTO forms (
arrival_id,
first_name,
last_name,
email,
street,
city,
state,
zip,
property_ownership,
electric_bill,
electric_company,
phone_home,
leadid_token,
domtok,
ref
)
SELECT
ac.id,
ac.first_name,
ac.last_name,
ac.email,
ac.street,
ac.city,
ac.state,
ac.zip,
ac.property_ownership,
ac.electric_bill,
ac.electric_company,
ac.phone_home,
ac.leadid_token,
ac.domtok,
ac.ref
FROM arrivals_copy as ac;

INSERT INTO events (event_type_id, arrival_id, url_id)
SELECT (SELECT id from event_types WHERE name = 'Form Complete' LIMIT 1), id, (SELECT id FROM urls WHERE url = 'easiersolar.com' LIMIT 1)
 FROM arrivals;

# --- !Downs

DROP TABLE ab_arrivals;
DROP TABLE ab_tests;
DROP TABLE browser_endpoints;
DROP TABLE endpoints;
DROP TABLE forms;
DROP TABLE events;
DROP TABLE arrivals;
DROP TABLE browsers;
DROP TABLE traffic_sources;
DROP TABLE traffic_types;
DROP TABLE domains;
DROP TABLE event_types;
DROP TABLE urls;
RENAME TABLE arrivals_copy TO arrivals;