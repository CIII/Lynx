# --- !Ups

INSERT INTO event_types(name, event_category, created_at, updated_at) VALUES('Page Focus', 'Page', NOW(), NOW()),('Page Blur', 'Page', NOW(), NOW());

# --- !Downs

DELETE FROM event_types WHERE name IN ("Page Focus", "Page Blur");
