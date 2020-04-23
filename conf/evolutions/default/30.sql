# --- !Ups

INSERT INTO attributes(name, created_at, updated_at) VALUES('maxmind_zip', NOW(), NOW());

# --- !Downs

DELETE FROM attributes WHERE name = "maxmind_zip";