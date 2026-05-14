/* 예약된 table 이름과 linux 문제 해결 */
ALTER TABLE player RENAME TO players;
ALTER TABLE data RENAME TO plugin_data;

CREATE TABLE IF NOT EXISTS player_banned (
    id INT AUTO_INCREMENT PRIMARY KEY,
    names JSON,
    ips JSON,
    uuid VARCHAR(25),
    reason VARCHAR(256) DEFAULT 'Legacy ban',
    date BIGINT DEFAULT 0
);

INSERT INTO player_banned (names, uuid, reason)
SELECT JSON_ARRAY(data), data, 'Legacy ban (name/UUID)'
FROM banned WHERE type = 0
GROUP BY data;

INSERT INTO player_banned (ips, reason)
SELECT JSON_ARRAY(data), 'Legacy ban (IP)'
FROM banned WHERE type = 1
GROUP BY data;

DROP TABLE IF EXISTS banned;

/* 더이상 사용하지 않는 column 삭제 */
ALTER TABLE players DROP COLUMN IF EXISTS freeze;
ALTER TABLE players DROP COLUMN IF EXISTS hud;
ALTER TABLE players DROP COLUMN IF EXISTS tpp;
ALTER TABLE players DROP COLUMN IF EXISTS tppTeam;
ALTER TABLE players DROP COLUMN IF EXISTS log;
ALTER TABLE players DROP COLUMN IF EXISTS oldUUID;
ALTER TABLE players DROP COLUMN IF EXISTS duplicateName;
ALTER TABLE players DROP COLUMN IF EXISTS tracking;
ALTER TABLE players DROP COLUMN IF EXISTS lastPlayedWorldId;
ALTER TABLE players DROP COLUMN IF EXISTS totalJoinCount;
ALTER TABLE players DROP COLUMN IF EXISTS totalKickCount;
ALTER TABLE players DROP COLUMN IF EXISTS animatedName;
ALTER TABLE players DROP COLUMN IF EXISTS currentPlayTime;

/* RDBMS Linux 기본 설정으로 인한 이름 변경 */
ALTER TABLE players CHANGE blockPlaceCount block_place_count INTEGER;
ALTER TABLE players CHANGE blockBreakCount block_break_count INTEGER;
ALTER TABLE players CHANGE firstPlayDate first_played BIGINT;
ALTER TABLE players CHANGE lastLoginTime last_played BIGINT;
ALTER TABLE players CHANGE languageTag language_tag VARCHAR(10);
ALTER TABLE players CHANGE totalPlayTime total_played INTEGER;
ALTER TABLE players CHANGE attackModeClear attack_clear INTEGER;
ALTER TABLE players CHANGE pvpVictoriesCount pvp_win_count SMALLINT;
ALTER TABLE players CHANGE pvpDefeatCount pvp_lose_count SMALLINT;
ALTER TABLE players CHANGE pvpEliminationTeamCount pvp_eliminated_count SMALLINT;
ALTER TABLE players CHANGE mvpTime pvp_mvp_count SMALLINT;
ALTER TABLE players CHANGE accountID account_id VARCHAR(50);
ALTER TABLE players CHANGE accountPW account_pw VARCHAR(256);
ALTER TABLE players CHANGE DISCORD discord_id VARCHAR(50);
ALTER TABLE players CHANGE MUTE chat_muted BOOLEAN;
ALTER TABLE players CHANGE showLevelEffects effect_visibility BOOLEAN;
ALTER TABLE players CHANGE effectLevel effect_level SMALLINT;
ALTER TABLE players CHANGE effectColor effect_color VARCHAR(20);
ALTER TABLE players CHANGE hideRanking hide_ranking BOOLEAN;
ALTER TABLE players CHANGE STRICT strict_mode BOOLEAN;
ALTER TABLE players CHANGE lastLoginDate last_login_date VARCHAR(50);
ALTER TABLE players CHANGE lastLeaveDate last_logout_date VARCHAR(50);
ALTER TABLE players CHANGE lastPlayedWorldName last_played_world_name VARCHAR(50);
ALTER TABLE players CHANGE lastPlayedWorldMode last_played_world_mode VARCHAR(50);
ALTER TABLE players CHANGE isConnected is_connected BOOLEAN;
ALTER TABLE players CHANGE banTime ban_expire_date VARCHAR(50);
ALTER TABLE players CHANGE joinStacks attendance_days INTEGER;

ALTER TABLE players MODIFY name VARCHAR(256);
ALTER TABLE players MODIFY uuid VARCHAR(25);

ALTER TABLE players ADD COLUMN level INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players ADD COLUMN exp INTEGER NOT NULL DEFAULT 0;

/* Use temp column pattern for timestamp conversion (MySQL bug fix) */
ALTER TABLE players ADD COLUMN first_played_tmp TIMESTAMP NULL;
ALTER TABLE players ADD COLUMN last_played_tmp TIMESTAMP NULL;
UPDATE players SET first_played_tmp = FROM_UNIXTIME(first_played / 1000) WHERE first_played IS NOT NULL AND first_played != 0;
UPDATE players SET last_played_tmp = FROM_UNIXTIME(last_played / 1000) WHERE last_played IS NOT NULL AND last_played != 0;
ALTER TABLE players DROP COLUMN first_played;
ALTER TABLE players DROP COLUMN last_played;
ALTER TABLE players CHANGE first_played_tmp first_played TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE players CHANGE last_played_tmp last_played TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE players SET last_login_date = last_login_date WHERE last_login_date IS NOT NULL AND last_login_date != '';
ALTER TABLE players MODIFY last_login_date TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE players SET last_logout_date = last_logout_date WHERE last_logout_date IS NOT NULL AND last_logout_date != '';
ALTER TABLE players MODIFY last_logout_date TIMESTAMP NULL;

UPDATE players SET ban_expire_date = ban_expire_date WHERE ban_expire_date IS NOT NULL AND ban_expire_date != '';
ALTER TABLE players MODIFY ban_expire_date TIMESTAMP NULL;

ALTER TABLE players MODIFY account_id VARCHAR(50) NULL DEFAULT NULL;
ALTER TABLE players MODIFY account_pw VARCHAR(256) NULL DEFAULT NULL;
ALTER TABLE players MODIFY discord_id VARCHAR(50) NULL DEFAULT NULL;
ALTER TABLE players MODIFY effect_level SMALLINT NULL DEFAULT NULL;
ALTER TABLE players MODIFY effect_color VARCHAR(20) NULL DEFAULT NULL;
ALTER TABLE players MODIFY last_played_world_name VARCHAR(50) NULL DEFAULT NULL;
ALTER TABLE players MODIFY last_played_world_mode VARCHAR(50) NULL DEFAULT NULL;
ALTER TABLE players MODIFY language_tag VARCHAR(10) NOT NULL DEFAULT 'en';
ALTER TABLE players MODIFY block_place_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players MODIFY block_break_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players MODIFY total_played INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players MODIFY attack_clear INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players MODIFY level INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players MODIFY exp INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players MODIFY attendance_days INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players MODIFY pvp_win_count SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE players MODIFY pvp_lose_count SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE players MODIFY pvp_eliminated_count SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE players MODIFY pvp_mvp_count SMALLINT NOT NULL DEFAULT 0;
ALTER TABLE players MODIFY chat_muted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE players MODIFY effect_visibility BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE players MODIFY hide_ranking BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE players MODIFY strict_mode BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE players MODIFY is_connected BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE players MODIFY permission VARCHAR(50) NOT NULL DEFAULT 'default';
UPDATE players SET permission = 'default' WHERE permission IS NULL OR permission = '';

UPDATE players SET language_tag = 'en' WHERE language_tag IS NULL OR language_tag = '';

ALTER TABLE plugin_data ADD COLUMN id INTEGER;
ALTER TABLE plugin_data ADD COLUMN database_version INTEGER;
ALTER TABLE plugin_data ADD COLUMN hub_map_name TEXT;

UPDATE plugin_data SET id = 1 WHERE id IS NULL;
UPDATE plugin_data SET database_version = 4 WHERE database_version IS NULL;

ALTER TABLE players ADD COLUMN wave_clear INTEGER NOT NULL DEFAULT 0;
ALTER TABLE players ADD COLUMN is_banned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE players ADD COLUMN id BIGINT AUTO_INCREMENT PRIMARY KEY FIRST;

UPDATE players SET wave_clear = 0 WHERE wave_clear IS NULL;
UPDATE players SET is_banned = FALSE WHERE is_banned IS NULL;

DROP TABLE IF EXISTS db;

DELETE p1 FROM players p1
INNER JOIN players p2
WHERE p1.uuid = p2.uuid AND p1.id > p2.id;

CREATE TABLE IF NOT EXISTS player_achievements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    achievement_name VARCHAR(100) NOT NULL,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_player_achievements_player_id__id FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);
