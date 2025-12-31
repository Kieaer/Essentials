/* 예약된 table 이름과 linux 문제 해결 */
ALTER TABLE player RENAME TO players;
ALTER TABLE "data" RENAME TO plugin_data;
ALTER TABLE banned RENAME TO player_banned;

/* 더이상 사용하지 않는 column 삭제 (존재할 때만) */
ALTER TABLE players DROP COLUMN IF EXISTS "FREEZE";
ALTER TABLE players DROP COLUMN IF EXISTS "HUD";
ALTER TABLE players DROP COLUMN IF EXISTS "TPP";
ALTER TABLE players DROP COLUMN IF EXISTS "tppTeam";
ALTER TABLE players DROP COLUMN IF EXISTS "LOG";
ALTER TABLE players DROP COLUMN IF EXISTS "oldUUID";
ALTER TABLE players DROP COLUMN IF EXISTS "duplicateName";
ALTER TABLE players DROP COLUMN IF EXISTS "TRACKING";
ALTER TABLE players DROP COLUMN IF EXISTS "lastPlayedWorldId";
ALTER TABLE players DROP COLUMN IF EXISTS "totalJoinCount";
ALTER TABLE players DROP COLUMN IF EXISTS "totalKickCount";

/* RDBMS Linux 기본 설정으로 인한 이름 변경 (원본이 따옴표 camelCase임) */
ALTER TABLE players ALTER COLUMN "blockPlaceCount" RENAME TO block_place_count;
ALTER TABLE players ALTER COLUMN "blockBreakCount" RENAME TO block_break_count;
ALTER TABLE players ALTER COLUMN "firstPlayDate" RENAME TO first_played;
ALTER TABLE players ALTER COLUMN "lastLoginTime" RENAME TO last_played;
ALTER TABLE players ALTER COLUMN "totalPlayTime" RENAME TO total_played;
ALTER TABLE players ALTER COLUMN "attackModeClear" RENAME TO attack_clear;
ALTER TABLE players ALTER COLUMN "pvpVictoriesCount" RENAME TO pvp_win_count;
ALTER TABLE players ALTER COLUMN "pvpDefeatCount" RENAME TO pvp_lose_count;
ALTER TABLE players ALTER COLUMN "pvpEliminationTeamCount" RENAME TO pvp_eliminated_count;
ALTER TABLE players ALTER COLUMN "mvpTime" RENAME TO pvp_mvp_count;
ALTER TABLE players ALTER COLUMN "accountID" RENAME TO account_id;
ALTER TABLE players ALTER COLUMN "accountPW" RENAME TO account_pw;
ALTER TABLE players ALTER COLUMN "DISCORD" RENAME TO discord_id;
ALTER TABLE players ALTER COLUMN "MUTE" RENAME TO chat_muted;
ALTER TABLE players ALTER COLUMN "showLevelEffects" RENAME TO effect_visibility;
ALTER TABLE players ALTER COLUMN "effectLevel" RENAME TO effect_level;
ALTER TABLE players ALTER COLUMN "effectColor" RENAME TO effect_color;
ALTER TABLE players ALTER COLUMN "hideRanking" RENAME TO hide_ranking;
ALTER TABLE players ALTER COLUMN "STRICT" RENAME TO strict_mode;
ALTER TABLE players ALTER COLUMN "lastLoginDate" RENAME TO last_login_date;
ALTER TABLE players ALTER COLUMN "lastLeaveDate" RENAME TO last_logout_date;
ALTER TABLE players ALTER COLUMN "lastPlayedWorldName" RENAME TO last_played_world_name;
ALTER TABLE players ALTER COLUMN "lastPlayedWorldMode" RENAME TO last_played_world_mode;
ALTER TABLE players ALTER COLUMN "isConnected" RENAME TO is_connected;
ALTER TABLE players ALTER COLUMN "banTime" RENAME TO ban_expire_date;
ALTER TABLE players ALTER COLUMN "joinStacks" RENAME TO attendance_days;

/* 새 데이터 추가 */
ALTER TABLE players ADD COLUMN wave_clear integer AFTER attack_clear;
ALTER TABLE players ADD COLUMN is_banned boolean AFTER is_connected;
ALTER TABLE plugin_data ADD COLUMN id integer;
ALTER TABLE plugin_data ADD COLUMN database_version integer;
ALTER TABLE plugin_data ADD COLUMN hub_map_name text;
UPDATE plugin_data SET id = 1 WHERE id IS NULL;
UPDATE plugin_data SET database_version = 4 WHERE database_version IS NULL;

/* 더이상 사용되지 않는 Table 삭제 */
DROP TABLE IF EXISTS db;

/* players 테이블에 id 컬럼 추가 (PLAYER_ACHIEVEMENTS 테이블의 외래 키 제약 조건을 위해 필요) */
ALTER TABLE players ADD COLUMN IF NOT EXISTS id BIGINT AUTO_INCREMENT PRIMARY KEY;

/* 새 테이블 추가 (만약 존재하지 않는 경우) */
CREATE TABLE IF NOT EXISTS PLAYER_ACHIEVEMENTS (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id BIGINT NOT NULL,
    achievement_name VARCHAR(50) NOT NULL,
    completed_at TIMESTAMP(9) DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT FK_PLAYER_ACHIEVEMENTS_PLAYER_ID__ID FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_player_achievements_unsigned_integer_id CHECK (id BETWEEN 0 AND 4294967295),
    CONSTRAINT chk_player_achievements_unsigned_integer_player_id CHECK (player_id BETWEEN 0 AND 4294967295)
);
