package essentials.core.plugin;

import essentials.internal.Bundle;
import essentials.internal.Log;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.Stringify;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static essentials.Main.*;
import static essentials.PluginVars.config_version;

public class Config {
    public int version;
    public Locale language;
    public boolean serverenable;
    public int serverport;
    public boolean clientenable;
    public int clientport;
    public String clienthost;
    public boolean realname;
    public boolean strictname;
    public int cupdatei;
    public boolean scanresource;
    public boolean antigrief;
    public boolean alertaction;
    public boolean explimit;
    public double basexp;
    public double exponent;
    public boolean levelupalarm;
    public int alarmlevel;
    public boolean banshare;
    public JsonArray bantrust;
    public boolean query;
    public boolean antivpn;
    public boolean antirush;
    public LocalTime antirushtime;
    public boolean vote;
    public boolean logging;
    public boolean update;
    public boolean internalDB;
    public boolean DBServer;
    public String DBurl;
    public boolean OldDBMigration;
    public String OldDBurl;
    public String OldDBID;
    public String OldDBPW;
    public String dataserverurl;
    public String dataserverid;
    public String dataserverpw;
    public boolean loginenable;
    public String passwordmethod;
    public boolean validconnect;
    public boolean autologin;
    public String discordtoken;
    public String discordlink;
    public boolean translate;
    public String translateid;
    public String translatepw;
    public boolean debug;
    public String debugcode;
    public boolean crashreport;
    public LocalTime savetime;
    public boolean rollback;
    public int slotnumber;
    public boolean autodifficulty;
    public int difficultyEasy;
    public int difficultyNormal;
    public int difficultyHard;
    public int difficultyInsane;
    public boolean border;
    public int spawnlimit;
    public String prefix;
    public String eventport;

    JsonObject obj;

    public Config() {
        JsonObject settings;
        JsonObject database;
        JsonObject network;
        JsonObject anti;
        JsonObject features;
        JsonObject difficulty;
        JsonObject tr;
        JsonObject auth;
        JsonObject discord;
        try {
            obj = JsonValue.readHjson(root.child("config.hjson").readString()).asObject();
            try {
                obj.get("settings").asObject();
            } catch (NullPointerException ignored) {
                LegacyUpgrade();
            }
        } catch (RuntimeException e) {
            JsonObject empty = new JsonObject();
            obj = new JsonObject();
            obj.add("settings", new JsonObject().add("database", empty));
            obj.add("network", empty);
            obj.add("antigrief", empty);
            obj.add("features", new JsonObject().add("difficulty", empty).add("translate", empty));
            obj.add("auth", new JsonObject().add("discord", empty));
        }

        settings = obj.get("settings").asObject();
        version = settings.getInt("version", config_version);
        language = new Locale(settings.getString("language", System.getProperty("user.language") + "_" + System.getProperty("user.country")));
        logging = settings.getBoolean("logging", true);
        update = settings.getBoolean("update", true);
        debug = settings.getBoolean("debug", false);
        debugcode = settings.getString("debugcode", "none");
        crashreport = settings.getBoolean("crashreport", true);
        prefix = settings.getString("prefix", "[green][Essentials] []");

        database = settings.get("database").asObject();
        internalDB = database.getBoolean("internalDB", true);
        DBServer = database.getBoolean("DBServer", false);
        DBurl = database.getString("DBurl", "jdbc:h2:file:./config/mods/Essentials/data/player");
        OldDBMigration = database.getBoolean("OldDBMigration", false);
        OldDBurl = database.getString("OldDBurl", "jdbc:sqlite:config/mods/Essentials/data/player.sqlite3");
        OldDBID = database.getString("OldDBID", "none");
        OldDBPW = database.getString("OldDBPW", "none");
        dataserverurl = database.getString("dataserverurl", "none");
        dataserverid = database.getString("dataserverid", "none");
        dataserverpw = database.getString("dataserverpw", "none");

        network = obj.get("network").asObject();
        serverenable = network.getBoolean("serverenable", false);
        serverport = network.getInt("serverport", 25000);
        clientenable = network.getBoolean("clientenable", false);
        clientport = network.getInt("clientport", 25000);
        clienthost = network.getString("clienthost", "mindustry.kr");
        banshare = network.getBoolean("banshare", false);
        bantrust = network.get("bantrust") == null ? JsonArray.readJSON("[\"127.0.0.1\",\"localhost\"]").asArray() : network.get("bantrust").asArray();
        query = network.getBoolean("query", false);

        anti = obj.get("antigrief").asObject();
        antigrief = anti.getBoolean("antigrief", false);
        antivpn = anti.getBoolean("antivpn", false);
        antirush = anti.getBoolean("antirush", false);
        antirushtime = LocalTime.parse(anti.getString("antirushtime", "00:10:00"), DateTimeFormatter.ofPattern("HH:mm:ss"));
        alertaction = anti.getBoolean("alertaction", false);
        realname = anti.getBoolean("realname", false);
        strictname = anti.getBoolean("strictname", false);
        scanresource = anti.getBoolean("scanresource", false);

        features = obj.get("features").asObject();
        explimit = features.getBoolean("explimit", false);
        basexp = features.getDouble("basexp", 500.0);
        exponent = features.getDouble("exponent", 1.12);
        levelupalarm = features.getBoolean("levelupalarm", false);
        alarmlevel = features.getInt("alarmlevel", 20);
        vote = features.getBoolean("vote", true);
        savetime = LocalTime.parse(features.getString("savetime", "00:10:00"), DateTimeFormatter.ofPattern("HH:mm:ss"));
        rollback = features.getBoolean("rollback", false);
        slotnumber = features.getInt("slotnumber", 1000);
        border = features.getBoolean("border", false);
        spawnlimit = features.getInt("spawnlimit", 500);
        eventport = features.getString("eventport", "8000-8050");
        cupdatei = features.getInt("cupdatei", 1000);

        difficulty = features.get("difficulty").asObject();
        autodifficulty = difficulty.getBoolean("autodifficulty", false);
        difficultyEasy = difficulty.getInt("difficultyEasy", 2);
        difficultyNormal = difficulty.getInt("difficultyNormal", 4);
        difficultyHard = difficulty.getInt("difficultyHard", 6);
        difficultyInsane = difficulty.getInt("difficultyInsane", 10);

        tr = features.get("translate").asObject();
        translate = tr.getBoolean("translate", false);
        translateid = tr.getString("translateid", "none");
        translatepw = tr.getString("translatepw", "none");

        auth = obj.get("auth").asObject();
        loginenable = auth.getBoolean("loginenable", false);
        passwordmethod = auth.getString("loginmethod", "password");
        validconnect = auth.getBoolean("validconnect", false);
        autologin = auth.getBoolean("autologin", true);

        discord = auth.get("discord").asObject();
        discordtoken = discord.getString("token", "none");
        discordlink = discord.getString("link", "none");

        update();
    }

    public void version(int version) {
        this.version = version;
    }

    public void language(Locale language) {
        this.language = language;
    }

    public void translate(boolean translate) {
        this.translate = translate;
    }

    public void crashreport(boolean crashreport) {
        this.crashreport = crashreport;
    }

    public void oldDBMigration(boolean oldDBMigration) {
        this.OldDBMigration = oldDBMigration;
    }

    public void update() {
        locale = tool.TextToLocale(obj.getString("language", locale.toString()));
        Bundle bundle = new Bundle(locale);

        if (obj.getInt("version", 0) < config_version) Log.info("config-updated");

        JsonObject config = new JsonObject();
        JsonObject settings = new JsonObject();
        JsonObject db = new JsonObject();
        JsonObject network = new JsonObject();
        JsonObject anti = new JsonObject();
        JsonObject features = new JsonObject();
        JsonObject difficulty = new JsonObject();
        JsonObject auth = new JsonObject();
        JsonObject discord = new JsonObject();
        JsonObject tr = new JsonObject();

        config.add("settings", settings, bundle.get("config-description"));
        config.add("network", network);
        config.add("antigrief", anti);
        config.add("features", features);
        config.add("auth", auth);

        // 플러그인 설정
        settings.add("version", version, bundle.get("config-version-description"));
        settings.add("language", language.toString(), bundle.get("config-language-description"));
        settings.add("logging", logging, bundle.get("config-logging-description"));
        settings.add("update", update, bundle.get("config-update-description"));
        //settings.add(CommentType.BOL, CommentStyle.BLOCK,"\n\nasdkfjlkfkjdaslkfjdaslkfjdsalkfjdsalkfjadsflkajdsflkasjflkdasjflks");
        settings.add("debug", debug, bundle.get("config-debug-description"));
        settings.add("debugcode", debugcode);
        settings.add("crash-report", crashreport);
        //settings.setLineLength(1);
        settings.add("prefix", prefix, bundle.get("config-prefix-description"));
        //settings.setLineLength(1);

        // DB 설정 (settings 상속)
        settings.add("database", db);
        db.add("internalDB", internalDB, bundle.get("config-database-description"));
        db.add("DBServer", DBServer);
        db.add("DBurl", DBurl);
        //db.setLineLength(1);
        db.add("old-db-migration", OldDBMigration, bundle.get("config-old-database-migration-description"));
        db.add("old-db-url", OldDBurl);
        db.add("old-db-id", OldDBID);
        db.add("old-db-pw", OldDBPW);
        //db.setLineLength(1);
        db.add("data-server-url", dataserverurl, bundle.get("config-data-share-description"));
        db.add("data-server-id", dataserverid);
        db.add("data-server-pw", dataserverpw);

        // 네트워크 설정
        network.add("server-enable", serverenable, bundle.get("config-network-description"));
        network.add("server-port", serverport);
        network.add("client-enable", clientenable);
        network.add("client-port", clientport);
        network.add("client-host", clienthost);
        //network.setLineLength(1);
        network.add("banshare", banshare, bundle.get("config-banshare-description"));
        network.add("bantrust", bantrust, bundle.get("config-bantrust-description"));
        //network.setLineLength(1);
        network.add("query", query, bundle.get("config-query-description"));

        // 테러방지 설정
        anti.add("antigrief", antigrief, bundle.get("config-antigrief-description"));
        anti.add("antivpn", antivpn, bundle.get("config-antivpn-description"));
        anti.add("antirush", antirush, bundle.get("config-antirush-description"));
        anti.add("antirushtime", antirushtime.format(DateTimeFormatter.ofPattern("HH:mm:ss")), bundle.get("config-antirushtime-description"));
        anti.add("alert-action", alertaction, bundle.get("config-alert-action-description"));
        anti.add("realname", realname, bundle.get("config-realname-description"));
        anti.add("strict-name", strictname, bundle.get("config-strict-name-description"));
        anti.add("scanresource", scanresource, bundle.get("config-scanresource-description"));

        // 특별한 기능 설정
        features.add("explimit", explimit, bundle.get("config-exp-explimit-description"));
        features.add("basexp", basexp, bundle.get("config-exp-basexp-description"));
        features.add("exponent", exponent, bundle.get("config-exp-exponent-description"));
        features.add("levelupalarm", levelupalarm, bundle.get("config-exp-levelupalarm-description"));
        features.add("alarm-minimal-level", alarmlevel, bundle.get("config-exp-minimal-level-description"));
        features.add("vote", vote, bundle.get("config-vote-description"));
        features.add("savetime", savetime.format(DateTimeFormatter.ofPattern("HH:mm:ss")), bundle.get("config-savetime-description"));
        features.add("rollback", rollback, bundle.get("config-slotnumber-description"));
        features.add("slotnumber", slotnumber);
        features.add("border", border, bundle.get("config-border-description"));
        features.add("spawnlimit", spawnlimit, bundle.get("config-spawnlimit-description"));
        features.add("eventport", eventport, bundle.get("config-event-port-description"));
        features.add("cupdatei", cupdatei, bundle.get("config-colornick-description"));

        // 난이도 설정 (features 상속)
        features.add("difficulty", difficulty, bundle.get("config-auto-difficulty-description"));
        difficulty.add("auto-difficulty", autodifficulty);
        difficulty.add("easy", difficultyEasy);
        difficulty.add("normal", difficultyNormal);
        difficulty.add("hard", difficultyHard);
        difficulty.add("insane", difficultyInsane);

        // 번역 설정 (features 상속)
        features.add("translate", tr, bundle.get("config-papago-description"));
        tr.add("translate", translate);
        tr.add("translateid", translateid);
        tr.add("translatepw", translatepw);

        // 로그인 설정
        auth.add("loginenable", loginenable, bundle.get("config-login-description"));
        auth.add("loginmethod", passwordmethod, bundle.get("config-loginmethod-description"));
        auth.add("validconnect", validconnect, bundle.get("config-validconnect-description"));
        auth.add("autologin", autologin);

        // Discord 설정 (auth 상속)
        //auth.setLineLength(1);
        auth.add("discord", discord, bundle.get("config-discord-description"));
        discord.add("token", discordtoken);
        discord.add("link", discordlink);

        root.child("config.hjson").writeString(config.toString(Stringify.HJSON_COMMENTS));
    }

    public void LegacyUpgrade() {
        int version = obj.getInt("version", config_version);
        Locale language = new Locale(obj.getString("language", System.getProperty("user.language") + "_" + System.getProperty("user.country")));
        boolean serverenable = obj.getBoolean("serverenable", false);
        int serverport = obj.getInt("serverport", 25000);
        boolean clientenable = obj.getBoolean("clientenable", false);
        int clientport = obj.getInt("clientport", 25000);
        String clienthost = obj.getString("clienthost", "mindustry.kr");
        boolean realname = obj.getBoolean("realname", false);
        boolean strictname = obj.getBoolean("strictname", false);
        int cupdatei = obj.getInt("cupdatei", 1000);
        boolean scanresource = obj.getBoolean("scanresource", false);
        boolean antigrief = obj.getBoolean("antigrief", false);
        boolean alertaction = obj.getBoolean("alertaction", false);
        boolean explimit = obj.getBoolean("explimit", false);
        double basexp = obj.getDouble("basexp", 500.0);
        double exponent = obj.getDouble("exponent", 1.12);
        boolean levelupalarm = obj.getBoolean("levelupalarm", false);
        int alarmlevel = obj.getInt("alarmlevel", 20);
        boolean banshare = obj.getBoolean("banshare", false);
        JsonArray bantrust = obj.get("bantrust") == null ? JsonArray.readJSON("[\"127.0.0.1\",\"localhost\"]").asArray() : obj.get("bantrust").asArray();
        boolean query = obj.getBoolean("query", false);
        boolean antivpn = obj.getBoolean("antivpn", false);
        boolean antirush = obj.getBoolean("antirush", false);
        LocalTime antirushtime = LocalTime.parse(obj.getString("antirushtime", "00:10:00"), DateTimeFormatter.ofPattern("HH:mm:ss"));
        boolean logging = obj.getBoolean("logging", true);
        boolean update = obj.getBoolean("update", true);
        boolean internalDB = obj.getBoolean("internalDB", true);
        boolean DBServer = obj.getBoolean("DBServer", false);
        String DBurl = obj.getString("DBurl", "jdbc:h2:file:./config/mods/Essentials/data/player");
        boolean OldDBMigration = obj.getBoolean("OldDBMigration", false);
        String OldDBurl = obj.getString("OldDBurl", "jdbc:sqlite:config/mods/Essentials/data/player.sqlite3");
        String OldDBID = obj.getString("OldDBID", "none");
        String OldDBPW = obj.getString("OldDBPW", "none");
        String dataserverurl = obj.getString("dataserverurl", "none");
        String dataserverid = obj.getString("dataserverid", "none");
        String dataserverpw = obj.getString("dataserverpw", "none");
        boolean loginenable = obj.getBoolean("loginenable", false);
        String passwordmethod = obj.getString("passwordmethod", "password");
        boolean validconnect = obj.getBoolean("validconnect", false);
        String discordtoken = obj.getString("discordtoken", "none");
        String discordlink = obj.getString("discordlink", "none");
        boolean translate = obj.getBoolean("translate", false);
        String translateid = obj.getString("translateid", "none");
        String translatepw = obj.getString("translatepw", "none");
        boolean debug = obj.getBoolean("debug", false);
        String debugcode = obj.getString("debugcode", "none");
        boolean crashreport = obj.getBoolean("crashreport", true);
        LocalTime savetime = LocalTime.parse(obj.getString("savetime", "00:10:00"), DateTimeFormatter.ofPattern("HH:mm:ss"));
        boolean rollback = obj.getBoolean("rollback", false);
        int slotnumber = obj.getInt("slotnumber", 1000);
        boolean autodifficulty = obj.getBoolean("autodifficulty", false);
        int difficultyEasy = obj.getInt("difficultyEasy", 2);
        int difficultyNormal = obj.getInt("difficultyNormal", 4);
        int difficultyHard = obj.getInt("difficultyHard", 6);
        int difficultyInsane = obj.getInt("difficultyInsane", 10);
        boolean border = obj.getBoolean("border", false);
        int spawnlimit = obj.getInt("spawnlimit", 500);
        String prefix = obj.getString("prefix", "[green][Essentials] []");
        String eventport = obj.getString("eventport", "8000-8050");

        JsonObject empty = new JsonObject();
        obj = new JsonObject();
        obj.add("settings", new JsonObject().add("database", empty));
        obj.add("network", empty);
        obj.add("antigrief", empty);
        obj.add("features", new JsonObject().add("difficulty", empty).add("translate", empty));
        obj.add("auth", new JsonObject().add("discord", empty));

        locale = tool.TextToLocale(obj.getString("language", locale.toString()));
        Bundle bundle = new Bundle(locale);

        JsonObject config = new JsonObject();
        JsonObject settings = new JsonObject();
        JsonObject db = new JsonObject();
        JsonObject network = new JsonObject();
        JsonObject anti = new JsonObject();
        JsonObject features = new JsonObject();
        JsonObject difficulty = new JsonObject();
        JsonObject auth = new JsonObject();
        JsonObject discord = new JsonObject();
        JsonObject tr = new JsonObject();

        config.add("settings", settings, bundle.get("config-description"));
        config.add("network", network);
        config.add("antigrief", anti);
        config.add("features", features);
        config.add("auth", auth);

        settings.add("version", version, bundle.get("config-version-description"));
        settings.add("language", language.toString(), bundle.get("config-language-description"));
        settings.add("logging", logging, bundle.get("config-logging-description"));
        settings.add("update", update, bundle.get("config-update-description"));
        settings.add("debug", debug, bundle.get("config-debug-description"));
        settings.add("debugcode", debugcode);
        settings.add("crash-report", crashreport);
        settings.add("prefix", prefix, bundle.get("config-prefix-description"));

        settings.add("database", db);
        db.add("internalDB", internalDB, bundle.get("config-database-description"));
        db.add("DBServer", DBServer);
        db.add("DBurl", DBurl);
        db.add("old-db-migration", OldDBMigration, bundle.get("config-old-database-migration-description"));
        db.add("old-db-url", OldDBurl);
        db.add("old-db-id", OldDBID);
        db.add("old-db-pw", OldDBPW);
        db.add("data-server-url", dataserverurl, bundle.get("config-data-share-description"));
        db.add("data-server-id", dataserverid);
        db.add("data-server-pw", dataserverpw);

        network.add("server-enable", serverenable, bundle.get("config-network-description"));
        network.add("server-port", serverport);
        network.add("client-enable", clientenable);
        network.add("client-port", clientport);
        network.add("client-host", clienthost);
        network.add("banshare", banshare, bundle.get("config-banshare-description"));
        network.add("bantrust", bantrust, bundle.get("config-bantrust-description"));
        network.add("query", query, bundle.get("config-query-description"));

        anti.add("antigrief", antigrief, bundle.get("config-antigrief-description"));
        anti.add("antivpn", antivpn, bundle.get("config-antivpn-description"));
        anti.add("antirush", antirush, bundle.get("config-antirush-description"));
        anti.add("antirushtime", antirushtime.format(DateTimeFormatter.ofPattern("HH:mm:ss")), bundle.get("config-antirushtime-description"));
        anti.add("alert-action", alertaction, bundle.get("config-alert-action-description"));
        anti.add("realname", realname, bundle.get("config-realname-description"));
        anti.add("strict-name", strictname, bundle.get("config-strict-name-description"));
        anti.add("scanresource", scanresource, bundle.get("config-scanresource-description"));

        features.add("explimit", explimit, bundle.get("config-exp-explimit-description"));
        features.add("basexp", basexp, bundle.get("config-exp-basexp-description"));
        features.add("exponent", exponent, bundle.get("config-exp-exponent-description"));
        features.add("levelupalarm", levelupalarm, bundle.get("config-exp-levelupalarm-description"));
        features.add("alarm-minimal-level", alarmlevel, bundle.get("config-exp-minimal-level-description"));
        features.add("vote", true, bundle.get("config-vote-description"));
        features.add("savetime", savetime.format(DateTimeFormatter.ofPattern("HH:mm:ss")), bundle.get("config-savetime-description"));
        features.add("rollback", rollback, bundle.get("config-slotnumber-description"));
        features.add("slotnumber", slotnumber);
        features.add("border", border, bundle.get("config-border-description"));
        features.add("spawnlimit", spawnlimit, bundle.get("config-spawnlimit-description"));
        features.add("eventport", eventport, bundle.get("config-event-port-description"));
        features.add("cupdatei", cupdatei, bundle.get("config-colornick-description"));

        features.add("difficulty", difficulty, bundle.get("config-auto-difficulty-description"));
        difficulty.add("auto-difficulty", autodifficulty);
        difficulty.add("easy", difficultyEasy);
        difficulty.add("normal", difficultyNormal);
        difficulty.add("hard", difficultyHard);
        difficulty.add("insane", difficultyInsane);

        features.add("translate", tr, bundle.get("config-papago-description"));
        tr.add("translate", translate);
        tr.add("translateid", translateid);
        tr.add("translatepw", translatepw);

        auth.add("loginenable", loginenable, bundle.get("config-login-description"));
        auth.add("loginmethod", passwordmethod, bundle.get("config-loginmethod-description"));
        auth.add("validconnect", validconnect, bundle.get("config-validconnect-description"));

        auth.add("discord", discord, bundle.get("config-discord-description"));
        discord.add("token", discordtoken);
        discord.add("link", discordlink);
    }
}
