# --- !Ups

INSERT INTO attributes(name, created_at, updated_at) VALUES('robot_detection_ran', NOW(), NOW());

# --- !Downs

DELETE FROM attributes WHERE name = "robot_detection_ran";
