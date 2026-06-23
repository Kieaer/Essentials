DELETE FROM player_achievements
WHERE player_id IN (
    SELECT id
    FROM (
        SELECT id, ROW_NUMBER() OVER (
            PARTITION BY uuid
            ORDER BY
                CASE WHEN last_login_date IS NULL THEN 1 ELSE 0 END ASC,
                last_login_date DESC,
                level DESC,
                id ASC
        ) as rn
        FROM players
    ) tmp
    WHERE rn > 1
);

DELETE FROM players
WHERE id IN (
    SELECT id
    FROM (
        SELECT id, ROW_NUMBER() OVER (
            PARTITION BY uuid
            ORDER BY
                CASE WHEN last_login_date IS NULL THEN 1 ELSE 0 END ASC,
                last_login_date DESC,
                level DESC,
                id ASC
        ) as rn
        FROM players
    ) tmp
    WHERE rn > 1
);

/* Fix NULL last_login_date left by v4 migration */
UPDATE players SET last_login_date = CURRENT_TIMESTAMP WHERE last_login_date IS NULL;

ALTER TABLE map_ratings DROP CONSTRAINT IF EXISTS map_ratings_map_hash_unique;
ALTER TABLE map_ratings ADD COLUMN IF NOT EXISTS difficulty INT DEFAULT 3;
ALTER TABLE map_ratings ADD COLUMN IF NOT EXISTS rating INT DEFAULT 3;
UPDATE map_ratings SET difficulty = 3, rating = CASE WHEN is_upvote = TRUE THEN 5 ELSE 1 END;
ALTER TABLE map_ratings DROP COLUMN IF EXISTS is_upvote;