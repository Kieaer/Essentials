/* 예약된 table 이름과 linux 문제 해결 */
ALTER TABLE IF EXISTS player RENAME TO players;
ALTER TABLE IF EXISTS banned RENAME TO player_banned;

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

/* RDBMS Linux 기본 설정으로 인한 이름 변경 */
ALTER TABLE IF EXISTS players RENAME COLUMN "blockPlaceCount" to block_place_count;
ALTER TABLE IF EXISTS players RENAME COLUMN "blockBreakCount" to block_break_count;
ALTER TABLE IF EXISTS players RENAME COLUMN "firstPlayDate" to first_played;
ALTER TABLE IF EXISTS players RENAME COLUMN "lastLoginTime" to last_played;
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
ALTER TABLE IF EXISTS players ALTER COLUMN first_played TYPE TIMESTAMP WITHOUT TIME ZONE USING to_timestamp(first_played / 1000.0);
ALTER TABLE IF EXISTS players ALTER COLUMN last_played TYPE TIMESTAMP WITHOUT TIME ZONE USING to_timestamp(last_played / 1000.0);
ALTER TABLE IF EXISTS players ALTER COLUMN last_login_date TYPE TIMESTAMP WITHOUT TIME ZONE USING (CASE WHEN last_login_date ~ '^\d{4}-\d{2}-\d{2}' THEN last_login_date::timestamp ELSE NULL END);
ALTER TABLE IF EXISTS players ALTER COLUMN last_logout_date TYPE TIMESTAMP WITHOUT TIME ZONE USING (CASE WHEN last_logout_date ~ '^\d{4}-\d{2}-\d{2}' THEN last_logout_date::timestamp ELSE NULL END);
ALTER TABLE IF EXISTS players ALTER COLUMN ban_expire_date TYPE TIMESTAMP WITHOUT TIME ZONE USING (CASE WHEN ban_expire_date ~ '^\d{4}-\d{2}-\d{2}' THEN ban_expire_date::timestamp ELSE NULL END);

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
ALTER TABLE IF EXISTS players ADD COLUMN IF NOT EXISTS wave_clear integer DEFAULT 0 NOT NULL;
ALTER TABLE IF EXISTS players ADD COLUMN IF NOT EXISTS is_banned boolean DEFAULT false NOT NULL;

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
    completed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT FK_PLAYER_ACHIEVEMENTS_PLAYER_ID__ID FOREIGN KEY (player_id) REFERENCES players(id) ON DELETE RESTRICT ON UPDATE RESTRICT
);
