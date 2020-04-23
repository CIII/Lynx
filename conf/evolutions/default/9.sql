# --- !Ups

ALTER TABLE arrivals ADD arpxs_abv VARCHAR(255) NULL AFTER arpxs_b;
ALTER TABLE arrivals ADD utm_source VARCHAR(255) NULL AFTER arpxs_abv;
ALTER TABLE arrivals ADD utm_campaign VARCHAR(255) NULL AFTER utm_source;

# --- !Downs

ALTER TABLE arrivals DROP COLUMN arpxs_abv;
ALTER TABLE arrivals DROP COLUMN utm_source;
ALTER TABLE arrivals DROP COLUMN utm_campaign;
