# --- !Ups

INSERT INTO ab_test_attributes(name, created_at, updated_at) VALUES("form1_load_rule", NOW(), NOW());
INSERT INTO ab_test_attributes(name, created_at, updated_at) VALUES("form13_submit_rule", NOW(), NOW());

# --- !Downs

DELETE FROM ab_test_attributes WHERE name = "form1_load_rule";
DELETE FROM ab_test_attributes WHERE name = "form13_submit_rule";