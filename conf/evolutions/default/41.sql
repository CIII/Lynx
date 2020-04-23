# --- !Ups

INSERT INTO attributes(name, created_at, updated_at) VALUES('form_sequence', NOW(), NOW());

# --- !Downs

DELETE FROM attributes WHERE name='form_sequence';