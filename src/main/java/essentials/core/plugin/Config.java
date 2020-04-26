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
    private boolean DBServer;
    private String DBurl;
    private boolean OldDBMigration;
    private String OldDBurl;
    private String OldDBID;
    private String OldDBPW;
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
        OldDBMigration = database.getBoolean("old-db-migration", false);
        OldDBurl = database.getString("old-db-url", "jdbc:sqlite:config/mods/Essentials/data/player.sqlite3");
        OldDBID = database.getString("old-db-id", "none");
        OldDBPW = database.getString("old-db-pw", "none");
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
        this.OldDBMigration = oldDBMigration;
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
        //settings.add(CommentType.BOL, CommentStyle.BLOCK,"\n\nasdkfjlkfkjdaslkfjdaslkfjdsalkfjdsalkfjadsflkajdsflkasjflkdasjflks");
        settings.add("debug", debug, bundle.get("config.debug"));
        settings.add("debugcode", debugcode);
        settings.add("crash-report", crashreport);
        //settings.setLineLength(1);
        settings.add("prefix", prefix, bundle.get("config.prefix"));
        //settings.setLineLength(1);

        // DB 설정 (settings 상속)
        settings.add("database", db);
        db.add("internalDB", internalDB, bundle.get("config.database"));
        db.add("DBServer", DBServer);
        db.add("DBurl", DBurl);
        //db.setLineLength(1);
        db.add("old-db-migration", OldDBMigration, bundle.get("config.database.old-database-migration"));
        db.add("old-db-url", OldDBurl);
        db.add("old-db-id", OldDBID);
        db.add("old-db-pw", OldDBPW);
        //db.setLineLength(1);
        db.add("data-server-url", dataserverurl, bundle.get("config.client.data-share"));
        db.add("data-server-id", dataserverid);
        db.add("data-server-pw", dataserverpw);

        // 네트워크 설정
        network.add("server-enable", serverenable, bundle.get("config.network"));
        network.add("server-port", serverport);
        network.add("client-enable", clientenable);
        network.add("client-port", clientport);
        network.add("client-host", clienthost);
        //network.setLineLength(1);
        network.add("banshare", banshare, bundle.get("config.server.banshare"));
        network.add("bantrust", bantrust, bundle.get("config.server.bantrust"));
        //network.setLineLength(1);
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
        //auth.setLineLength(1);
        auth.add("discord", discord, bundle.get("config.feature.discord.desc"));
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
        JsonArray bantrust = obj.get("bantrust") == null ? readJSON("[\"127.0.0.1\",\"localhost\"]").asArray() : obj.get("bantrust").asArray();
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

        settings.add("version", version, bundle.get("config.version"));
        settings.add("language", language.toString(), bundle.get("config.language.description"));
        settings.add("logging", logging, bundle.get("config.feature.logging"));
        settings.add("update", update, bundle.get("config.update"));
        settings.add("debug", debug, bundle.get("config.debug"));
        settings.add("debugcode", debugcode);
        settings.add("crash-report", crashreport);
        settings.add("prefix", prefix, bundle.get("config.prefix"));

        settings.add("database", db);
        db.add("internalDB", internalDB, bundle.get("config.database"));
        db.add("DBServer", DBServer);
        db.add("DBurl", DBurl);
        db.add("old-db-migration", OldDBMigration, bundle.get("config.database.old-database-migration"));
        db.add("old-db-url", OldDBurl);
        db.add("old-db-id", OldDBID);
        db.add("old-db-pw", OldDBPW);
        db.add("data-server-url", dataserverurl, bundle.get("config.client.data-share"));
        db.add("data-server-id", dataserverid);
        db.add("data-server-pw", dataserverpw);

        network.add("server-enable", serverenable, bundle.get("config.network"));
        network.add("server-port", serverport);
        network.add("client-enable", clientenable);
        network.add("client-port", clientport);
        network.add("client-host", clienthost);
        network.add("banshare", banshare, bundle.get("config.server.banshare"));
        network.add("bantrust", bantrust, bundle.get("config.server.bantrust"));
        network.add("query", query, bundle.get("config.server.query"));

        anti.add("antigrief", antigrief, bundle.get("config.anti-grief.desc"));
        anti.add("antivpn", antivpn, bundle.get("config.anti-grief.vpn"));
        anti.add("antirush", antirush, bundle.get("config.anti-grief.pvprush"));
        anti.add("antirushtime", antirushtime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        anti.add("alert-action", alertaction, bundle.get("config-alert-action-description"));
        anti.add("realname", realname, bundle.get("config.anti-grief.realname"));
        anti.add("strict-name", strictname, bundle.get("config-strict-name-description"));
        anti.add("scanresource", scanresource, bundle.get("config.anti-grief.scan-resource"));

        features.add("explimit", explimit, bundle.get("config.feature.exp.limit"));
        features.add("basexp", basexp, bundle.get("config.feature.exp.basexp"));
        features.add("exponent", exponent, bundle.get("config.feature.exp.exponent"));
        features.add("levelupalarm", levelupalarm, bundle.get("config.feature.exp.levelup-alarm"));
        features.add("alarm-minimal-level", alarmlevel, bundle.get("config.feature.exp.minimal-level"));
        features.add("vote", true, bundle.get("config.feature.vote"));
        features.add("savetime", savetime.format(DateTimeFormatter.ofPattern("HH:mm:ss")), bundle.get("config.feature.save-time"));
        features.add("rollback", rollback, bundle.get("config.feature.slot-number"));
        features.add("slotnumber", slotnumber);
        features.add("border", border, bundle.get("config.feature.border"));
        features.add("spawnlimit", spawnlimit, bundle.get("config.feature.spawn-limit"));
        features.add("eventport", eventport, bundle.get("config.feature.event.port"));
        features.add("cupdatei", cupdatei, bundle.get("config.feature.colornick"));

        features.add("difficulty", difficulty, bundle.get("config.auto-difficulty"));
        difficulty.add("auto-difficulty", autodifficulty);
        difficulty.add("easy", difficultyEasy);
        difficulty.add("normal", difficultyNormal);
        difficulty.add("hard", difficultyHard);
        difficulty.add("insane", difficultyInsane);

        features.add("translate", tr, bundle.get("config.feature.papago"));
        tr.add("translate", translate);
        tr.add("translateid", translateid);
        tr.add("translatepw", translatepw);

        auth.add("loginenable", loginenable, bundle.get("config.account.login"));
        auth.add("loginmethod", passwordmethod, bundle.get("config.account.login.method"));
        auth.add("validconnect", validconnect, bundle.get("config.account.valid-connect"));

        auth.add("discord", discord, bundle.get("config.feature.discord.desc"));
        discord.add("token", discordtoken);
        discord.add("link", discordlink);
    }

    public int getVersion() {
        return version;
    }

    public Locale getLanguage() {
        return language;
    }

    public boolean isServerenable() {
        return serverenable;
    }

    public int getServerport() {
        return serverport;
    }

    public boolean isClientenable() {
        return clientenable;
    }

    public int getClientport() {
        return clientport;
    }

    public String getClienthost() {
        return clienthost;
    }

    public boolean isRealname() {
        return realname;
    }

    public boolean isStrictname() {
        return strictname;
    }

    public int getCupdatei() {
        return cupdatei;
    }

    public boolean isScanresource() {
        return scanresource;
    }

    public boolean isAntigrief() {
        return antigrief;
    }

    public boolean isAlertaction() {
        return alertaction;
    }

    public boolean isExplimit() {
        return explimit;
    }

    public double getBasexp() {
        return basexp;
    }

    public double getExponent() {
        return exponent;
    }

    public boolean isLevelupalarm() {
        return levelupalarm;
    }

    public int getAlarmlevel() {
        return alarmlevel;
    }

    public boolean isBanshare() {
        return banshare;
    }

    public JsonArray getBantrust() {
        return bantrust;
    }

    public boolean isQuery() {
        return query;
    }

    public boolean isAntivpn() {
        return antivpn;
    }

    public boolean isAntirush() {
        return antirush;
    }

    public LocalTime getAntirushtime() {
        return antirushtime;
    }

    public boolean isVote() {
        return vote;
    }

    public boolean isLogging() {
        return logging;
    }

    public boolean isUpdate() {
        return update;
    }

    public boolean isInternalDB() {
        return internalDB;
    }

    public boolean isDBServer() {
        return DBServer;
    }

    public String getDBurl() {
        return DBurl;
    }

    public boolean isOldDBMigration() {
        return OldDBMigration;
    }

    public String getOldDBurl() {
        return OldDBurl;
    }

    public String getOldDBID() {
        return OldDBID;
    }

    public String getOldDBPW() {
        return OldDBPW;
    }

    public String getDataserverurl() {
        return dataserverurl;
    }

    public String getDataserverid() {
        return dataserverid;
    }

    public String getDataserverpw() {
        return dataserverpw;
    }

    public boolean isLoginenable() {
        return loginenable;
    }

    public String getPasswordmethod() {
        return passwordmethod;
    }

    public boolean isValidconnect() {
        return validconnect;
    }

    public boolean isAutologin() {
        return autologin;
    }

    public String getDiscordtoken() {
        return discordtoken;
    }

    public String getDiscordlink() {
        return discordlink;
    }

    public boolean isTranslate() {
        return translate;
    }

    public String getTranslateid() {
        return translateid;
    }

    public String getTranslatepw() {
        return translatepw;
    }

    public boolean isDebug() {
        return debug;
    }

    public String getDebugcode() {
        return debugcode;
    }

    public boolean isCrashreport() {
        return crashreport;
    }

    public LocalTime getSavetime() {
        return savetime;
    }

    public boolean isRollback() {
        return rollback;
    }

    public int getSlotnumber() {
        return slotnumber;
    }

    public boolean isAutodifficulty() {
        return autodifficulty;
    }

    public int getDifficultyEasy() {
        return difficultyEasy;
    }

    public int getDifficultyNormal() {
        return difficultyNormal;
    }

    public int getDifficultyHard() {
        return difficultyHard;
    }

    public int getDifficultyInsane() {
        return difficultyInsane;
    }

    public boolean isBorder() {
        return border;
    }

    public int getSpawnlimit() {
        return spawnlimit;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getEventport() {
        return eventport;
    }
}
