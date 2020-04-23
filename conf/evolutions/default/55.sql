# --- !Ups

INSERT INTO listings VALUES
    (8, 'Boston Solar', 'http://d2qm0cphmqrv42.cloudfront.net/app/images/thanks-boston-solar.png', 'www.bostonsolar.com', null, null, 8.5, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
    (9, 'New England Clean Energy', 'http://d2qm0cphmqrv42.cloudefront.net/app/images/thanks-nece.png', 'www.newenglandcleanenergy.com', null, null, null, CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()) ON DUPLICATE KEY UPDATE id=id;
    
INSERT INTO listing_descs VALUES
    (24, 8, 'Solar Power World’s #1 MA Contractor', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
    (25, 8, 'Has completed 3,000+ installs in the last 5 years', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
    (26, 8, '25-year product warranty and performance guarantee', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
    (27, 9, 'Largest MA, RI and NH installer by volume', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
    (28, 9, 'Very good reviews, with a 4.90 on SolarReviews', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()),
    (29, 9, 'Local company with a history of excellent quality<br\>241 Reviews', CURRENT_TIMESTAMP(), CURRENT_TIMESTAMP()) ON DUPLICATE KEY UPDATE id=id;
    
# --- !Downs

DELETE FROM listing_descs WHERE id IN (24, 25, 26, 27, 28, 29);
DELETE FROM listings WHERE id IN (8, 9);
