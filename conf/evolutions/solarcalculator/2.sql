# --- !Ups

CREATE TABLE IF NOT EXISTS cost_by_zips (
    zip_code INT NOT NULL,
    cost FLOAT NOT NULL,
    PRIMARY KEY (zip_code)
);

# --- !Downs

DROP TABLE IF EXISTS cost_by_zips;
