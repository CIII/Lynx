# --- !Ups

INSERT INTO event_types(name, event_category, created_at, updated_at) VALUES('Form Step Loaded', 'Form Step Load', NOW(), NOW()), ('Page Scroll', 'Page', NOW(), NOW()), ('Page Mouse Movement', 'Page', NOW(), NOW());
INSERT INTO attributes(name, created_at, updated_at) VALUES('form_type', NOW(), NOW());

# --- !Downs

DELETE FROM event_types WHERE name IN ("Form Step Loaded", "Page Scroll", "Page Mouse Movement");
DELETE FROM attributes WHERE name = "form_type";
