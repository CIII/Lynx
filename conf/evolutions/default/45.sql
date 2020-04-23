# --- !Ups

INSERT INTO ab_test_attributes(name, created_at, updated_at) VALUES("hero_image_path", NOW(), NOW());

# --- !Downs

DELETE FROM ab_test_attributes WHERE name = "hero_image_path";