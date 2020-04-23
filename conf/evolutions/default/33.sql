# --- !Ups

INSERT INTO event_types(name, event_category, created_at, updated_at) VALUES('Credit Loaded', 'Form Step Load', NOW(), NOW()),
    ('First and Last Name Loaded', 'Form Step Load', NOW(), NOW())

# --- !Downs
DELETE FROM event_types WHERE name IN ('Credit Loaded', 'First and Last Name Loaded');
