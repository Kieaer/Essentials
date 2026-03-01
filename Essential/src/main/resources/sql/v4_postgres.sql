/* 예약된 table 이름과 linux 문제 해결 */
ALTER TABLE player RENAME TO players;
ALTER TABLE data RENAME TO plugin_data;
ALTER TABLE banned RENAME TO player_banned;

/* 더이상 사용하지 않는 column 삭제 */
ALTER TABLE players DROP COLUMN "freeze";
ALTER TABLE players DROP COLUMN hud;
ALTER TABLE players DROP COLUMN tpp;
ALTER TABLE players DROP COLUMN "tppTeam";
ALTER TABLE players DROP COLUMN log;
ALTER TABLE players DROP COLUMN "oldUUID";
ALTER TABLE players DROP COLUMN "duplicateName";
ALTER TABLE players DROP COLUMN tracking;
ALTER TABLE players DROP COLUMN "lastPlayedWorldId";
ALTER TABLE players DROP COLUMN "totalJoinCount";
ALTER TABLE players DROP COLUMN "totalKickCount";

/* RDBMS Linux 기본 설정으로 인한 이름 변경 */
ALTER TABLE players RENAME COLUMN "blockPlaceCount" to block_place_count;
ALTER TABLE players RENAME COLUMN "blockBreakCount" to block_break_count;
ALTER TABLE players RENAME COLUMN "firstPlayDate" to first_played;
ALTER TABLE players RENAME COLUMN "lastLoginTime" to last_played;
ALTER TABLE players RENAME COLUMN "totalPlayTime" to total_played;
ALTER TABLE players RENAME COLUMN "attackModeClear" to attack_clear;
ALTER TABLE players RENAME COLUMN "pvpVictoriesCount" to pvp_win_count;
ALTER TABLE players RENAME COLUMN "pvpDefeatCount" to pvp_lose_count;
ALTER TABLE players RENAME COLUMN "pvpEliminationTeamCount" to pvp_eliminated_count;
ALTER TABLE players RENAME COLUMN "mvpTime" to pvp_mvp_count;
ALTER TABLE players RENAME COLUMN "accountID" to account_id;
ALTER TABLE players RENAME COLUMN "accountPW" to account_pw;
ALTER TABLE players RENAME COLUMN discord to discord_id;
ALTER TABLE players RENAME COLUMN mute to chat_muted;
ALTER TABLE players RENAME COLUMN "showLevelEffects" to effect_visibility;
ALTER TABLE players RENAME COLUMN "effectLevel" to effect_level;
ALTER TABLE players RENAME COLUMN "effectColor" to effect_color;
ALTER TABLE players RENAME COLUMN "hideRanking" to hide_ranking;
ALTER TABLE players RENAME COLUMN strict to strict_mode;
ALTER TABLE players RENAME COLUMN "lastLoginDate" to last_login_date;
ALTER TABLE players RENAME COLUMN "lastLeaveDate" to last_logout_date;
ALTER TABLE players RENAME COLUMN "lastPlayedWorldName" to last_played_world_name;
ALTER TABLE players RENAME COLUMN "lastPlayedWorldMode" to last_played_world_mode;
ALTER TABLE players RENAME COLUMN "isConnected" to is_connected;
ALTER TABLE players RENAME COLUMN "banTime" to ban_expire_date;
ALTER TABLE players RENAME COLUMN "joinStacks" to attendance_days;

/* Date-Time Type Conversion */
ALTER TABLE players ALTER COLUMN first_played TYPE TIMESTAMP WITHOUT TIME ZONE USING to_timestamp(first_played / 1000.0);
ALTER TABLE players ALTER COLUMN last_played TYPE TIMESTAMP WITHOUT TIME ZONE USING to_timestamp(last_played / 1000.0);
ALTER TABLE players ALTER COLUMN last_login_date TYPE TIMESTAMP WITHOUT TIME ZONE USING (CASE WHEN last_login_date ~ '^\d{4}-\d{2}-\d{2}' THEN last_login_date::timestamp ELSE NULL END);
ALTER TABLE players ALTER COLUMN last_logout_date TYPE TIMESTAMP WITHOUT TIME ZONE USING (CASE WHEN last_logout_date ~ '^\d{4}-\d{2}-\d{2}' THEN last_logout_date::timestamp ELSE NULL END);
ALTER TABLE players ALTER COLUMN ban_expire_date TYPE TIMESTAMP WITHOUT TIME ZONE USING (CASE WHEN ban_expire_date ~ '^\d{4}-\d{2}-\d{2}' THEN ban_expire_date::timestamp ELSE NULL END);

/* 새 데이터 추가 */
ALTER TABLE players ADD COLUMN wave_clear integer DEFAULT 0 NOT NULL;
ALTER TABLE players ADD COLUMN is_banned boolean DEFAULT false NOT NULL;
ALTER TABLE plugin_data ADD COLUMN id SERIAL PRIMARY KEY;
ALTER TABLE plugin_data ADD COLUMN database_version integer DEFAULT 4;
ALTER TABLE plugin_data ADD COLUMN hub_map_name text;

UPDATE players SET wave_clear = 0 WHERE wave_clear IS NULL;
UPDATE players SET is_banned = false WHERE is_banned IS NULL;
UPDATE plugin_data SET database_version = 4 WHERE database_version IS NULL;

/* 더이상 사용되지 않는 Table 삭제 */
DROP TABLE db;

/* players 테이블에 id 컬럼 추가 */
ALTER TABLE players ADD COLUMN id BIGSERIAL PRIMARY KEY;

/* 새 테이블 추가 */
CREATE TABLE IF NOT EXISTS PLAYER_ACHIEVEMENTS (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    achievement_name VARCHAR(100) NOT NULL,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT FK_PLAYER_ACHIEVEMENTS_PLAYER_ID__ID FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);
