/* 예약된 table 이름과 linux 문제 해결 */
ALTER TABLE player RENAME TO players;
ALTER TABLE data RENAME TO plugin_data;
ALTER TABLE banned RENAME TO player_banned;

/* 더이상 사용하지 않는 column 삭제 */
ALTER TABLE players DROP COLUMN freeze;
ALTER TABLE players DROP COLUMN hud;
ALTER TABLE players DROP COLUMN tpp;
ALTER TABLE players DROP COLUMN tppTeam;
ALTER TABLE players DROP COLUMN log;
ALTER TABLE players DROP COLUMN oldUUID;
ALTER TABLE players DROP COLUMN duplicateName;
ALTER TABLE players DROP COLUMN tracking;
ALTER TABLE players DROP COLUMN lastPlayedWorldId;
ALTER TABLE players DROP COLUMN totalJoinCount;
ALTER TABLE players DROP COLUMN totalKickCount;

/* RDBMS Linux 기본 설정으로 인한 이름 변경 */
ALTER TABLE players RENAME COLUMN blockPlaceCount to block_place_count;
ALTER TABLE players RENAME COLUMN blockBreakCount to block_break_count;
ALTER TABLE players RENAME COLUMN firstPlayDate to first_played;
ALTER TABLE players RENAME COLUMN lastLoginTime to last_played;
ALTER TABLE players RENAME COLUMN totalPlayTime to total_played;
ALTER TABLE players RENAME COLUMN attackModeClear to attack_clear;
ALTER TABLE players RENAME COLUMN pvpVictoriesCount to pvp_win_count;
ALTER TABLE players RENAME COLUMN pvpDefeatCount to pvp_lose_count;
ALTER TABLE players RENAME COLUMN pvpEliminationTeamCount to pvp_eliminated_count;
ALTER TABLE players RENAME COLUMN mvpTime to pvp_mvp_count;
ALTER TABLE players RENAME COLUMN accountID to account_id;
ALTER TABLE players RENAME COLUMN accountPW to account_id;
ALTER TABLE players RENAME COLUMN discord to discord_id;
ALTER TABLE players RENAME COLUMN mute to chat_muted;
ALTER TABLE players RENAME COLUMN showLevelEffects to effect_visibility;
ALTER TABLE players RENAME COLUMN effectLevel to effect_level;
ALTER TABLE players RENAME COLUMN effectColor to effect_color;
ALTER TABLE players RENAME COLUMN hideRanking to hide_ranking;
ALTER TABLE players RENAME COLUMN strict to strict_mode;
ALTER TABLE players RENAME COLUMN lastLoginDate to last_login_date;
ALTER TABLE players RENAME COLUMN lastLeaveDate to last_logout_date;
ALTER TABLE players RENAME COLUMN lastPlayedWorldName to last_played_world_name;
ALTER TABLE players RENAME COLUMN lastPlayedWorldMode to last_played_world_mode;
ALTER TABLE players RENAME COLUMN isConnected to is_connected;
ALTER TABLE players RENAME COLUMN banTime to ban_expire_date;
ALTER TABLE players RENAME COLUMN joinStacks to attendance_days;

/* 새 데이터 추가 */
ALTER TABLE players ADD COLUMN wave_clear integer AFTER attack_clear;
ALTER TABLE players ADD COLUMN effect_visibility integer AFTER chat_muted;
ALTER TABLE players ADD COLUMN is_banned boolean AFTER is_connected;

/* 더이상 사용되지 않는 Table 삭제 */
DROP TABLE db;

/* 현재 DB 버전 추가 */
INSERT INTO plugin_data VALUES (21, 4, {})