# --- !Ups

INSERT INTO attributes(name, created_at, updated_at) VALUES('button_location', NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES('button_text', NOW(), NOW());

# --- !Downs

DELETE FROM attributes WHERE name='button_location';
DELETE FROM attributes WHERE name='button_text';
