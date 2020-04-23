# forms schema change

# --- !Ups

ALTER TABLE forms ADD xxTrustedFormToken VARCHAR(255) NULL AFTER ref;
ALTER TABLE forms ADD xxTrustedFormCertUrl VARCHAR(255) NULL AFTER ref;

UPDATE forms AS f, arrivals AS arr, arrivals_copy AS arrcpy
SET f.xxTrustedFormToken = arrcpy.xxTrustedFormToken, f.xxTrustedFormCertUrl = arrcpy.xxTrustedFormCertUrl
WHERE arr.arrival_id = arrcpy.arrival_id AND f.arrival_id = arr.id;

INSERT INTO event_types (name, event_category) VALUES ('Page Loaded', 'Page');

UPDATE event_types SET event_category = 'Page' WHERE name = 'Page Closed';

UPDATE events SET event_type_id = (SELECT id FROM event_types WHERE name = 'Page Loaded' LIMIT 1) WHERE event_type_id = 6;

# --- !Downs

ALTER TABLE forms DROP COLUMN xxTrustedFormToken;
ALTER TABLE forms DROP COLUMN xxTrustedFormCertUrl;