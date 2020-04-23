# arrivals schema change

# --- !Ups

RENAME TABLE Arrival TO arrivals;
ALTER TABLE arrivals ADD created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE arrivals ADD updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE arrivals MODIFY arrival_id VARCHAR(500) NOT NULL;

# --- !Downs

ALTER TABLE arrivals DROP COLUMN created_at;
ALTER TABLE arrivals DROP COLUMN updated_at;
RENAME TABLE arrivals TO Arrival;