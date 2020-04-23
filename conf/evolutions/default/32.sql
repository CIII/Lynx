# --- !Ups

ALTER TABLE `ab_test_members` ADD COLUMN `session_id` VARCHAR(45) NULL AFTER `event_id`;

# --- !Downs

ALTER TABLE `ab_test_members` DROP COLUMN `session_id`;