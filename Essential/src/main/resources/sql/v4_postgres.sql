/* 예약된 table 이름과 linux 문제 해결 */
ALTER TABLE IF EXISTS player RENAME TO players;

CREATE TABLE IF NOT EXISTS player_banned (
    id SERIAL PRIMARY KEY,
    names JSONB,
    ips JSONB,
    uuid VARCHAR(25),
    reason VARCHAR(256) DEFAULT 'Legacy ban',
    "date" BIGINT DEFAULT 0
);

INSERT INTO player_banned (names, uuid, reason)
SELECT jsonb_build_array(data), data, 'Legacy ban (name/UUID)'
FROM banned WHERE type = 0
GROUP BY data;

INSERT INTO player_banned (ips, reason)
SELECT jsonb_build_array(data), 'Legacy ban (IP)'
FROM banned WHERE type = 1
GROUP BY data;

DROP TABLE IF EXISTS banned;

/* 더이상 사용하지 않는 column 삭제 */
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
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS "animatedName";
ALTER TABLE IF EXISTS players DROP COLUMN IF EXISTS "currentPlayTime";

/* RDBMS Linux 기본 설정으로 인한 이름 변경 */
ALTER TABLE IF EXISTS players RENAME COLUMN "blockPlaceCount" to block_place_count;
ALTER TABLE IF EXISTS players RENAME COLUMN "blockBreakCount" to block_break_count;
ALTER TABLE IF EXISTS players RENAME COLUMN "firstPlayDate" to first_played;
ALTER TABLE IF EXISTS players RENAME COLUMN "lastLoginTime" to last_played;
ALTER TABLE IF EXISTS players RENAME COLUMN "languageTag" to language_tag;
ALTER TABLE IF EXISTS players RENAME COLUMN "totalPlayTime" to total_played;
ALTER TABLE IF EXISTS players RENAME COLUMN "attackModeClear" to attack_clear;
ALTER TABLE IF EXISTS players RENAME COLUMN "pvpVictoriesCount" to pvp_win_count;
ALTER TABLE IF EXISTS players RENAME COLUMN "pvpDefeatCount" to pvp_lose_count;
ALTER TABLE IF EXISTS players RENAME COLUMN "pvpEliminationTeamCount" to pvp_eliminated_count;
ALTER TABLE IF EXISTS players RENAME COLUMN "mvpTime" to pvp_mvp_count;
ALTER TABLE IF EXISTS players RENAME COLUMN "accountID" to account_id;
ALTER TABLE IF EXISTS players RENAME COLUMN "accountPW" to account_pw;
ALTER TABLE IF EXISTS players RENAME COLUMN discord to discord_id;
ALTER TABLE IF EXISTS players RENAME COLUMN mute to chat_muted;
ALTER TABLE IF EXISTS players RENAME COLUMN "showLevelEffects" to effect_visibility;
ALTER TABLE IF EXISTS players RENAME COLUMN "effectLevel" to effect_level;
ALTER TABLE IF EXISTS players RENAME COLUMN "effectColor" to effect_color;
ALTER TABLE IF EXISTS players RENAME COLUMN "hideRanking" to hide_ranking;
ALTER TABLE IF EXISTS players RENAME COLUMN strict to strict_mode;
ALTER TABLE IF EXISTS players RENAME COLUMN "lastLoginDate" to last_login_date;
ALTER TABLE IF EXISTS players RENAME COLUMN "lastLeaveDate" to last_logout_date;
ALTER TABLE IF EXISTS players RENAME COLUMN "lastPlayedWorldName" to last_played_world_name;
ALTER TABLE IF EXISTS players RENAME COLUMN "lastPlayedWorldMode" to last_played_world_mode;
ALTER TABLE IF EXISTS players RENAME COLUMN "isConnected" to is_connected;
ALTER TABLE IF EXISTS players RENAME COLUMN "banTime" to ban_expire_date;
ALTER TABLE IF EXISTS players RENAME COLUMN "joinStacks" to attendance_days;

/* Date-Time 변환 */
ALTER TABLE IF EXISTS players ALTER COLUMN first_played TYPE TIMESTAMP WITHOUT TIME ZONE USING CASE WHEN first_played IS NOT NULL AND first_played != 0 THEN to_timestamp(first_played / 1000.0) ELSE CURRENT_TIMESTAMP END;
ALTER TABLE IF EXISTS players ALTER COLUMN last_played TYPE TIMESTAMP WITHOUT TIME ZONE USING CASE WHEN last_played IS NOT NULL AND last_played != 0 THEN to_timestamp(last_played / 1000.0) ELSE CURRENT_TIMESTAMP END;
ALTER TABLE IF EXISTS players ALTER COLUMN last_login_date TYPE TIMESTAMP WITHOUT TIME ZONE USING (CASE WHEN last_login_date ~ '^\d{4}-\d{2}-\d{2}' THEN last_login_date::timestamp ELSE CURRENT_TIMESTAMP END);
ALTER TABLE IF EXISTS players ALTER COLUMN last_logout_date TYPE TIMESTAMP WITHOUT TIME ZONE USING (CASE WHEN last_logout_date ~ '^\d{4}-\d{2}-\d{2}' THEN last_logout_date::timestamp END);
ALTER TABLE IF EXISTS players ALTER COLUMN ban_expire_date TYPE TIMESTAMP WITHOUT TIME ZONE USING (CASE WHEN ban_expire_date ~ '^\d{4}-\d{2}-\d{2}' THEN ban_expire_date::timestamp END);

ALTER TABLE IF EXISTS players ALTER COLUMN first_played SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE IF EXISTS players ALTER COLUMN last_played SET DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE IF EXISTS players ALTER COLUMN last_login_date SET DEFAULT CURRENT_TIMESTAMP;

/* Set DEFAULT values for all columns */
ALTER TABLE IF EXISTS players ALTER COLUMN language_tag SET DEFAULT 'en';
ALTER TABLE IF EXISTS players ALTER COLUMN block_place_count SET DEFAULT 0;
ALTER TABLE IF EXISTS players ALTER COLUMN block_break_count SET DEFAULT 0;
ALTER TABLE IF EXISTS players ALTER COLUMN total_played SET DEFAULT 0;
ALTER TABLE IF EXISTS players ALTER COLUMN attack_clear SET DEFAULT 0;
ALTER TABLE IF EXISTS players ALTER COLUMN pvp_win_count SET DEFAULT 0;
ALTER TABLE IF EXISTS players ALTER COLUMN pvp_lose_count SET DEFAULT 0;
ALTER TABLE IF EXISTS players ALTER COLUMN pvp_eliminated_count SET DEFAULT 0;
ALTER TABLE IF EXISTS players ALTER COLUMN pvp_mvp_count SET DEFAULT 0;
ALTER TABLE IF EXISTS players ALTER COLUMN account_id SET DEFAULT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN account_pw SET DEFAULT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN discord_id SET DEFAULT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN chat_muted SET DEFAULT FALSE;
ALTER TABLE IF EXISTS players ALTER COLUMN effect_visibility SET DEFAULT FALSE;
ALTER TABLE IF EXISTS players ALTER COLUMN effect_level SET DEFAULT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN effect_color SET DEFAULT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN hide_ranking SET DEFAULT FALSE;
ALTER TABLE IF EXISTS players ALTER COLUMN strict_mode SET DEFAULT FALSE;
ALTER TABLE IF EXISTS players ALTER COLUMN last_logout_date SET DEFAULT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN last_played_world_name SET DEFAULT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN last_played_world_mode SET DEFAULT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN is_connected SET DEFAULT FALSE;
ALTER TABLE IF EXISTS players ALTER COLUMN ban_expire_date SET DEFAULT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN attendance_days SET DEFAULT 0;
ALTER TABLE IF EXISTS players ALTER COLUMN permission SET DEFAULT 'default';
ALTER TABLE IF EXISTS players ALTER COLUMN level SET DEFAULT 0;
ALTER TABLE IF EXISTS players ALTER COLUMN exp SET DEFAULT 0;
ALTER TABLE IF EXISTS players ALTER COLUMN status SET DEFAULT '{}';

/* Drop NOT NULL constraint for nullable columns */
ALTER TABLE IF EXISTS players ALTER COLUMN account_id DROP NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN account_pw DROP NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN discord_id DROP NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN effect_level DROP NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN effect_color DROP NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN last_logout_date DROP NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN last_played_world_name DROP NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN last_played_world_mode DROP NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN ban_expire_date DROP NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN status DROP NOT NULL;

/* Initialize NULL values for existing rows */
UPDATE players SET first_played = CURRENT_TIMESTAMP WHERE first_played IS NULL;
UPDATE players SET last_played = CURRENT_TIMESTAMP WHERE last_played IS NULL;
UPDATE players SET language_tag = 'en' WHERE language_tag IS NULL OR language_tag = '';
UPDATE players SET permission = 'default' WHERE permission IS NULL OR permission = '';
UPDATE players SET block_place_count = 0 WHERE block_place_count IS NULL;
UPDATE players SET block_break_count = 0 WHERE block_break_count IS NULL;
UPDATE players SET total_played = 0 WHERE total_played IS NULL;
UPDATE players SET attack_clear = 0 WHERE attack_clear IS NULL;
UPDATE players SET pvp_win_count = 0 WHERE pvp_win_count IS NULL;
UPDATE players SET pvp_lose_count = 0 WHERE pvp_lose_count IS NULL;
UPDATE players SET pvp_eliminated_count = 0 WHERE pvp_eliminated_count IS NULL;
UPDATE players SET pvp_mvp_count = 0 WHERE pvp_mvp_count IS NULL;
UPDATE players SET level = 0 WHERE level IS NULL;
UPDATE players SET exp = 0 WHERE exp IS NULL;
UPDATE players SET chat_muted = FALSE WHERE chat_muted IS NULL;
UPDATE players SET effect_visibility = FALSE WHERE effect_visibility IS NULL;
UPDATE players SET hide_ranking = FALSE WHERE hide_ranking IS NULL;
UPDATE players SET strict_mode = FALSE WHERE strict_mode IS NULL;
UPDATE players SET is_connected = FALSE WHERE is_connected IS NULL;
UPDATE players SET attendance_days = 0 WHERE attendance_days IS NULL;
UPDATE players SET status = '{}' WHERE status IS NULL OR status = '';

/* Set NOT NULL constraints */
ALTER TABLE IF EXISTS players ALTER COLUMN first_played SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN last_played SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN last_login_date SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN language_tag SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN block_place_count SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN block_break_count SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN level SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN exp SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN total_played SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN attack_clear SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN pvp_win_count SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN pvp_lose_count SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN pvp_eliminated_count SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN pvp_mvp_count SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN permission SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN chat_muted SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN effect_visibility SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN hide_ranking SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN strict_mode SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN is_connected SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN attendance_days SET NOT NULL;

/* Column 길이 증가 */
ALTER TABLE IF EXISTS players ALTER COLUMN name TYPE VARCHAR(256);
ALTER TABLE IF EXISTS players ALTER COLUMN permission TYPE VARCHAR(100);

/* 플러그인 데이터 테이블 보장 */
CREATE TABLE IF NOT EXISTS plugin_data (
    id SERIAL PRIMARY KEY,
    database_version integer DEFAULT 4,
    hub_map_name text,
    data text
);

INSERT INTO plugin_data (hub_map_name, data)
SELECT
    (status::jsonb ->> 'hubMode'),
    jsonb_build_object(
        'warpZone', COALESCE(data::jsonb -> 'warpZones', '[]'::jsonb),
        'warpCount', COALESCE(data::jsonb -> 'warpCounts', '[]'::jsonb),
        'warpTotal', COALESCE(data::jsonb -> 'warpTotals', '[]'::jsonb),
        'warpBlock', COALESCE(data::jsonb -> 'warpBlocks', '[]'::jsonb),
        'blacklistedNames', COALESCE(data::jsonb -> 'blacklist', '[]'::jsonb),
        'mapRatings', COALESCE(data::jsonb -> 'mapRatings', '{}'::jsonb)
    )::text
FROM (
    SELECT
        data,
        (data::json ->> 'status')::jsonb as status
    FROM public.data
) sub
WHERE EXISTS (SELECT 1 FROM pg_tables WHERE tablename = 'data' AND schemaname = 'public')
AND NOT EXISTS (SELECT 1 FROM plugin_data)
LIMIT 1;

/* data 테이블 삭제 */
DROP TABLE IF EXISTS public.data;

/* 새 데이터 추가 */
ALTER TABLE IF EXISTS players ADD COLUMN IF NOT EXISTS wave_clear integer DEFAULT 0;
ALTER TABLE IF EXISTS players ADD COLUMN IF NOT EXISTS is_banned boolean DEFAULT false;
ALTER TABLE IF EXISTS players ALTER COLUMN wave_clear SET NOT NULL;
ALTER TABLE IF EXISTS players ALTER COLUMN is_banned SET NOT NULL;

UPDATE players SET wave_clear = 0 WHERE wave_clear IS NULL;
UPDATE players SET is_banned = false WHERE is_banned IS NULL;
UPDATE plugin_data SET database_version = 4 WHERE database_version IS NULL;

/* 데이터가 비어있을 경우 초기 데이터 삽입 */
INSERT INTO plugin_data (database_version, data) SELECT 4, '{"warpZone":[],"warpCount":[],"warpTotal":[],"warpBlock":[],"blacklistedNames":[],"mapRatings":{}}' WHERE NOT EXISTS (SELECT 1 FROM plugin_data);

/* 더이상 사용되지 않는 Table 삭제 */
DROP TABLE IF EXISTS db;

/* players 테이블에 id 컬럼 추가 */
ALTER TABLE IF EXISTS players ADD COLUMN IF NOT EXISTS id BIGSERIAL PRIMARY KEY;

/* 새 테이블 추가 */
CREATE TABLE IF NOT EXISTS PLAYER_ACHIEVEMENTS (
    id BIGSERIAL PRIMARY KEY,
    player_id BIGINT NOT NULL,
    achievement_name VARCHAR(100) NOT NULL,
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT FK_PLAYER_ACHIEVEMENTS_PLAYER_ID__ID FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);
