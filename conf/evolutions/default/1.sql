# Arrivals schema

# --- !Ups

CREATE TABLE Arrival (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    arrival_id bigint(20) NOT NULL,
    first_name varchar(255) NULL,
    last_name varchar(255) NULL,
    email varchar(255) NULL,
    zip varchar(255) NULL,
    city varchar(255) NULL,
    state varchar(255) NULL,
    street varchar(255) NULL,
    property_ownership varchar(255) NULL,
    electric_bill varchar(255) NULL,
    electric_company varchar(255) NULL,
    phone_home varchar(255) NULL,
    leadid_token varchar(255) NULL,
    xxTrustedFormCertUrl varchar(255) NULL,
    listid varchar(255) NULL,
    domtok varchar(255) NULL,
    ref varchar(255) NULL,
    post_status varchar(255) NULL,
    PRIMARY KEY (id)
);

# --- !Downs

DROP TABLE Arrival;