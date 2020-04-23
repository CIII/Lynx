# --- !Ups

ALTER TABLE forms ADD COLUMN dob DATE AFTER xxTrustedFormToken, add column full_name varchar(100) after last_name;
create trigger form_created_at before insert on forms for each row begin set new.created_at = CURRENT_TIMESTAMP(), new.updated_at = CURRENT_TIMESTAMP();; end;
create trigger form_updated_at before update on forms for each row begin set new.updated_at = CURRENT_TIMESTAMP();; end;

# --- !Downs

ALTER TABLE forms DROP COLUMN dob, DROP COLUMN full_name;
DROP TRIGGER IF EXISTS form_created_at;
DROP TRIGGER IF EXISTS form_updated_at;