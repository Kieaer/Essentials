DELETE FROM player_achievements
WHERE player_id IN (SELECT id
             FROM (SELECT id, ROW_NUMBER() OVER (
                 PARTITION BY uuid
                 ORDER BY
                     CASE WHEN last_login_date IS NULL THEN 1 ELSE 0 END ASC,
                     last_login_date DESC,
                     "level" DESC,
                     id ASC
             ) as rn
             FROM players) tmp
             WHERE rn > 1);

DELETE
FROM players
WHERE id IN (SELECT id
             FROM (SELECT id, ROW_NUMBER() OVER (
                 PARTITION BY uuid
                 ORDER BY
                     CASE WHEN last_login_date IS NULL THEN 1 ELSE 0 END ASC,
                     last_login_date DESC,
                     "level" DESC,
                     id ASC
             ) as rn
             FROM players) tmp
             WHERE rn > 1);

/* Fix NULL last_login_date left by v4 H2 migration REGEXP bug */
UPDATE PLAYERS SET last_login_date = CURRENT_TIMESTAMP(9) WHERE last_login_date IS NULL;

ALTER TABLE map_ratings DROP CONSTRAINT IF EXISTS map_ratings_map_hash_unique;