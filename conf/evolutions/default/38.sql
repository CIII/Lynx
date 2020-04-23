# --- !Ups

INSERT INTO event_types(name, event_category, created_at, updated_at) VALUES('Phone Loaded', 'Form Step Load', NOW(), NOW()),
    ('Email Loaded', 'Form Step Load', NOW(), NOW())

# --- !Downs
DELETE FROM event_types WHERE name IN ('Phone Loaded', 'Email Loaded');
