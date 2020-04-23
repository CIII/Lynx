# --- !Ups

INSERT INTO event_types (name, event_category ) VALUES ("Page Rendered", "Page")
# --- !Downs

DELETE FROM even_types WHERE name = "Page Rendered"
