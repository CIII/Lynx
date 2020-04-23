# forms schema change

# --- !Ups

ALTER TABLE forms ADD post_status SMALLINT(3) NULL AFTER ref;

UPDATE forms AS f, arrivals AS arr, arrivals_copy AS arrcpy
SET f.post_status = CAST(arrcpy.post_status AS UNSIGNED)
WHERE arr.arrival_id = arrcpy.arrival_id AND f.arrival_id = arr.id;

# --- !Downs

ALTER TABLE forms DROP COLUMN post_status;
