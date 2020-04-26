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
import static org.hjson.JsonValue.readJSON;

public class Config {
    JsonObject obj;
    private int version;
    private Locale language;
    private boolean serverenable;
    private int serverport;
    private boolean clientenable;
    private int clientport;
    private String clienthost;
    private boolean realname;
    private boolean strictname;
    private int cupdatei;
    private boolean scanresource;
    private boolean antigrief;
    private boolean alertaction;
    private boolean explimit;
    private double basexp;
    private double exponent;
    private boolean levelupalarm;
    private int alarmlevel;
    private boolean banshare;
    private JsonArray bantrust;
    private boolean query;
    private boolean antivpn;
    private boolean antirush;
    private LocalTime antirushtime;
    private boolean vote;
    private boolean logging;
    private boolean update;
    private boolean internalDB;
    private boolean dbserver;
    private String dburl;
    private boolean olddbmigration;
    private String olddburl;
    private String olddbid;
    private String olddbpw;
    private String dataserverurl;
    private String dataserverid;
    private String dataserverpw;
    private boolean loginenable;
    private String passwordmethod;
    private boolean validconnect;
    private boolean autologin;
    private String discordtoken;
    private String discordlink;
    private boolean translate;
    private String translateid;
    private String translatepw;
    private boolean debug;
    private String debugcode;
    private boolean crashreport;
    private LocalTime savetime;
    private boolean rollback;
    private int slotnumber;
    private boolean autodifficulty;
    private int difficultyEasy;
    private int difficultyNormal;
    private int difficultyHard;
    private int difficultyInsane;
    private boolean border;
    private int spawnlimit;
    private String prefix;
    private String eventport;

    public void init() {
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
        dbserver = database.getBoolean("DBServer", false);
        dburl = database.getString("DBurl", "jdbc:h2:file:./config/mods/Essentials/data/player");
        olddbmigration = database.getBoolean("old-db-migration", false);
        olddburl = database.getString("old-db-url", "jdbc:sqlite:config/mods/Essentials/data/player.sqlite3");
        olddbid = database.getString("old-db-id", "none");
        olddbpw = database.getString("old-db-pw", "none");
        dataserverurl = database.getString("data-server-url", "none");
        dataserverid = database.getString("data-server-id", "none");
        dataserverpw = database.getString("data-server-pw", "none");

        network = obj.get("network").asObject();
        serverenable = network.getBoolean("server-enable", false);
        serverport = network.getInt("server-port", 25000);
        clientenable = network.getBoolean("client-enable", false);
        clientport = network.getInt("client-port", 25000);
        clienthost = network.getString("client-host", "mindustry.kr");
        banshare = network.getBoolean("banshare", false);
        bantrust = network.get("bantrust") == null ? readJSON("[\"127.0.0.1\",\"localhost\"]").asArray() : network.get("bantrust").asArray();
        query = network.getBoolean("query", false);

        anti = obj.get("antigrief").asObject();
        antigrief = anti.getBoolean("antigrief", false);
        antivpn = anti.getBoolean("antivpn", false);
        antirush = anti.getBoolean("antirush", false);
        antirushtime = LocalTime.parse(anti.getString("antirushtime", "00:10:00"), DateTimeFormatter.ofPattern("HH:mm:ss"));
        alertaction = anti.getBoolean("alert-action", false);
        realname = anti.getBoolean("realname", false);
        strictname = anti.getBoolean("strict-name", false);
        scanresource = anti.getBoolean("scanresource", false);

        features = obj.get("features").asObject();
        explimit = features.getBoolean("explimit", false);
        basexp = features.getDouble("basexp", 500.0);
        exponent = features.getDouble("exponent", 1.12);
        levelupalarm = features.getBoolean("levelupalarm", false);
        alarmlevel = features.getInt("alarm-minimal-level", 20);
        vote = features.getBoolean("vote", true);
        savetime = LocalTime.parse(features.getString("savetime", "00:10:00"), DateTimeFormatter.ofPattern("HH:mm:ss"));
        rollback = features.getBoolean("rollback", false);
        slotnumber = features.getInt("slotnumber", 1000);
        border = features.getBoolean("border", false);
        spawnlimit = features.getInt("spawnlimit", 500);
        eventport = features.getString("eventport", "8000-8050");
        cupdatei = features.getInt("cupdatei", 1000);

        difficulty = features.get("difficulty").asObject();
        autodifficulty = difficulty.getBoolean("auto-difficulty", false);
        difficultyEasy = difficulty.getInt("easy", 2);
        difficultyNormal = difficulty.getInt("normal", 4);
        difficultyHard = difficulty.getInt("hard", 6);
        difficultyInsane = difficulty.getInt("insane", 10);

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
        this.olddbmigration = oldDBMigration;
    }

    public void update() {
        locale = tool.TextToLocale(obj.getString("language", locale.toString()));
        Bundle bundle = new Bundle(locale);

        if (obj.getInt("version", 0) < config_version) Log.info("config.updated");

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
        settings.add("version", version, bundle.get("config.version"));
        settings.add("language", language.toString(), bundle.get("config.language.description"));
        settings.add("logging", logging, bundle.get("config.feature.logging"));
        settings.add("update", update, bundle.get("config.update"));
        settings.add("debug", debug, bundle.get("config.debug"));
        settings.add("debugcode", debugcode);
        settings.add("crash-report", crashreport);
        settings.add("prefix", prefix, bundle.get("config.prefix"));

        // DB 설정 (settings 상속)
        settings.add("database", db);
        db.add("internalDB", internalDB, bundle.get("config.database"));
        db.add("DBServer", dbserver);
        db.add("DBurl", dburl);
        db.add("old-db-migration", olddbmigration, bundle.get("config.database.old-database-migration"));
        db.add("old-db-url", olddburl);
        db.add("old-db-id", olddbid);
        db.add("old-db-pw", olddbpw);
        db.add("data-server-url", dataserverurl, bundle.get("config.client.data-share"));
        db.add("data-server-id", dataserverid);
        db.add("data-server-pw", dataserverpw);

        // 네트워크 설정
        network.add("server-enable", serverenable, bundle.get("config.network"));
        network.add("server-port", serverport);
        network.add("client-enable", clientenable);
        network.add("client-port", clientport);
        network.add("client-host", clienthost);
        network.add("banshare", banshare, bundle.get("config.server.banshare"));
        network.add("bantrust", bantrust, bundle.get("config.server.bantrust"));
        network.add("query", query, bundle.get("config.server.query"));

        // 테러방지 설정
        anti.add("antigrief", antigrief, bundle.get("config.anti-grief.desc"));
        anti.add("antivpn", antivpn, bundle.get("config.anti-grief.vpn"));
        anti.add("antirush", antirush, bundle.get("config.anti-grief.pvprush"));
        anti.add("antirushtime", antirushtime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        anti.add("alert-action", alertaction, bundle.get("config-alert-action-description"));
        anti.add("realname", realname, bundle.get("config.anti-grief.realname"));
        anti.add("strict-name", strictname, bundle.get("config-strict-name-description"));
        anti.add("scanresource", scanresource, bundle.get("config.anti-grief.scan-resource"));

        // 특별한 기능 설정
        features.add("explimit", explimit, bundle.get("config.feature.exp.limit"));
        features.add("basexp", basexp, bundle.get("config.feature.exp.basexp"));
        features.add("exponent", exponent, bundle.get("config.feature.exp.exponent"));
        features.add("levelupalarm", levelupalarm, bundle.get("config.feature.exp.levelup-alarm"));
        features.add("alarm-minimal-level", alarmlevel, bundle.get("config.feature.exp.minimal-level"));
        features.add("vote", vote, bundle.get("config.feature.vote"));
        features.add("savetime", savetime.format(DateTimeFormatter.ofPattern("HH:mm:ss")), bundle.get("config.feature.save-time"));
        features.add("rollback", rollback, bundle.get("config.feature.slot-number"));
        features.add("slotnumber", slotnumber);
        features.add("border", border, bundle.get("config.feature.border"));
        features.add("spawnlimit", spawnlimit, bundle.get("config.feature.spawn-limit"));
        features.add("eventport", eventport, bundle.get("config.feature.event.port"));
        features.add("cupdatei", cupdatei, bundle.get("config.feature.colornick"));

        // 난이도 설정 (features 상속)
        features.add("difficulty", difficulty, bundle.get("config.auto-difficulty"));
        difficulty.add("auto-difficulty", autodifficulty);
        difficulty.add("easy", difficultyEasy);
        difficulty.add("normal", difficultyNormal);
        difficulty.add("hard", difficultyHard);
        difficulty.add("insane", difficultyInsane);

        // 번역 설정 (features 상속)
        features.add("translate", tr, bundle.get("config.feature.papago"));
        tr.add("translate", translate);
        tr.add("translateid", translateid);
        tr.add("translatepw", translatepw);

        // 로그인 설정
        auth.add("loginenable", loginenable, bundle.get("config.account.login"));
        auth.add("loginmethod", passwordmethod, bundle.get("config.account.login.method"));
        auth.add("validconnect", validconnect, bundle.get("config.account.valid-connect"));
        auth.add("autologin", autologin);

        // Discord 설정 (auth 상속)
        auth.add("discord", discord, bundle.get("config.feature.discord.desc"));
        discord.add("token", discordtoken);
        discord.add("link", discordlink);

        root.child("config.hjson").writeString(config.toString(Stringify.HJSON_COMMENTS));
    }

    public void LegacyUpgrade() {
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

        settings.add("version", obj.getInt("version", config_version), bundle.get("config.version"));
        settings.add("language", new Locale(obj.getString("language", System.getProperty("user.language") + "_" + System.getProperty("user.country"))).toString(), bundle.get("config.language.description"));
        settings.add("logging", obj.getBoolean("logging", true), bundle.get("config.feature.logging"));
        settings.add("update", obj.getBoolean("update", true), bundle.get("config.update"));
        settings.add("debug", obj.getBoolean("debug", false), bundle.get("config.debug"));
        settings.add("debugcode", obj.getString("debugcode", "none"));
        settings.add("crash-report", obj.getBoolean("crashreport", true));
        settings.add("prefix", obj.getString("prefix", "[green][Essentials] []"), bundle.get("config.prefix"));

        settings.add("database", db);
        db.add("internalDB", obj.getBoolean("internalDB", true), bundle.get("config.database"));
        db.add("DBServer", obj.getBoolean("DBServer", false));
        db.add("DBurl", obj.getString("DBurl", "jdbc:h2:file:./config/mods/Essentials/data/player"));
        db.add("old-db-migration", obj.getBoolean("OldDBMigration", false), bundle.get("config.database.old-database-migration"));
        db.add("old-db-url", obj.getString("OldDBurl", "jdbc:sqlite:config/mods/Essentials/data/player.sqlite3"));
        db.add("old-db-id", obj.getString("OldDBID", "none"));
        db.add("old-db-pw", obj.getString("OldDBPW", "none"));
        db.add("data-server-url", obj.getString("dataserverurl", "none"), bundle.get("config.client.data-share"));
        db.add("data-server-id", obj.getString("dataserverid", "none"));
        db.add("data-server-pw", obj.getString("dataserverpw", "none"));

        network.add("server-enable", obj.getBoolean("serverenable", false), bundle.get("config.network"));
        network.add("server-port", obj.getInt("serverport", 25000));
        network.add("client-enable", obj.getBoolean("clientenable", false));
        network.add("client-port", obj.getInt("clientport", 25000));
        network.add("client-host", obj.getString("clienthost", "mindustry.kr"));
        network.add("banshare", obj.getBoolean("banshare", false), bundle.get("config.server.banshare"));
        network.add("bantrust", obj.get("bantrust") == null ? readJSON("[\"127.0.0.1\",\"localhost\"]").asArray() : obj.get("bantrust").asArray(), bundle.get("config.server.bantrust"));
        network.add("query", obj.getBoolean("query", false), bundle.get("config.server.query"));

        anti.add("antigrief", obj.getBoolean("antigrief", false), bundle.get("config.anti-grief.desc"));
        anti.add("antivpn", obj.getBoolean("antivpn", false), bundle.get("config.anti-grief.vpn"));
        anti.add("antirush", obj.getBoolean("antirush", false), bundle.get("config.anti-grief.pvprush"));
        anti.add("antirushtime", LocalTime.parse(obj.getString("antirushtime", "00:10:00"), DateTimeFormatter.ofPattern("HH:mm:ss")).format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        anti.add("alert-action", obj.getBoolean("alertaction", false), bundle.get("config-alert-action-description"));
        anti.add("realname", obj.getBoolean("realname", false), bundle.get("config.anti-grief.realname"));
        anti.add("strict-name", obj.getBoolean("strictname", false), bundle.get("config-strict-name-description"));
        anti.add("scanresource", obj.getBoolean("scanresource", false), bundle.get("config.anti-grief.scan-resource"));

        features.add("explimit", obj.getBoolean("explimit", false), bundle.get("config.feature.exp.limit"));
        features.add("basexp", obj.getDouble("basexp", 500.0), bundle.get("config.feature.exp.basexp"));
        features.add("exponent", obj.getDouble("exponent", 1.12), bundle.get("config.feature.exp.exponent"));
        features.add("levelupalarm", obj.getBoolean("levelupalarm", false), bundle.get("config.feature.exp.levelup-alarm"));
        features.add("alarm-minimal-level", obj.getInt("alarmlevel", 20), bundle.get("config.feature.exp.minimal-level"));
        features.add("vote", true, bundle.get("config.feature.vote"));
        features.add("savetime", LocalTime.parse(obj.getString("savetime", "00:10:00"), DateTimeFormatter.ofPattern("HH:mm:ss")).format(DateTimeFormatter.ofPattern("HH:mm:ss")), bundle.get("config.feature.save-time"));
        features.add("rollback", obj.getBoolean("rollback", false), bundle.get("config.feature.slot-number"));
        features.add("slotnumber", obj.getInt("slotnumber", 1000));
        features.add("border", obj.getBoolean("border", false), bundle.get("config.feature.border"));
        features.add("spawnlimit", obj.getInt("spawnlimit", 500), bundle.get("config.feature.spawn-limit"));
        features.add("eventport", obj.getString("eventport", "8000-8050"), bundle.get("config.feature.event.port"));
        features.add("cupdatei", obj.getInt("cupdatei", 1000), bundle.get("config.feature.colornick"));

        features.add("difficulty", difficulty, bundle.get("config.auto-difficulty"));
        difficulty.add("auto-difficulty", obj.getBoolean("autodifficulty", false));
        difficulty.add("easy", obj.getInt("difficultyEasy", 2));
        difficulty.add("normal", obj.getInt("difficultyNormal", 4));
        difficulty.add("hard", obj.getInt("difficultyHard", 6));
        difficulty.add("insane", obj.getInt("difficultyInsane", 10));

        features.add("translate", tr, bundle.get("config.feature.papago"));
        tr.add("translate", obj.getBoolean("translate", false));
        tr.add("translateid", obj.getString("translateid", "none"));
        tr.add("translatepw", obj.getString("translatepw", "none"));

        auth.add("loginenable", obj.getBoolean("loginenable", false), bundle.get("config.account.login"));
        auth.add("loginmethod", obj.getString("passwordmethod", "password"), bundle.get("config.account.login.method"));
        auth.add("validconnect", obj.getBoolean("validconnect", false), bundle.get("config.account.valid-connect"));

        auth.add("discord", discord, bundle.get("config.feature.discord.desc"));
        discord.add("token", obj.getString("discordtoken", "none"));
        discord.add("link", obj.getString("discordlink", "none"));
    }

    public int version() {
        return version;
    }

    public Locale language() {
        return language;
    }

    public boolean serverenable() {
        return serverenable;
    }

    public int serverport() {
        return serverport;
    }

    public boolean clienten() {
        return clientenable;
    }

    public int clientport() {
        return clientport;
    }

    public String clienthost() {
        return clienthost;
    }

    public boolean realname() {
        return realname;
    }

    public boolean strictname() {
        return strictname;
    }

    public int cupdatei() {
        return cupdatei;
    }

    public boolean scanresource() {
        return scanresource;
    }

    public boolean antigrief() {
        return antigrief;
    }

    public boolean alertaction() {
        return alertaction;
    }

    public boolean explimit() {
        return explimit;
    }

    public double basexp() {
        return basexp;
    }

    public double exponent() {
        return exponent;
    }

    public boolean levelupalarm() {
        return levelupalarm;
    }

    public int alarmlevel() {
        return alarmlevel;
    }

    public boolean banshare() {
        return banshare;
    }

    public JsonArray bantrust() {
        return bantrust;
    }

    public boolean query() {
        return query;
    }

    public boolean antivpn() {
        return antivpn;
    }

    public boolean antirush() {
        return antirush;
    }

    public LocalTime antirushtime() {
        return antirushtime;
    }

    public boolean vote() {
        return vote;
    }

    public boolean logging() {
        return logging;
    }

    public boolean isUpdate() {
        return update;
    }

    public boolean internaldb() {
        return internalDB;
    }

    public boolean dbserver() {
        return dbserver;
    }

    public String dburl() {
        return dburl;
    }

    public boolean olddbmigration() {
        return olddbmigration;
    }

    public String olddburl() {
        return olddburl;
    }

    public String olddbid() {
        return olddbid;
    }

    public String olddbpw() {
        return olddbpw;
    }

    public String dataserverurl() {
        return dataserverurl;
    }

    public String dataserverid() {
        return dataserverid;
    }

    public String dataserverpw() {
        return dataserverpw;
    }

    public boolean loginenable() {
        return loginenable;
    }

    public String passwordmethod() {
        return passwordmethod;
    }

    public boolean validconnect() {
        return validconnect;
    }

    public boolean autologin() {
        return autologin;
    }

    public String discordtoken() {
        return discordtoken;
    }

    public String discordlink() {
        return discordlink;
    }

    public boolean translate() {
        return translate;
    }

    public String translateid() {
        return translateid;
    }

    public String translatepw() {
        return translatepw;
    }

    public boolean debug() {
        return debug;
    }

    public String debugcode() {
        return debugcode;
    }

    public boolean crashreport() {
        return crashreport;
    }

    public LocalTime savetime() {
        return savetime;
    }

    public boolean rollback() {
        return rollback;
    }

    public int slownumber() {
        return slotnumber;
    }

    public boolean autodifficulty() {
        return autodifficulty;
    }

    public int difficultyeasy() {
        return difficultyEasy;
    }

    public int difficultynormal() {
        return difficultyNormal;
    }

    public int difficultyhard() {
        return difficultyHard;
    }

    public int difficultyinsane() {
        return difficultyInsane;
    }

    public boolean border() {
        return border;
    }

    public int spawnlimit() {
        return spawnlimit;
    }

    public String prefix() {
        return prefix;
    }

    public String eventport() {
        return eventport;
    }
}
