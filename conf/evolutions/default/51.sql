# --- !Ups

DROP TABLE IF EXISTS config_user_account;

CREATE TABLE config_user_account (
  `id` INT NOT NULL AUTO_INCREMENT,
  `user_name` VARCHAR(50) NULL,
  `password` VARCHAR(50) NULL,
  PRIMARY KEY (`id`));

# --- !Downs

DROP TABLE IF EXISTS config_user_account;