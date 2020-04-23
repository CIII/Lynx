# --- !Ups

INSERT INTO attributes(name, created_at, updated_at) VALUES("arrivalID", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("arrival_id", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("arrivalid", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("loc_physical_ms", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("utm_content", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("adgroupid", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("network", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("placement", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("device", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("matchtype", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("targetid", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("adposition", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("loc_interest_ms", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("feeditemid", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("url", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("adid", NOW(), NOW());

# --- !Downs

DELETE FROM attributes WHERE name="arrivalID";
DELETE FROM attributes WHERE name="arrival_id";
DELETE FROM attributes WHERE name="arrivalid";
DELETE FROM attributes WHERE name="loc_physical_ms";
DELETE FROM attributes WHERE name="utm_content";
DELETE FROM attributes WHERE name="adgroupid";
DELETE FROM attributes WHERE name="network";
DELETE FROM attributes WHERE name="placement";
DELETE FROM attributes WHERE name="device";
DELETE FROM attributes WHERE name="matchtype";
DELETE FROM attributes WHERE name="targetid";
DELETE FROM attributes WHERE name="adposition";
DELETE FROM attributes WHERE name="loc_interest_ms";
DELETE FROM attributes WHERE name="feeditemid";
DELETE FROM attributes WHERE name="url";
DELETE FROM attributes WHERE name="adid";
