# --- !Ups

INSERT INTO listings VALUES (10, 'Invaleon Solar Technologies', 'http://d2qm0cphmqrv42.cloudfront.net/app/images/thanks-invaleon.png', 'http://www.invaleonsolar.com', NULL, 25, NULL, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()) ON DUPLICATE KEY UPDATE name=VALUES(name), image_url=VALUES(image_url), site_url=VALUES(site_url), rating=VALUES(rating), review_count=VALUES(review_count), score=VALUES(score), last_updated=CURRENT_TIMESTAMP();
INSERT INTO listing_descs VALUES
    (30, 10, 'User reviews frequently mention competitive low prices', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
    (31, 10, 'Excellent track record, with a 5.0 rating on 25 reviews', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
    (32, 10, 'Local MA founded in 2011, 350 completed projects', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP())
    ON DUPLICATE KEY UPDATE listing_id=VALUES(listing_id), `desc`=VALUES(`desc`), updated_at=CURRENT_TIMESTAMP();

# --- !Downs

DELETE FROM listing_descs WHERE id IN (30, 31, 32);
DELETE FROM listings WHERE id = 10;