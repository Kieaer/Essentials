package essentials;

public class PluginTestVars {
    public final boolean clean = true;

    public final String config = "{\n" +
            "  \"settings\": {\n" +
            "    \"version\": 13,\n" +
            "    \"language\": \"ko-KR\",\n" +
            "    \"logging\": true,\n" +
            "    \"update\": true,\n" +
            "    \"debug\": true,\n" +
            "    \"debugcode\": \"none\",\n" +
            "    \"crash-report\": true,\n" +
            "    \"prefix\": \"[green][Essentials] []\",\n" +
            "    \"database\": {\n" +
            "      \"internalDB\": true,\n" +
            "      \"DBServer\": true,\n" +
            "      \"DBurl\": \"jdbc:h2:file:./config/mods/Essentials/data/player\",\n" +
            "      \"old-db-migration\": false,\n" +
            "      \"old-db-url\": \"jdbc:sqlite:config/mods/Essentials/data/player.sqlite3\",\n" +
            "      \"old-db-id\": \"none\",\n" +
            "      \"old-db-pw\": \"none\",\n" +
            "      \"data-server-url\": \"none\",\n" +
            "      \"data-server-id\": \"none\",\n" +
            "      \"data-server-pw\": \"none\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"network\": {\n" +
            "    \"server-enable\": true,\n" +
            "    \"server-port\": 25000,\n" +
            "    \"client-enable\": true,\n" +
            "    \"client-port\": 25000,\n" +
            "    \"client-host\": \"127.0.0.1\",\n" +
            "    \"banshare\": true,\n" +
            "    \"bantrust\": [\n" +
            "      \"127.0.0.1\",\n" +
            "      \"localhost\"\n" +
            "    ],\n" +
            "    \"query\": true\n" +
            "  },\n" +
            "  \"antigrief\": {\n" +
            "    \"antigrief\": true,\n" +
            "    \"antivpn\": true,\n" +
            "    \"antirush\": true,\n" +
            "    \"antirushtime\": \"00:10:00\",\n" +
            "    \"alert-action\": true,\n" +
            "    \"realname\": true,\n" +
            "    \"strict-name\": true,\n" +
            "    \"scanresource\": true\n" +
            "  },\n" +
            "  \"features\": {\n" +
            "    \"explimit\": true,\n" +
            "    \"basexp\": 500,\n" +
            "    \"exponent\": 1.12,\n" +
            "    \"levelupalarm\": true,\n" +
            "    \"alarm-minimal-level\": 20,\n" +
            "    \"vote\": true,\n" +
            "    \"savetime\": \"00:10:00\",\n" +
            "    \"rollback\": true,\n" +
            "    \"slotnumber\": 1000,\n" +
            "    \"border\": true,\n" +
            "    \"spawnlimit\": 500,\n" +
            "    \"eventport\": \"8000-8050\",\n" +
            "    \"cupdatei\": 1000,\n" +
            "    \"difficulty\": {\n" +
            "      \"auto-difficulty\": true,\n" +
            "      \"easy\": 2,\n" +
            "      \"normal\": 4,\n" +
            "      \"hard\": 6,\n" +
            "      \"insane\": 10\n" +
            "    },\n" +
            "    \"translate\": {\n" +
            "      \"translate\": false,\n" +
            "      \"translateid\": \"none\",\n" +
            "      \"translatepw\": \"none\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"auth\": {\n" +
            "    \"loginenable\": true,\n" +
            "    \"loginmethod\": \"password\",\n" +
            "    \"validconnect\": true,\n" +
            "    \"autologin\": true,\n" +
            "    \"discord\": {\n" +
            "      \"token\": \"none\",\n" +
            "      \"link\": \"none\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
    public final String sql = "CREATE TABLE IF NOT EXISTS players (\n" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
            "name TEXT,\n" +
            "uuid TEXT,\n" +
            "country TEXT,\n" +
            "country_code TEXT,\n" +
            "language TEXT,\n" +
            "isadmin TEXT,\n" +
            "placecount INTEGER,\n" +
            "breakcount INTEGER,\n" +
            "killcount INTEGER,\n" +
            "deathcount INTEGER,\n" +
            "joincount INTEGER,\n" +
            "kickcount INTEGER,\n" +
            "level INTEGER,\n" +
            "exp INTEGER,\n" +
            "reqexp INTEGER,\n" +
            "reqtotalexp TEXT,\n" +
            "firstdate TEXT,\n" +
            "lastdate TEXT,\n" +
            "lastplacename TEXT,\n" +
            "lastbreakname TEXT,\n" +
            "lastchat TEXT,\n" +
            "playtime TEXT,\n" +
            "attackclear INTEGER,\n" +
            "pvpwincount INTEGER,\n" +
            "pvplosecount INTEGER,\n" +
            "pvpbreakout INTEGER,\n" +
            "reactorcount INTEGER,\n" +
            "bantimeset INTEGER,\n" +
            "bantime TEXT,\n" +
            "banned TEXT,\n" +
            "translate TEXT,\n" +
            "crosschat TEXT,\n" +
            "colornick TEXT,\n" +
            "connected TEXT,\n" +
            "connserver TEXT,\n" +
            "permission TEXT,\n" +
            "mute TEXT,\n" +
            "udid TEXT,\n" +
            "accountid TEXT,\n" +
            "accountpw TEXT\n" +
            ");";
    public final String json = "{\n" +
            "  version: 10\n" +
            "  language: en\n" +
            "  server-enable: false\n" +
            "  server-port: 25000\n" +
            "  client-enable: false\n" +
            "  client-port: 25000\n" +
            "  client-host: mindustry.kr\n" +
            "  realname: false\n" +
            "  strict-name: false\n" +
            "  cupdatei: 1000\n" +
            "  detectreactor: false\n" +
            "  scanresource: false\n" +
            "  antigrief: false\n" +
            "  alert-blockdetect: false\n" +
            "  alert-deposit: false\n" +
            "  explimit: false\n" +
            "  basexp: 500.0\n" +
            "  exponent: 1.1200000047683716\n" +
            "  levelupalarm: false\n" +
            "  alarm-minimal-level: 20\n" +
            "  banshare: false\n" +
            "  bantrust: none\n" +
            "  query: false\n" +
            "  antivpn: false\n" +
            "  enable-antirush: false\n" +
            "  antirushtime: 10:00\n" +
            "  logging: false\n" +
            "  update: true\n" +
            "  sqlite: true\n" +
            "  dburl: jdbc:sqlite:config/mods/Essentials/data/player.sqlite3\n" +
            "  dbid: none\n" +
            "  dbpw: none\n" +
            "  loginenable: false\n" +
            "  loginmethod: password\n" +
            "  validconnect: false\n" +
            "  discord-token: Put your discord bot token here\n" +
            "  discord-guild: 0\n" +
            "  discord-room: none\n" +
            "  discord-link: Put your discord invite link here\n" +
            "  discord-register-role: none\n" +
            "  discord-command-prefix: !\n" +
            "  enable-translate: false\n" +
            "  clientId: none\n" +
            "  clientSecret: none\n" +
            "  debug: true\n" +
            "  savetime: 10\n" +
            "  enable-rollback: false\n" +
            "  slotnumber: 1000\n" +
            "  auto-difficulty: false\n" +
            "  easy: 2\n" +
            "  normal: 4\n" +
            "  hard: 6\n" +
            "  insane: 10\n" +
            "  border: false\n" +
            "  spawnlimit: 30\n" +
            "  prefix: \"[green][Essentials] []\"\n" +
            "  event-port: 8000-8050\n" +
            "}";
}
