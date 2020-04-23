# urls schema change

# --- !Ups

ALTER TABLE urls MODIFY url VARCHAR(2083) NOT NULL;


# --- !Downs

ALTER TABLE urls MODIFY url VARCHAR(255) NOT NULL;