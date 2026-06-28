DELETE FROM player_achievements
WHERE player_id IN (
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (
            PARTITION BY uuid
            ORDER BY
                CASE WHEN last_login_date IS NULL OR last_login_date = '' THEN 1 ELSE 0 END ASC,
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
    SELECT id FROM (
        SELECT id, ROW_NUMBER() OVER (
            PARTITION BY uuid
            ORDER BY
                CASE WHEN last_login_date IS NULL OR last_login_date = '' THEN 1 ELSE 0 END ASC,
                last_login_date DESC,
                level DESC,
                id ASC
        ) as rn
        FROM players
    ) tmp
    WHERE rn > 1
);

/* Fix NULL last_login_date left by v4 migration */
UPDATE players SET last_login_date = CURRENT_TIMESTAMP WHERE last_login_date IS NULL OR last_login_date = '';

ALTER TABLE map_ratings DROP INDEX map_ratings_map_hash_unique;
ALTER TABLE map_ratings ADD COLUMN difficulty INT DEFAULT 3;
ALTER TABLE map_ratings ADD COLUMN rating INT DEFAULT 3;
UPDATE map_ratings SET difficulty = 3, rating = CASE WHEN is_upvote = TRUE OR is_upvote = 1 THEN 5 ELSE 1 END;
ALTER TABLE map_ratings DROP COLUMN is_upvote;

/* 판당 기여도 점수 테이블 */
CREATE TABLE IF NOT EXISTS player_contributions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    game_mode VARCHAR(20) NOT NULL,
    map_name VARCHAR(64),
    score DOUBLE NOT NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_player_contributions_player_id__id FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);