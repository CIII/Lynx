# --- !Ups
CREATE TABLE `listings` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `image_url` varchar(255) DEFAULT NULL,
  `site_url` varchar(255) DEFAULT NULL,
  `rating` decimal(5,1) DEFAULT NULL,
  `review_count` int(11) DEFAULT NULL,
  `score` decimal(5,1) DEFAULT NULL,
  `created_date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `last_updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8;

CREATE TRIGGER listings_last_updated
BEFORE UPDATE
ON listings FOR EACH ROW
set new.last_updated = current_timestamp();

CREATE TABLE `listing_descs` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `listing_id` bigint(20) unsigned NOT NULL,
  `desc` varchar(100) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `listing_descs_listings_fk` (`listing_id`),
  CONSTRAINT `listing_descs_listings_fk` FOREIGN KEY (`listing_id`) REFERENCES `listings` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8;

CREATE TRIGGER listing_descs_updated_ta
BEFORE UPDATE
ON listing_descs FOR EACH ROW
set new.updated_at = current_timestamp();

LOCK TABLES `listings` WRITE;
/*!40000 ALTER TABLE `listings` DISABLE KEYS */;
INSERT INTO `listings` VALUES (1,'Test Order 1','image url','site url',1,3,5,'2017-03-21 18:27:04','2017-03-21 18:27:04'),(2,'Test Order 2','image url 2','site url 2',2,4,6,'2017-03-21 18:27:42','2017-03-21 18:27:42'),(3,'LeadGenesis:Solar','image url 3','site url 3',7,9,11,'2017-03-21 21:16:46','2017-03-21 21:18:47');
/*!40000 ALTER TABLE `listings` ENABLE KEYS */;
UNLOCK TABLES;

LOCK TABLES `listing_descs` WRITE;
/*!40000 ALTER TABLE `listing_descs` DISABLE KEYS */;
INSERT INTO `listing_descs` VALUES (1,1,'desc 1 1','2017-03-22 16:26:40','2017-03-22 16:26:40'),(2,1,'desc 1 2','2017-03-22 16:26:42','2017-03-22 16:26:42'),(3,1,'desc 1 3','2017-03-22 16:26:43','2017-03-22 16:26:43'),(6,2,'desc 2 1','2017-03-22 16:29:05','2017-03-22 16:29:05'),(7,2,'desc 2 2','2017-03-22 16:29:06','2017-03-22 16:29:06'),(8,2,'desc 2 3','2017-03-22 16:29:07','2017-03-22 16:29:07'),(9,3,'desc 3 1','2017-03-22 16:29:09','2017-03-22 16:29:09'),(10,3,'desc 3 2','2017-03-22 16:29:10','2017-03-22 16:29:10'),(11,3,'desc 3 3','2017-03-22 16:29:10','2017-03-22 16:29:10');
/*!40000 ALTER TABLE `listing_descs` ENABLE KEYS */;
UNLOCK TABLES;

# --- !Downs

DROP TABLE IF EXISTS `listing_descs`;
DROP TABLE IF EXISTS `listings`;
