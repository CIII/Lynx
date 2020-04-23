# --- !Ups

INSERT INTO attributes(name, created_at, updated_at) VALUES("user_agent", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("ip_address", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("os", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("arpxs_a_ref", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("arpxs_b", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("arpxs_abv", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("utm_source", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("utm_campaign", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("gclid", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("referer", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("request_url", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("robot_id", NOW(), NOW());

# --- !Downs

DELETE FROM attributes WHERE name="user_agent";
DELETE FROM attributes WHERE name="ip_address";
DELETE FROM attributes WHERE name="os";
DELETE FROM attributes WHERE name="arpxs_a_ref";
DELETE FROM attributes WHERE name="arpxs_b";
DELETE FROM attributes WHERE name="arpxs_abv";
DELETE FROM attributes WHERE name="utm_source";
DELETE FROM attributes WHERE name="utm_campaign";
DELETE FROM attributes WHERE name="gclid";
DELETE FROM attributes WHERE name="referer";
DELETE FROM attributes WHERE name="request_url";
DELETE FROM attributes WHERE name="robot_id";

