# --- !Ups

INSERT INTO event_types (name, event_category) VALUES ('Page View', 'Page');

# --- !Downs

DELETE FROM event_types WHERE name = "Page View";
