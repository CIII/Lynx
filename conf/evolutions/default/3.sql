# arrivals schema change

# --- !Ups

ALTER TABLE arrivals ADD campid  VARCHAR(255) NULL;
ALTER TABLE arrivals ADD xxTrustedFormToken VARCHAR(255) NULL;
ALTER TABLE arrivals ADD hid VARCHAR(255) NULL;
ALTER TABLE arrivals ADD gclid VARCHAR(255) NULL;
ALTER TABLE arrivals ADD arpxs_a_ref VARCHAR(255) NULL;
ALTER TABLE arrivals ADD arpxs_b VARCHAR(255) NULL;


# --- !Downs

ALTER TABLE arrivals DROP COLUMN campid;
ALTER TABLE arrivals DROP COLUMN xxTrustedFormToken;
ALTER TABLE arrivals DROP COLUMN hid;
ALTER TABLE arrivals DROP COLUMN gclid;
ALTER TABLE arrivals DROP COLUMN arpxs_a_ref;
ALTER TABLE arrivals DROP COLUMN arpxs_b;
