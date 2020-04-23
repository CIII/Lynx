# --- !Ups

CREATE TABLE IF NOT EXISTS utilities (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    name varchar(255) NOT NULL,
    state varchar(2) NOT NULL,
    sum_of_customers INT NOT NULL,
    sum_of_sales BIGINT(20) NOT NULL,
    sum_of_revenues BIGINT(20) NOT NULL,
    average_kwh_per_user DECIMAL,
    cost FLOAT NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS cost_by_states (
    state varchar(2) NOT NULL,
    sum_of_customers INT NOT NULL,
    sum_of_sales BIGINT(20) NOT NULL,
    sum_of_revenues BIGINT(20) NOT NULL,
    average_kwh_per_user DECIMAL,
    cost FLOAT NOT NULL,
    PRIMARY KEY (state)
);

CREATE TABLE IF NOT EXISTS solar_yields (
    zip_code INT NOT NULL,
    ac_monthly_1 FLOAT,
    ac_monthly_2 FLOAT,
    ac_monthly_3 FLOAT,
    ac_monthly_4 FLOAT,
    ac_monthly_5 FLOAT,
    ac_monthly_6 FLOAT,
    ac_monthly_7 FLOAT,
    ac_monthly_8 FLOAT,
    ac_monthly_9 FLOAT,
    ac_monthly_10 FLOAT,
    ac_monthly_11 FLOAT,
    ac_monthly_12 FLOAT,
    ac_annual FLOAT,
    solrad_annual FLOAT,
    capacity_factor FLOAT,
    PRIMARY KEY(zip_code)
);

# srec in dollar/megawatts
CREATE TABLE IF NOT EXISTS SRECs (
    state VARCHAR(2) NOT NULL,
    srec INT NOT NULL,
    PRIMARY KEY(state)
);

CREATE TABLE IF NOT EXISTS system_costs (
    state VARCHAR(2) NOT NULL,
    low FLOAT NOT NULL,
    high FLOAT NOT NULL,
    PRIMARY KEY(state)
);

# --- !Downs

DROP TABLE IF EXISTS utility;
DROP TABLE IF EXISTS solar_yields;
DROP TABLE IF EXISTS cost_by_states;
DROP TABLE IF EXISTS SREC;
DROP TABLE IF EXISTS system_cost;
