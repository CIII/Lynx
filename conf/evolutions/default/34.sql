# --- !Ups

INSERT INTO attributes(name, created_at, updated_at) VALUES('scroll_top', NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES('scroll_bottom', NOW(), NOW());

# --- !Downs

DELETE FROM attributes WHERE name='scroll_top';
DELETE FROM attributes WHERE name='scroll_bottom';