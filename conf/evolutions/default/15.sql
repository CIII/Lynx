# --- !Ups

INSERT INTO attributes(name, created_at, updated_at) VALUES("campaignid", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("creative", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("keyword", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("utm_medium", NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES("utm_term", NOW(), NOW());

# --- !Downs

DELETE FROM attributes WHERE name="campaignid";
DELETE FROM attributes WHERE name="creative";
DELETE FROM attributes WHERE name="keyword";
DELETE FROM attributes WHERE name="utm_medium";
DELETE FROM attributes WHERE name="utm_term";