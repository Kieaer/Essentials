DELETE FROM player_achievements
WHERE player_id IN (
    SELECT p1.id
    FROM players p1
             INNER JOIN players p2 ON p1.uuid = p2.uuid AND p1.id > p2.id
);

DELETE FROM players p1
    USING players p2
WHERE p1.uuid = p2.uuid AND p1.id > p2.id;

/* Fix NULL last_login_date left by v4 migration */
UPDATE players SET last_login_date = CURRENT_TIMESTAMP WHERE last_login_date IS NULL;