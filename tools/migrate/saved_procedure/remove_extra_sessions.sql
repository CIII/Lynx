USE easiersolar;

DROP TEMPORARY TABLE IF EXISTS sid;

CREATE TEMPORARY TABLE sid (
	sid BIGINT(20)
);

INSERT INTO 
sid(sid)
(SELECT 
	id
FROM	
	sessions
WHERE
	sessions.id NOT IN
		(SELECT DISTINCT
			session_id
		FROM
			events) AND
	created_at <= '2017-01-04 17:39:10'
LIMIT 1000);

#SELECT sid FROM sid;

DELETE FROM sessions
WHERE sessions.id IN (SELECT sid FROM sid);

DROP TEMPORARY TABLE IF EXISTS sid;
