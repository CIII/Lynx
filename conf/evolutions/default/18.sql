# --- !Ups

INSERT INTO attributes(name, created_at, updated_at) VALUES('page_type', NOW(), NOW());

# --- !Downs

DELETE FROM attributes WHERE name='page_type';
