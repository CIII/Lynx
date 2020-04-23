# --- !Ups

INSERT INTO event_types (name, event_category) VALUES ('Page Mouse Click', 'Page');

# --- !Downs

DELETE FROM event_types WHERE name = "Page Mouse Click";
