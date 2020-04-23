# easiersolar schema update

# --- !Ups

CREATE TABLE api_tokens (
    id BIGINT(20) NOT NULL AUTO_INCREMENT,
    token  VARCHAR(255) NOT NULL,
    active  TINYINT(1),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

INSERT INTO api_tokens(token, active, created_at, updated_at) VALUES('13aea576-7394-4934-a2a1-6eba36fd74f9', true, NOW(), NOW());
INSERT INTO api_tokens(token, active, created_at, updated_at) VALUES('d51b4893-3120-4b45-8d57-1500f1eb1c03', true, NOW(), NOW());
INSERT INTO api_tokens(token, active, created_at, updated_at) VALUES('41b32a99-202d-4a82-927f-f5bd7992a422', true, NOW(), NOW());
INSERT INTO api_tokens(token, active, created_at, updated_at) VALUES('fbcf48ff-740f-4d19-81d8-5ff7f6b4f844', true, NOW(), NOW());
INSERT INTO api_tokens(token, active, created_at, updated_at) VALUES('9432fe20-25b0-4731-9de3-5fb3bce0f565', true, NOW(), NOW());

# --- !Downs

DROP TABLE IF EXISTS api_tokens;