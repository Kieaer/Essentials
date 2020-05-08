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
import static org.hjson.JsonValue.readJSON;

public class Config {
    public JsonObject obj;
    private int version;
    private Locale language;
    private boolean serverEnable;
    private int serverPort;
    private boolean clientEnable;
    private int clientPort;
    private String clientHost;
    private boolean realName;
    private boolean strictName;
    private int cupdatei;
    private boolean scanResource;
    private boolean antiGrief;
    private boolean alertAction;
    private boolean expLimit;
    private double baseXp;
    private double exponent;
    private boolean levelUpAlarm;
    private int alarmLevel;
    private boolean banShare;
    private JsonArray banTrust;
    private boolean query;
    private boolean antiVPN;
    private boolean antiRush;
    private LocalTime antiRushtime;
    private boolean vote;
    private boolean logging;
    private boolean update;
    private boolean internalDB;
    private boolean dbServer;
    private String dbUrl;
    private boolean oldDBMigration;
    private String oldDBurl;
    private String oldDBid;
    private String oldDBpw;
    private String dataserverUrl;
    private String dataserverId;
    private String dataserverPw;
    private boolean loginEnable;
    private String passwordMethod;
    private boolean validConnect;
    private boolean autoLogin;
    private String discordToken;
    private String discordLink;
    private boolean translate;
    private String translateId;
    private String translatePw;
    private boolean debug;
    private String debugCode;
    private boolean crashReport;
    private LocalTime saveTime;
    private boolean rollback;
    private int slotNumber;
    private boolean autoDifficulty;
    private int difficultyEasy;
    private int difficultyNormal;
    private int difficultyHard;
    private int difficultyInsane;
    private boolean border;
    private int spawnLimit;
    private String prefix;
    private String eventPort;

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
        version = settings.getInt("version", vars.configVersion());

        String lc = settings.getString("language", System.getProperty("user.language") + "_" + System.getProperty("user.country")).split(",")[0];
        if (lc.split("-").length == 2) {
            String[] array = lc.split("-");
            language = new Locale(array[0], array[1]);
        } else {
            language = new Locale(System.getProperty("user.language") + "_" + System.getProperty("user.country"));
        }
        locale = config.language();

        logging = settings.getBoolean("logging", true);
        update = settings.getBoolean("update", true);
        debug = settings.getBoolean("debug", false);
        debugCode = settings.getString("debugcode", "none");
        crashReport = settings.getBoolean("crashreport", true);
        prefix = settings.getString("prefix", "[green][Essentials] []");

        database = settings.get("database").asObject();
        internalDB = database.getBoolean("internalDB", true);
        dbServer = database.getBoolean("DBServer", false);
        dbUrl = database.getString("DBurl", "jdbc:h2:file:./config/mods/Essentials/data/player");
        oldDBMigration = database.getBoolean("old-db-migration", false);
        oldDBurl = database.getString("old-db-url", "jdbc:sqlite:config/mods/Essentials/data/player.sqlite3");
        oldDBid = database.getString("old-db-id", "none");
        oldDBpw = database.getString("old-db-pw", "none");
        dataserverUrl = database.getString("data-server-url", "none");
        dataserverId = database.getString("data-server-id", "none");
        dataserverPw = database.getString("data-server-pw", "none");

        network = obj.get("network").asObject();
        serverEnable = network.getBoolean("server-enable", false);
        serverPort = network.getInt("server-port", 25000);
        clientEnable = network.getBoolean("client-enable", false);
        clientPort = network.getInt("client-port", 25000);
        clientHost = network.getString("client-host", "mindustry.kr");
        banShare = network.getBoolean("banshare", false);
        banTrust = network.get("bantrust") == null ? readJSON("[\"127.0.0.1\",\"localhost\"]").asArray() : network.get("bantrust").asArray();
        query = network.getBoolean("query", false);

        anti = obj.get("antigrief").asObject();
        antiGrief = anti.getBoolean("antigrief", false);
        antiVPN = anti.getBoolean("antivpn", false);
        antiRush = anti.getBoolean("antirush", false);
        antiRushtime = LocalTime.parse(anti.getString("antirushtime", "00:10:00"), DateTimeFormatter.ofPattern("HH:mm:ss"));
        alertAction = anti.getBoolean("alert-action", false);
        realName = anti.getBoolean("realname", false);
        strictName = anti.getBoolean("strict-name", false);
        scanResource = anti.getBoolean("scanresource", false);

        features = obj.get("features").asObject();
        expLimit = features.getBoolean("explimit", false);
        baseXp = features.getDouble("basexp", 500.0);
        exponent = features.getDouble("exponent", 1.12);
        levelUpAlarm = features.getBoolean("levelupalarm", false);
        alarmLevel = features.getInt("alarm-minimal-level", 20);
        vote = features.getBoolean("vote", true);
        saveTime = LocalTime.parse(features.getString("savetime", "00:10:00"), DateTimeFormatter.ofPattern("HH:mm:ss"));
        rollback = features.getBoolean("rollback", false);
        slotNumber = features.getInt("slotnumber", 1000);
        border = features.getBoolean("border", false);
        spawnLimit = features.getInt("spawnlimit", 500);
        eventPort = features.getString("eventport", "8000-8050");
        cupdatei = features.getInt("cupdatei", 1000);

        difficulty = features.get("difficulty").asObject();
        autoDifficulty = difficulty.getBoolean("auto-difficulty", false);
        difficultyEasy = difficulty.getInt("easy", 2);
        difficultyNormal = difficulty.getInt("normal", 4);
        difficultyHard = difficulty.getInt("hard", 6);
        difficultyInsane = difficulty.getInt("insane", 10);

        tr = features.get("translate").asObject();
        translate = tr.getBoolean("translate", false);
        translateId = tr.getString("translateid", "none");
        translatePw = tr.getString("translatepw", "none");

        auth = obj.get("auth").asObject();
        loginEnable = auth.getBoolean("loginenable", false);
        passwordMethod = auth.getString("loginmethod", "password");
        validConnect = auth.getBoolean("validconnect", false);
        autoLogin = auth.getBoolean("autologin", true);

        discord = auth.get("discord").asObject();
        discordToken = discord.getString("token", "none");
        discordLink = discord.getString("link", "none");

        updateConfig();
    }

    public void updateConfig() {
        locale = tool.TextToLocale(obj.getString("language", locale.toString()));
        Bundle bundle = new Bundle(locale);

        if (obj.getInt("version", 0) < vars.configVersion()) Log.info("config.updated");

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
        settings.add("language", language.toLanguageTag(), bundle.get("config.language.description"));
        settings.add("logging", logging, bundle.get("config.feature.logging"));
        settings.add("update", update, bundle.get("config.update"));
        settings.add("debug", debug, bundle.get("config.debug"));
        settings.add("debugcode", debugCode);
        settings.add("crash-report", crashReport);
        settings.add("prefix", prefix, bundle.get("config.prefix"));

        // DB 설정 (settings 상속)
        settings.add("database", db);
        db.add("internalDB", internalDB, bundle.get("config.database"));
        db.add("DBServer", dbServer);
        db.add("DBurl", dbUrl);
        db.add("old-db-migration", oldDBMigration, bundle.get("config.database.old-database-migration"));
        db.add("old-db-url", oldDBurl);
        db.add("old-db-id", oldDBid);
        db.add("old-db-pw", oldDBpw);
        db.add("data-server-url", dataserverUrl, bundle.get("config.client.data-share"));
        db.add("data-server-id", dataserverId);
        db.add("data-server-pw", dataserverPw);

        // 네트워크 설정
        network.add("server-enable", serverEnable, bundle.get("config.network"));
        network.add("server-port", serverPort);
        network.add("client-enable", clientEnable);
        network.add("client-port", clientPort);
        network.add("client-host", clientHost);
        network.add("banshare", banShare, bundle.get("config.server.banshare"));
        network.add("bantrust", banTrust, bundle.get("config.server.bantrust"));
        network.add("query", query, bundle.get("config.server.query"));

        // 테러방지 설정
        anti.add("antigrief", antiGrief, bundle.get("config.anti-grief.desc"));
        anti.add("antivpn", antiVPN, bundle.get("config.anti-grief.vpn"));
        anti.add("antirush", antiRush, bundle.get("config.anti-grief.pvprush"));
        anti.add("antirushtime", antiRushtime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        anti.add("alert-action", alertAction, bundle.get("config-alert-action-description"));
        anti.add("realname", realName, bundle.get("config.anti-grief.realname"));
        anti.add("strict-name", strictName, bundle.get("config-strict-name-description"));
        anti.add("scanresource", scanResource, bundle.get("config.anti-grief.scan-resource"));

        // 특별한 기능 설정
        features.add("explimit", expLimit, bundle.get("config.feature.exp.limit"));
        features.add("basexp", baseXp, bundle.get("config.feature.exp.basexp"));
        features.add("exponent", exponent, bundle.get("config.feature.exp.exponent"));
        features.add("levelupalarm", levelUpAlarm, bundle.get("config.feature.exp.levelup-alarm"));
        features.add("alarm-minimal-level", alarmLevel, bundle.get("config.feature.exp.minimal-level"));
        features.add("vote", vote, bundle.get("config.feature.vote"));
        features.add("savetime", saveTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")), bundle.get("config.feature.save-time"));
        features.add("rollback", rollback, bundle.get("config.feature.slot-number"));
        features.add("slotnumber", slotNumber);
        features.add("border", border, bundle.get("config.feature.border"));
        features.add("spawnlimit", spawnLimit, bundle.get("config.feature.spawn-limit"));
        features.add("eventport", eventPort, bundle.get("config.feature.event.port"));
        features.add("cupdatei", cupdatei, bundle.get("config.feature.colornick"));

        // 난이도 설정 (features 상속)
        features.add("difficulty", difficulty, bundle.get("config.auto-difficulty"));
        difficulty.add("auto-difficulty", autoDifficulty);
        difficulty.add("easy", difficultyEasy);
        difficulty.add("normal", difficultyNormal);
        difficulty.add("hard", difficultyHard);
        difficulty.add("insane", difficultyInsane);

        // 번역 설정 (features 상속)
        features.add("translate", tr, bundle.get("config.feature.papago"));
        tr.add("translate", translate);
        tr.add("translateid", translateId);
        tr.add("translatepw", translatePw);

        // 로그인 설정
        auth.add("loginenable", loginEnable, bundle.get("config.account.login"));
        auth.add("loginmethod", passwordMethod, bundle.get("config.account.login.method"));
        auth.add("validconnect", validConnect, bundle.get("config.account.valid-connect"));
        auth.add("autologin", autoLogin);

        // Discord 설정 (auth 상속)
        auth.add("discord", discord, bundle.get("config.feature.discord.desc"));
        discord.add("token", discordToken);
        discord.add("link", discordLink);

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

        settings.add("version", obj.getInt("version", vars.configVersion()), bundle.get("config.version"));
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

    public void version(int version) {
        this.version = version;
    }

    public Locale language() {
        return language;
    }

    public void language(Locale language) {
        this.language = language;
    }

    public boolean serverEnable() {
        return serverEnable;
    }

    public void serverEnable(boolean serverEnable) {
        this.serverEnable = serverEnable;
    }

    public int serverPort() {
        return serverPort;
    }

    public void serverPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public boolean clientEnable() {
        return clientEnable;
    }

    public void clientEnable(boolean clientEnable) {
        this.clientEnable = clientEnable;
    }

    public int clientPort() {
        return clientPort;
    }

    public void clientPort(int clientPort) {
        this.clientPort = clientPort;
    }

    public String clientHost() {
        return clientHost;
    }

    public void clientHost(String clientHost) {
        this.clientHost = clientHost;
    }

    public boolean realName() {
        return realName;
    }

    public void realName(boolean realName) {
        this.realName = realName;
    }

    public boolean strictName() {
        return strictName;
    }

    public void strictName(boolean strictName) {
        this.strictName = strictName;
    }

    public int cupdatei() {
        return cupdatei;
    }

    public void cupdatei(int cupdatei) {
        this.cupdatei = cupdatei;
    }

    public boolean scanResource() {
        return scanResource;
    }

    public void scanResource(boolean scanResource) {
        this.scanResource = scanResource;
    }

    public boolean antiGrief() {
        return antiGrief;
    }

    public void antiGrief(boolean antiGrief) {
        this.antiGrief = antiGrief;
    }

    public boolean alertAction() {
        return alertAction;
    }

    public void alertAction(boolean alertAction) {
        this.alertAction = alertAction;
    }

    public boolean expLimit() {
        return expLimit;
    }

    public void expLimit(boolean expLimit) {
        this.expLimit = expLimit;
    }

    public double baseXp() {
        return baseXp;
    }

    public void baseXp(double baseXp) {
        this.baseXp = baseXp;
    }

    public double exponent() {
        return exponent;
    }

    public void exponent(double exponent) {
        this.exponent = exponent;
    }

    public boolean levelUpAlarm() {
        return levelUpAlarm;
    }

    public void levelUpAlarm(boolean levelUpAlarm) {
        this.levelUpAlarm = levelUpAlarm;
    }

    public int alarmLevel() {
        return alarmLevel;
    }

    public void alarmLevel(int alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    public boolean banShare() {
        return banShare;
    }

    public void banShare(boolean banShare) {
        this.banShare = banShare;
    }

    public JsonArray banTrust() {
        return banTrust;
    }

    public void banTrust(JsonArray banTrust) {
        this.banTrust = banTrust;
    }

    public boolean query() {
        return query;
    }

    public void query(boolean query) {
        this.query = query;
    }

    public boolean antiVPN() {
        return antiVPN;
    }

    public void antiVPN(boolean antiVpn) {
        this.antiVPN = antiVpn;
    }

    public boolean antiRush() {
        return antiRush;
    }

    public void antiRush(boolean antiRush) {
        this.antiRush = antiRush;
    }

    public LocalTime antiRushtime() {
        return antiRushtime;
    }

    public void antiRushtime(LocalTime antiRushtime) {
        this.antiRushtime = antiRushtime;
    }

    public boolean vote() {
        return vote;
    }

    public void vote(boolean vote) {
        this.vote = vote;
    }

    public boolean logging() {
        return logging;
    }

    public void logging(boolean logging) {
        this.logging = logging;
    }

    public boolean update() {
        return update;
    }

    public void update(boolean update) {
        this.update = update;
    }

    public boolean internalDB() {
        return internalDB;
    }

    public void internalDB(boolean internalDB) {
        this.internalDB = internalDB;
    }

    public boolean dbServer() {
        return dbServer;
    }

    public void dbServer(boolean dbServer) {
        this.dbServer = dbServer;
    }

    public String dbUrl() {
        return dbUrl;
    }

    public void dbUrl(String dbUrl) {
        this.dbUrl = dbUrl;
    }

    public boolean oldDBMigration() {
        return oldDBMigration;
    }

    public void oldDBMigration(boolean oldDBMigration) {
        this.oldDBMigration = oldDBMigration;
    }

    public String oldDBurl() {
        return oldDBurl;
    }

    public void oldDBurl(String oldDBurl) {
        this.oldDBurl = oldDBurl;
    }

    public String oldDBid() {
        return oldDBid;
    }

    public void oldDBid(String oldDBid) {
        this.oldDBid = oldDBid;
    }

    public String oldDBpw() {
        return oldDBpw;
    }

    public void oldDBpw(String oldDBpw) {
        this.oldDBpw = oldDBpw;
    }

    public String dataserverUrl() {
        return dataserverUrl;
    }

    public void dataserverUrl(String dataserverUrl) {
        this.dataserverUrl = dataserverUrl;
    }

    public String dataserverId() {
        return dataserverId;
    }

    public void dataserverId(String dataserverId) {
        this.dataserverId = dataserverId;
    }

    public String dataserverPw() {
        return dataserverPw;
    }

    public void dataserverPw(String dataserverPw) {
        this.dataserverPw = dataserverPw;
    }

    public boolean loginEnable() {
        return loginEnable;
    }

    public void loginEnable(boolean loginEnable) {
        this.loginEnable = loginEnable;
    }

    public String passwordMethod() {
        return passwordMethod;
    }

    public void passwordMethod(String passwordMethod) {
        this.passwordMethod = passwordMethod;
    }

    public boolean validConnect() {
        return validConnect;
    }

    public void validConnect(boolean validConnect) {
        this.validConnect = validConnect;
    }

    public boolean autoLogin() {
        return autoLogin;
    }

    public void autoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    public String discordToken() {
        return discordToken;
    }

    public void discordToken(String discordToken) {
        this.discordToken = discordToken;
    }

    public String discordLink() {
        return discordLink;
    }

    public void discordLink(String discordLink) {
        this.discordLink = discordLink;
    }

    public boolean translate() {
        return translate;
    }

    public void translate(boolean translate) {
        this.translate = translate;
    }

    public String translateId() {
        return translateId;
    }

    public void translateId(String translateId) {
        this.translateId = translateId;
    }

    public String translatePw() {
        return translatePw;
    }

    public void translatePw(String translatePw) {
        this.translatePw = translatePw;
    }

    public boolean debug() {
        return debug;
    }

    public void debug(boolean debug) {
        this.debug = debug;
    }

    public String debugCode() {
        return debugCode;
    }

    public void debugCode(String debugCode) {
        this.debugCode = debugCode;
    }

    public boolean crashReport() {
        return crashReport;
    }

    public void crashReport(boolean crashReport) {
        this.crashReport = crashReport;
    }

    public LocalTime saveTime() {
        return saveTime;
    }

    public void saveTime(LocalTime saveTime) {
        this.saveTime = saveTime;
    }

    public boolean rollback() {
        return rollback;
    }

    public void rollback(boolean rollback) {
        this.rollback = rollback;
    }

    public int slotNumber() {
        return slotNumber;
    }

    public void slotNumber(int slotNumber) {
        this.slotNumber = slotNumber;
    }

    public boolean autoDifficulty() {
        return autoDifficulty;
    }

    public void autoDifficulty(boolean autoDifficulty) {
        this.autoDifficulty = autoDifficulty;
    }

    public int difficultyEasy() {
        return difficultyEasy;
    }

    public void difficultyEasy(int difficultyEasy) {
        this.difficultyEasy = difficultyEasy;
    }

    public int difficultyNormal() {
        return difficultyNormal;
    }

    public void difficultyNormal(int difficultyNormal) {
        this.difficultyNormal = difficultyNormal;
    }

    public int difficultyHard() {
        return difficultyHard;
    }

    public void difficultyHard(int difficultyHard) {
        this.difficultyHard = difficultyHard;
    }

    public int difficultyInsane() {
        return difficultyInsane;
    }

    public void difficultyInsane(int difficultyInsane) {
        this.difficultyInsane = difficultyInsane;
    }

    public boolean border() {
        return border;
    }

    public void border(boolean border) {
        this.border = border;
    }

    public int spawnLimit() {
        return spawnLimit;
    }

    public void spawnLimit(int spawnLimit) {
        this.spawnLimit = spawnLimit;
    }

    public String prefix() {
        return prefix;
    }

    public void prefix(String prefix) {
        this.prefix = prefix;
    }

    public String eventPort() {
        return eventPort;
    }

    public void eventPort(String eventPort) {
        this.eventPort = eventPort;
    }
}
