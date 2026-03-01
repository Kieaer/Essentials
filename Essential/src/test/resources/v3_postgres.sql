--
-- PostgreSQL database dump simplified for Testcontainers
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

INSERT INTO public.banned (type, data) VALUES (0, 'Mh94dHKb+dcAAAAANcaMaA==');
INSERT INTO public.banned (type, data) VALUES (1, '219.249.17.64');

INSERT INTO public.data (data) VALUES ('{"warpZones":[],"warpBlocks":[{"mapName":"Lobby","x":56,"y":76,"tileName":"core-bastion","size":4,"ip":"mindustry.kr","port":7107,"description":"[yellow]샌드박스"},{"mapName":"Lobby","x":42,"y":76,"tileName":"core-bastion","size":4,"ip":"mindustry.kr","port":7100,"description":"[orange]에르키아[] - [scarlet]공격"},{"mapName":"Lobby","x":29,"y":69,"tileName":"core-bastion","size":4,"ip":"mindustry.kr","port":7106,"description":"[orange]에르키아[] - [red]PvP"},{"mapName":"Lobby","x":22,"y":56,"tileName":"core-bastion","size":4,"ip":"mindustry.kr","port":7108,"description":"[orange]에르키아[] - [green]생존"},{"mapName":"Lobby","x":76,"y":42,"tileName":"core-bastion","size":4,"ip":"mindustry.kr","port":7104,"description":"[sky]세르플로[] - [scarlet]공격"},{"mapName":"Lobby","x":69,"y":29,"tileName":"core-bastion","size":4,"ip":"mindustry.kr","port":7105,"description":"[sky]세르플로[] - [red]PvP"},{"mapName":"Lobby","x":56,"y":22,"tileName":"core-bastion","size":4,"ip":"mindustry.kr","port":7103,"description":"[sky]세르플로[] - [green]생존"},{"mapName":"Lobby","x":69,"y":69,"tileName":"core-bastion","size":4,"ip":"mindustry.kr","port":7109,"description":"[scarlet]마[green]참[blue]내[yellow] 고쳐진 로직미술관"}],"warpCounts":[],"warpTotals":[],"blacklist":[],"banned":[],"status":"{\"iptablesFirst\":\"none\",\"Lobby\":\"none\",\"hubMode\":\"Lobby\"}"}');

INSERT INTO public.db (version) VALUES (3);
INSERT INTO public.db (version) VALUES (3);

INSERT INTO public.player (name, uuid, "languageTag", "blockPlaceCount", "blockBreakCount", "totalJoinCount", "totalKickCount", level, exp, "firstPlayDate", "lastLoginTime", "totalPlayTime", "attackModeClear", "pvpVictoriesCount", "pvpDefeatCount", "animatedName", permission, mute, "accountID", "accountPW", status, discord, "effectLevel", "effectColor", "hideRanking", "freeze", hud, tpp, "tppTeam", log, "oldUUID", "banTime", "duplicateName", tracking, "joinStacks", "lastLoginDate", "lastLeaveDate", "showLevelEffects", "currentPlayTime", "isConnected", "lastPlayedWorldName", "lastPlayedWorldMode", "lastPlayedWorldId", "mvpTime", "pvpEliminationTeamCount", strict) VALUES ('Gureumi', 'hMHCIDJpHKQAAAAAbzCq5A==', 'ko', 122213, 31833, 1572, 0, 245, 30476956, 1668345446430, 1770117510029, 2875632, 189, 18, 28, false, 'owner', false, 'Gureumi', '$2a$10$cvrUBihEx3VwSAg00lCtVO57otRSiLimc3LaMkxckhH7jJtomlmOq', '{"effectLevel":"0","record.wave":"503","record.time.meetowner":"217960","effectColor":"#87CEEB1A","achievement.meetowner":"2024-08-12T21:16:34.7399908","achievement.builder":"2024-08-05T15:32:40.6456128","language":"ko","record.time.serpulo":"184207","record.time.erekir":"34703","record.time.chat":"779","achievement.aggressor":"2024-08-05T16:31:41.1657939","hud":"[]","record.time.sandbox":"82461","page":"0"}', 'f', 230, '#27e2f501', false, false, NULL, NULL, NULL, false, NULL, 10080, NULL, false, 0, '2026-02-03', '2026-02-03T20:20:26.534709217', true, 47, false, 'Scrap Island', NULL, 6567, 0, 29, false);

CREATE INDEX player_name ON public.player USING btree (name);
