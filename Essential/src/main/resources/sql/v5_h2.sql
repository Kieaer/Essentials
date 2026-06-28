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
ALTER TABLE map_ratings ADD COLUMN IF NOT EXISTS difficulty INT DEFAULT 3;
ALTER TABLE map_ratings ADD COLUMN IF NOT EXISTS rating INT DEFAULT 3;
UPDATE map_ratings SET difficulty = 3, rating = CASE WHEN is_upvote = TRUE THEN 5 ELSE 1 END;
ALTER TABLE map_ratings DROP COLUMN IF EXISTS is_upvote;

/* 판당 기여도 점수 테이블 */
CREATE TABLE IF NOT EXISTS player_contributions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    game_mode VARCHAR(20) NOT NULL,
    map_name VARCHAR(64),
    score DOUBLE PRECISION NOT NULL,
    recorded_at TIMESTAMP(9) DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_PLAYER_CONTRIBUTIONS_PLAYER_ID__ID FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);