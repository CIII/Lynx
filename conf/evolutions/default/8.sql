# add event type change

# --- !Ups

INSERT INTO event_types (name, event_category) VALUES('Form Step 2B', 'Form Step Submit');


# --- !Downs

DELETE FROM event_types WHERE name = 'Form Step 2B';