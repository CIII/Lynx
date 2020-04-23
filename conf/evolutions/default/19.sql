# --- !Ups

ALTER TABLE sessions ADD COLUMN `last_activity` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER `updated_at`;
# --- !Downs

ALTER TABLE sessions DROP COLUMN `last_activity`;
