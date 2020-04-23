# --- !Ups

INSERT INTO attributes (name) VALUES ('mouse_x'),('mouse_y');

# --- !Downs

DELETE FROM attributes WHERE name = "mouse_x" OR name = "mouse_y";
