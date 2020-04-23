use easiersolar;

DROP TEMPORARY TABLE IF EXISTS f_shouldbe;

CREATE TEMPORARY TABLE f_shouldbe (
	f_id BIGINT(20),
    s_id BIGINT(20)
);

INSERT INTO
	f_shouldbe(f_id,s_id)
SELECT
	f.id,
	e.session_id
FROM
	forms f
JOIN 
	events e on f.event_id = e.id
WHERE f.created_at < "2017-01-05"
ORDER BY f.created_at desc;

UPDATE
	forms f,
    f_shouldbe fs
SET
	f.session_id= fs.s_id
WHERE
	f.id = fs.f_id;

DROP TEMPORARY TABLE IF EXISTS f_shouldbe;