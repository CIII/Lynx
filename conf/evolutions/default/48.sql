# --- !Ups

DROP TABLE IF EXISTS leads;

CREATE TABLE leads (
  lead_id bigint(20) NOT NULL,
  user_id VARCHAR(8) NULL,
  session_id bigint(20) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (lead_id),
  FOREIGN KEY (session_id) REFERENCES sessions (id));
# --- !Downs

DROP TABLE IF EXISTS leads;