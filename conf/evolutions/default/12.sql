# --- !Ups

ALTER TABLE events MODIFY parent_event_id BIGINT(20)
# --- !Downs

ALTER TABLE events MODIFY parent_event_id BIGINT(20) NOT NULL

