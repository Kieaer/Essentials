/* 예약된 table 이름과 linux 문제 해결 */
ALTER TABLE IF EXISTS "player" RENAME TO players;
ALTER TABLE IF EXISTS player RENAME TO players;
ALTER TABLE IF EXISTS "data" RENAME TO plugin_data;
ALTER TABLE IF EXISTS data RENAME TO plugin_data;
ALTER TABLE IF EXISTS banned RENAME TO player_banned;

/* 더이상 사용하지 않는 column 삭제 (존재할 때만) */
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS "freeze";
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS hud;
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS tpp;
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS "tppTeam";
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS log;
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS "oldUUID";
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS "duplicateName";
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS tracking;
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS "lastPlayedWorldId";
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS "totalJoinCount";
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS "totalKickCount";

/* RDBMS Linux 기본 설정으로 인한 이름 변경 */
ALTER TABLE IF EXISTS players RENAME COLUMN "blockPlaceCount" TO block_place_count;
ALTER TABLE IF EXISTS players RENAME COLUMN "blockBreakCount" TO block_break_count;
ALTER TABLE IF EXISTS players RENAME COLUMN "firstPlayDate" TO first_played;
ALTER TABLE IF EXISTS players RENAME COLUMN "lastLoginTime" TO last_played;
ALTER TABLE IF EXISTS players RENAME COLUMN "totalPlayTime" TO total_played;
ALTER TABLE IF EXISTS players RENAME COLUMN "attackModeClear" TO attack_clear;
ALTER TABLE IF EXISTS players RENAME COLUMN "pvpVictoriesCount" TO pvp_win_count;
ALTER TABLE IF EXISTS players RENAME COLUMN "pvpDefeatCount" TO pvp_lose_count;
ALTER TABLE IF EXISTS players RENAME COLUMN "pvpEliminationTeamCount" TO pvp_eliminated_count;
ALTER TABLE IF EXISTS players RENAME COLUMN "mvpTime" TO pvp_mvp_count;
ALTER TABLE IF EXISTS players RENAME COLUMN "accountID" TO account_id;
ALTER TABLE IF EXISTS players RENAME COLUMN "accountPW" TO account_pw;
ALTER TABLE IF EXISTS players RENAME COLUMN discord TO discord_id;
ALTER TABLE IF EXISTS players RENAME COLUMN mute TO chat_muted;
ALTER TABLE IF EXISTS players RENAME COLUMN "showLevelEffects" TO effect_visibility;
ALTER TABLE IF EXISTS players RENAME COLUMN "effectLevel" TO effect_level;
ALTER TABLE IF EXISTS players RENAME COLUMN "effectColor" TO effect_color;
ALTER TABLE IF EXISTS players RENAME COLUMN "hideRanking" TO hide_ranking;
ALTER TABLE IF EXISTS players RENAME COLUMN strict TO strict_mode;
ALTER TABLE IF EXISTS players RENAME COLUMN "lastLoginDate" TO last_login_date;
ALTER TABLE IF EXISTS players RENAME COLUMN "lastLeaveDate" TO last_logout_date;
ALTER TABLE IF EXISTS players RENAME COLUMN "lastPlayedWorldName" TO last_played_world_name;
ALTER TABLE IF EXISTS players RENAME COLUMN "lastPlayedWorldMode" TO last_played_world_mode;
ALTER TABLE IF EXISTS players RENAME COLUMN "isConnected" TO is_connected;
ALTER TABLE IF EXISTS players RENAME COLUMN "banTime" TO ban_expire_date;
ALTER TABLE IF EXISTS players RENAME COLUMN "joinStacks" TO attendance_days;

/* Date-Time Type Conversion */
-- first_played (BIGINT -> TIMESTAMP)
ALTER TABLE IF EXISTS players ADD COLUMN IF NOT EXISTS first_played_new TIMESTAMP;
UPDATE players SET first_played_new = DATEADD('MILLISECOND', first_played, TIMESTAMP '1970-01-01 00:00:00') WHERE first_played IS NOT NULL;
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS first_played;
ALTER TABLE IF EXISTS players RENAME COLUMN first_played_new TO first_played;

-- last_played (BIGINT -> TIMESTAMP)
ALTER TABLE IF EXISTS players ADD COLUMN IF NOT EXISTS last_played_new TIMESTAMP;
UPDATE players SET last_played_new = DATEADD('MILLISECOND', last_played, TIMESTAMP '1970-01-01 00:00:00') WHERE last_played IS NOT NULL;
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS last_played;
ALTER TABLE IF EXISTS players RENAME COLUMN last_played_new TO last_played;

-- last_login_date, last_logout_date, ban_expire_date (VARCHAR -> TIMESTAMP)
ALTER TABLE IF EXISTS players ALTER COLUMN last_login_date SET DATA TYPE TIMESTAMP;
ALTER TABLE IF EXISTS players ALTER COLUMN last_logout_date SET DATA TYPE TIMESTAMP;
ALTER TABLE IF EXISTS players ALTER COLUMN ban_expire_date SET DATA TYPE TIMESTAMP;

/* Column Length Increase */
ALTER TABLE IF EXISTS players ALTER COLUMN name SET DATA TYPE VARCHAR(256);
ALTER TABLE IF EXISTS players ALTER COLUMN permission SET DATA TYPE VARCHAR(100);

/* 플러그인 데이터 테이블 보장 */
CREATE TABLE IF NOT EXISTS plugin_data (
    id INTEGER AUTO_INCREMENT PRIMARY KEY,
    database_version INTEGER,
    hub_map_name TEXT,
    data TEXT
);

/* 새 데이터 추가 */
ALTER TABLE IF EXISTS players ADD COLUMN IF NOT EXISTS wave_clear integer DEFAULT 0 NOT NULL;
ALTER TABLE IF EXISTS players ADD COLUMN IF NOT EXISTS is_banned boolean DEFAULT false NOT NULL;
ALTER TABLE IF EXISTS plugin_data ADD COLUMN IF NOT EXISTS id integer;
ALTER TABLE IF EXISTS plugin_data ADD COLUMN IF NOT EXISTS database_version integer;
ALTER TABLE IF EXISTS plugin_data ADD COLUMN IF NOT EXISTS hub_map_name text;
ALTER TABLE IF EXISTS plugin_data ADD COLUMN IF NOT EXISTS data text;

UPDATE players SET wave_clear = 0 WHERE wave_clear IS NULL;
UPDATE players SET is_banned = false WHERE is_banned IS NULL;
UPDATE plugin_data SET id = 1 WHERE id IS NULL;
UPDATE plugin_data SET database_version = 4 WHERE database_version IS NULL;

/* 데이터가 비어있을 경우 초기 데이터 삽입 */
INSERT INTO plugin_data (id, database_version, data) SELECT 1, 4, '{}' FROM (SELECT 1) WHERE NOT EXISTS (SELECT 1 FROM plugin_data);

/* 더이상 사용되지 않는 Table 삭제 */
DROP TABLE IF EXISTS db;

/* players 테이블에 id 컬럼 추가 */
ALTER TABLE IF EXISTS players ADD COLUMN IF NOT EXISTS id BIGINT AUTO_INCREMENT PRIMARY KEY;

/* 새 테이블 추가 */
CREATE TABLE IF NOT EXISTS PLAYER_ACHIEVEMENTS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    achievement_name VARCHAR(100) NOT NULL,
    completed_at TIMESTAMP(9) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT FK_PLAYER_ACHIEVEMENTS_PLAYER_ID__ID FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_player_achievements_unsigned_integer_id CHECK (id BETWEEN 0 AND 4294967295),
    CONSTRAINT chk_player_achievements_unsigned_integer_player_id CHECK (player_id BETWEEN 0 AND 4294967295)
);
