--
-- PostgreSQL version-3 fixture for Testcontainers.
-- All records in this fixture are synthetic migration data.
--

CREATE TABLE public.banned (
    type integer NOT NULL,
    data text NOT NULL
);

ALTER TABLE public.banned OWNER TO plugins;

CREATE TABLE public.data (
    data text NOT NULL
);

ALTER TABLE public.data OWNER TO plugins;

CREATE TABLE public.db (
    version integer NOT NULL
);

ALTER TABLE public.db OWNER TO plugins;

CREATE TABLE public.player (
    name text NOT NULL,
    uuid text NOT NULL,
    "languageTag" text NOT NULL,
    "blockPlaceCount" integer NOT NULL,
    "blockBreakCount" integer NOT NULL,
    "totalJoinCount" integer NOT NULL,
    "totalKickCount" integer NOT NULL,
    level integer NOT NULL,
    exp integer NOT NULL,
    "firstPlayDate" bigint NOT NULL,
    "lastLoginTime" bigint NOT NULL,
    "totalPlayTime" bigint NOT NULL,
    "attackModeClear" integer NOT NULL,
    "pvpVictoriesCount" integer NOT NULL,
    "pvpDefeatCount" integer NOT NULL,
    "animatedName" boolean NOT NULL,
    permission text NOT NULL,
    mute boolean NOT NULL,
    "accountID" text NOT NULL,
    "accountPW" text NOT NULL,
    status text NOT NULL,
    discord text,
    "effectLevel" integer,
    "effectColor" text,
    "hideRanking" boolean NOT NULL,
    "freeze" boolean NOT NULL,
    hud text,
    tpp text,
    "tppTeam" integer,
    log boolean NOT NULL,
    "oldUUID" text,
    "banTime" text,
    "duplicateName" text,
    tracking boolean NOT NULL,
    "joinStacks" integer NOT NULL,
    "lastLoginDate" text,
    "lastLeaveDate" text,
    "showLevelEffects" boolean NOT NULL,
    "currentPlayTime" bigint NOT NULL,
    "isConnected" boolean NOT NULL,
    "lastPlayedWorldName" text,
    "lastPlayedWorldMode" text,
    "lastPlayedWorldId" integer,
    "mvpTime" integer NOT NULL,
    "pvpEliminationTeamCount" integer NOT NULL,
    strict boolean NOT NULL
);

ALTER TABLE public.player OWNER TO plugins;

INSERT INTO public.banned (type, data) VALUES (0, 'test-banned-player');
INSERT INTO public.banned (type, data) VALUES (1, '203.0.113.7');
INSERT INTO public.data (data) VALUES ('{"warpZones":[],"warpBlocks":[],"warpCounts":[],"warpTotals":[],"blacklist":[],"banned":[],"status":"{}"}');
INSERT INTO public.db (version) VALUES (3);
INSERT INTO public.db (version) VALUES (3);

INSERT INTO public.player (name, uuid, "languageTag", "blockPlaceCount", "blockBreakCount", "totalJoinCount", "totalKickCount", level, exp, "firstPlayDate", "lastLoginTime", "totalPlayTime", "attackModeClear", "pvpVictoriesCount", "pvpDefeatCount", "animatedName", permission, mute, "accountID", "accountPW", status, discord, "effectLevel", "effectColor", "hideRanking", "freeze", hud, tpp, "tppTeam", log, "oldUUID", "banTime", "duplicateName", tracking, "joinStacks", "lastLoginDate", "lastLeaveDate", "showLevelEffects", "currentPlayTime", "isConnected", "lastPlayedWorldName", "lastPlayedWorldMode", "lastPlayedWorldId", "mvpTime", "pvpEliminationTeamCount", strict) VALUES ('migration-user', 'migration-test-player', 'ko', 122213, 0, 1, 0, 1, 56, 0, 0, 0, 0, 0, 0, false, 'owner', false, 'migration-user', 'test-password-hash', '{"language":"ko"}', NULL, 0, '#000000', false, false, NULL, NULL, NULL, false, NULL, NULL, NULL, false, 0, '2025-01-01', '2025-01-01T00:00:00', true, 0, false, NULL, NULL, 0, 0, 0, false);

CREATE INDEX player_name ON public.player USING btree (name);
