# --- !Ups

INSERT INTO event_types(name, event_category, created_at, updated_at) VALUES ('Thank You Loaded', 'Form Step Load', NOW(), NOW());

# --- !Downs
DELETE FROM event_types WHERE name IN ('Thank You Loaded');