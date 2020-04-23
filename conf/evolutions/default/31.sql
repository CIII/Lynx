# --- !Ups

DELETE FROM event_types WHERE name = "Form Step Loaded";
INSERT INTO event_types(name, event_category, created_at, updated_at) VALUES('Address Loaded', 'Form Step Load', NOW(), NOW()),
    ('Ownership Loaded', 'Form Step Load', NOW(), NOW()), ('Power Bill Loaded', 'Form Step Load', NOW(), NOW()),
    ('Power Company Loaded', 'Form Step Load', NOW(), NOW()), ('Name Loaded', 'Form Step Load', NOW(), NOW()),
    ('Email and Phone Loaded', 'Form Step Load', NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES('local_hour', NOW(), NOW());

# --- !Downs

INSERT INTO event_types(name, event_category, created_at, updated_at) VALUES('Form Step Loaded', 'Form Step Load', NOW(), NOW());
DELETE FROM event_types WHERE name IN ("Address Loaded", "Ownership Loaded", 'Power Bill Loaded', 'Power Company Loaded', 'Name Loaded', 'Email and Phone Loaded');
DELETE FROM attributes WHERE name = "local_hour";