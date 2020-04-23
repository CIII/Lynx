# --- !Ups

ALTER TABLE sessions MODIFY user_agent varchar(500);

# --- !Downs

ALTER TABLE sessions MODIFY user_agent varchar(255);