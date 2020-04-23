# --- !Ups

INSERT INTO domains (domain, default_template_id) VALUES
  ('homesolar.pro', 0);

# --- !Downs

DELETE FROM domains WHERE domain = 'homesolar.pro';
