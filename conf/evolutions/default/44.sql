# --- !Ups

INSERT INTO attributes(name, created_at, updated_at) VALUES('maxmind_state', NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES('maxmind_country', NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES('maxmind_city', NOW(), NOW());

# --- !Downs

DELETE FROM attributes WHERE name='maxmind_state';
DELETE FROM attributes WHERE name='maxmind_country';
DELETE FROM attributes WHERE name='maxmind_city';
