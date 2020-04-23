# --- !Ups

ALTER TABLE cost_by_zips ADD state varchar(2) NOT NULL;

# --- !Downs

ALTER TABLE tbl_Country DROP COLUMN state;