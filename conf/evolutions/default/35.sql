# --- !Ups

INSERT INTO ab_test_attributes(name, created_at, updated_at) VALUES("sequence", NOW(), NOW());

# --- !Downs

DELETE FROM ab_test_attributes WHERE name = "sequence";