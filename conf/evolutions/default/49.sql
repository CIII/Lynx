# --- !Ups

INSERT INTO event_types (name, event_category) VALUES ('Page Loading', 'Page');

# --- !Downs

DELETE FROM event_types WHERE name = "Page Loading";
