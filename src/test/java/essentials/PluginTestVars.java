package essentials;

public class PluginTestVars {
    public final boolean clean = true;
    public final boolean discordTest = false;

    public final String config = "{\n" +
            "  \"settings\": {\n" +
            "    \"version\": 13,\n" +
            "    \"language\": \"ko-KR\",\n" +
            "    \"logging\": true,\n" +
            "    \"update\": true,\n" +
            "    \"debug\": false,\n" +
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
            "    \"server-enable\": false,\n" +
            "    \"server-port\": 25000,\n" +
            "    \"client-enable\": false,\n" +
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
            "  \"essentials.features\": {\n" +
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
}
