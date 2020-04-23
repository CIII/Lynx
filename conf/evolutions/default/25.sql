# --- !Ups

INSERT INTO attributes(name, created_at, updated_at) VALUES('fire_error', NOW(), NOW());

# --- !Downs

DELETE FROM attributes WHERE name = "fire_error";