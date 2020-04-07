package essentials.core.plugin;

import essentials.internal.Bundle;
import essentials.internal.Log;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;

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
    public boolean detectreactor;
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
    public boolean voteenable;
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
    public String emailserver;
    public int emailport;
    public String emailAccountID;
    public String emailUsername;
    public String emailPassword;
    public String discordtoken;
    public Long discordguild;
    public String discordroom;
    public String discordlink;
    public String discordrole;
    public String discordprefix;
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
        try {
            obj = JsonValue.readHjson(root.child("config.hjson").readString()).asObject();
        } catch (RuntimeException e) {
            obj = new JsonObject();
        }
        version = obj.getInt("version", config_version);
        language = new Locale(obj.getString("language", System.getProperty("user.language") + "_" + System.getProperty("user.country")));
        serverenable = obj.getBoolean("serverenable", false);
        serverport = obj.getInt("serverport", 25000);
        clientenable = obj.getBoolean("clientenable", false);
        clientport = obj.getInt("clientport", 25000);
        clienthost = obj.getString("clienthost", "mindustry.kr");
        realname = obj.getBoolean("realname", false);
        strictname = obj.getBoolean("strictname", false);
        cupdatei = obj.getInt("cupdatei", 1000);
        detectreactor = obj.getBoolean("detectreactor", false);
        scanresource = obj.getBoolean("scanresource", false);
        antigrief = obj.getBoolean("antigrief", false);
        alertaction = obj.getBoolean("alertaction", false);
        explimit = obj.getBoolean("explimit", false);
        basexp = obj.getDouble("basexp", 500.0);
        exponent = obj.getDouble("exponent", 1.12);
        levelupalarm = obj.getBoolean("levelupalarm", false);
        alarmlevel = obj.getInt("alarmlevel", 20);
        banshare = obj.getBoolean("banshare", false);
        bantrust = obj.get("bantrust") == null ? JsonArray.readJSON("[\"127.0.0.1\",\"localhost\"]").asArray() : obj.get("bantrust").asArray();
        query = obj.getBoolean("query", false);
        antivpn = obj.getBoolean("antivpn", false);
        antirush = obj.getBoolean("antirush", false);
        antirushtime = LocalTime.parse(obj.getString("antirushtime", "00:10:00"), DateTimeFormatter.ofPattern("HH:mm:ss"));
        voteenable = obj.getBoolean("voteenable", true);
        logging = obj.getBoolean("logging", true);
        update = obj.getBoolean("update", true);
        internalDB = obj.getBoolean("internalDB", true);
        DBServer = obj.getBoolean("DBServer", false);
        DBurl = obj.getString("DBurl", "jdbc:h2:file:./config/mods/Essentials/data/player");
        OldDBMigration = obj.getBoolean("OldDBMigration", false);
        OldDBurl = obj.getString("OldDBurl", "jdbc:sqlite:config/mods/Essentials/data/player.sqlite3");
        OldDBID = obj.getString("OldDBID", "none");
        OldDBPW = obj.getString("OldDBPW", "none");
        dataserverurl = obj.getString("dataserverurl", "none");
        dataserverid = obj.getString("dataserverid", "none");
        dataserverpw = obj.getString("dataserverpw", "none");
        loginenable = obj.getBoolean("loginenable", false);
        passwordmethod = obj.getString("passwordmethod", "password");
        validconnect = obj.getBoolean("validconnect", false);
        emailserver = obj.getString("emailserver", "smtp.gmail.com");
        emailport = obj.getInt("emailport", 587);
        emailAccountID = obj.getString("emailAccountID", "none");
        emailUsername = obj.getString("emailUsername", "none");
        emailPassword = obj.getString("emailPassword", "none");
        discordtoken = obj.getString("discordtoken", "none");
        discordguild = obj.getLong("discordguild", 0L);
        discordroom = obj.getString("discordroom", "none");
        discordlink = obj.getString("discordlink", "none");
        discordrole = obj.getString("discordrole", "none");
        discordprefix = obj.getString("discordprefix", "none");
        translate = obj.getBoolean("translate", false);
        translateid = obj.getString("translateid", "none");
        translatepw = obj.getString("translatepw", "none");
        debug = obj.getBoolean("debug", false);
        debugcode = obj.getString("debugcode", "none");
        crashreport = obj.getBoolean("crashreport", true);
        savetime = LocalTime.parse(obj.getString("savetime", "00:10:00"), DateTimeFormatter.ofPattern("HH:mm:ss"));
        rollback = obj.getBoolean("rollback", false);
        slotnumber = obj.getInt("slotnumber", 1000);
        autodifficulty = obj.getBoolean("autodifficulty", false);
        difficultyEasy = obj.getInt("difficultyEasy", 2);
        difficultyNormal = obj.getInt("difficultyNormal", 4);
        difficultyHard = obj.getInt("difficultyHard", 6);
        difficultyInsane = obj.getInt("difficultyInsane", 10);
        border = obj.getBoolean("border", false);
        spawnlimit = obj.getInt("spawnlimit", 500);
        prefix = obj.getString("prefix", "[green][Essentials] []");
        eventport = obj.getString("eventport", "8000-8050");

        update();
    }

    public void version(int version) {
        this.version = version;
    }

    public void language(Locale language) {
        this.language = language;
    }

    public void serverenable(boolean serverenable) {
        this.serverenable = serverenable;
    }

    public void serverport(int serverport) {
        this.serverport = serverport;
    }

    public void clientenable(boolean clientenable) {
        this.clientenable = clientenable;
    }

    public void clientport(int clientport) {
        this.clientport = clientport;
    }

    public void clienthost(String clienthost) {
        this.clienthost = clienthost;
    }

    public void realname(boolean realname) {
        this.realname = realname;
    }

    public void strictname(boolean strictname) {
        this.strictname = strictname;
    }

    public void cupdatei(int cupdatei) {
        this.cupdatei = cupdatei;
    }

    public void detectreactor(boolean detectreactor) {
        this.detectreactor = detectreactor;
    }

    public void scanresource(boolean scanresource) {
        this.scanresource = scanresource;
    }

    public void antigrief(boolean antigrief) {
        this.antigrief = antigrief;
    }

    public void alertaction(boolean alertaction) {
        this.alertaction = alertaction;
    }

    public void explimit(boolean explimit) {
        this.explimit = explimit;
    }

    public void basexp(double basexp) {
        this.basexp = basexp;
    }

    public void exponent(double exponent) {
        this.exponent = exponent;
    }

    public void levelupalarm(boolean levelupalarm) {
        this.levelupalarm = levelupalarm;
    }

    public void alarmlevel(int alarmlevel) {
        this.alarmlevel = alarmlevel;
    }

    public void banshare(boolean banshare) {
        this.banshare = banshare;
    }

    public void bantrust(JsonArray bantrust) {
        this.bantrust = bantrust;
    }

    public void query(boolean query) {
        this.query = query;
    }

    public void antivpn(boolean antivpn) {
        this.antivpn = antivpn;
    }

    public void antirush(boolean antirush) {
        this.antirush = antirush;
    }

    public void antirushtime(LocalTime antirushtime) {
        this.antirushtime = antirushtime;
    }

    public void voteenable(boolean voteenable) {
        this.voteenable = voteenable;
    }

    public void logging(boolean logging) {
        this.logging = logging;
    }

    public void update(boolean update) {
        this.update = update;
    }

    public void internalDB(boolean internalDB) {
        this.internalDB = internalDB;
    }

    public void dBServer(boolean DBServer) {
        this.DBServer = DBServer;
    }

    public void dBurl(String DBurl) {
        this.DBurl = DBurl;
    }

    public void oldDBMigration(boolean oldDBMigration) {
        OldDBMigration = oldDBMigration;
    }

    public void oldDBurl(String oldDBurl) {
        OldDBurl = oldDBurl;
    }

    public void oldDBID(String oldDBID) {
        OldDBID = oldDBID;
    }

    public void oldDBPW(String oldDBPW) {
        OldDBPW = oldDBPW;
    }

    public void dataserverurl(String dataserverurl) {
        this.dataserverurl = dataserverurl;
    }

    public void dataserverid(String dataserverid) {
        this.dataserverid = dataserverid;
    }

    public void dataserverpw(String dataserverpw) {
        this.dataserverpw = dataserverpw;
    }

    public void loginenable(boolean loginenable) {
        this.loginenable = loginenable;
    }

    public void passwordmethod(String passwordmethod) {
        this.passwordmethod = passwordmethod;
    }

    public void validconnect(boolean validconnect) {
        this.validconnect = validconnect;
    }

    public void emailserver(String emailserver) {
        this.emailserver = emailserver;
    }

    public void emailport(int emailport) {
        this.emailport = emailport;
    }

    public void emailAccountID(String emailAccountID) {
        this.emailAccountID = emailAccountID;
    }

    public void emailUsername(String emailUsername) {
        this.emailUsername = emailUsername;
    }

    public void emailPassword(String emailPassword) {
        this.emailPassword = emailPassword;
    }

    public void discordtoken(String discordtoken) {
        this.discordtoken = discordtoken;
    }

    public void discordguild(Long discordguild) {
        this.discordguild = discordguild;
    }

    public void discordroom(String discordroom) {
        this.discordroom = discordroom;
    }

    public void discordlink(String discordlink) {
        this.discordlink = discordlink;
    }

    public void discordrole(String discordrole) {
        this.discordrole = discordrole;
    }

    public void discordprefix(String discordprefix) {
        this.discordprefix = discordprefix;
    }

    public void translate(boolean translate) {
        this.translate = translate;
    }

    public void translateid(String translateid) {
        this.translateid = translateid;
    }

    public void translatepw(String translatepw) {
        this.translatepw = translatepw;
    }

    public void debug(boolean debug) {
        this.debug = debug;
    }

    public void debugcode(String debugcode) {
        this.debugcode = debugcode;
    }

    public void crashreport(boolean crashreport) {
        this.crashreport = crashreport;
    }

    public void savetime(LocalTime savetime) {
        this.savetime = savetime;
    }

    public void rollback(boolean rollback) {
        this.rollback = rollback;
    }

    public void slotnumber(int slotnumber) {
        this.slotnumber = slotnumber;
    }

    public void autodifficulty(boolean autodifficulty) {
        this.autodifficulty = autodifficulty;
    }

    public void difficultyEasy(int difficultyEasy) {
        this.difficultyEasy = difficultyEasy;
    }

    public void difficultyNormal(int difficultyNormal) {
        this.difficultyNormal = difficultyNormal;
    }

    public void difficultyHard(int difficultyHard) {
        this.difficultyHard = difficultyHard;
    }

    public void difficultyInsane(int difficultyInsane) {
        this.difficultyInsane = difficultyInsane;
    }

    public void border(boolean border) {
        this.border = border;
    }

    public void spawnlimit(int spawnlimit) {
        this.spawnlimit = spawnlimit;
    }

    public void prefix(String prefix) {
        this.prefix = prefix;
    }

    public void eventport(String eventport) {
        this.eventport = eventport;
    }

    public void update() {
        locale = tool.TextToLocale(obj.getString("language", locale.toString()));
        Bundle bundle = new Bundle(locale);

        if (obj.getInt("version", 0) < config_version) Log.info("config-updated");

        // TODO config 구조 변경
        String text = "{\n" +
                "  # " + bundle.get("config-version-description") + "\n" +
                "  version: " + version + "\n" +
                "\n" +
                "  # " + bundle.get("config-language-description") + "\n" +
                "  language: " + language.toString() + "\n" +
                "\n" +
                "  # " + bundle.get("config-server/client-description") + "\n" +
                "  # " + bundle.get("config-server/client-child-description") + "\n" +
                "  server-enable: " + serverenable + "\n" +
                "  server-port: " + serverport + "\n" +
                "\n" +
                "  client-enable: " + clientenable + "\n" +
                "  client-port: " + clientport + "\n" +
                "  client-host: " + clienthost + "\n" +
                "\n" +
                "  # " + bundle.get("config-realname-description") + "\n" +
                "  # " + bundle.get("config-realname-child-description") + "\n" +
                "  # " + bundle.get("config-realname-strict-description") + "\n" +
                "  realname: " + realname + "\n" +
                "  strict-name: " + strictname + "\n" +
                "\n" +
                "  # " + bundle.get("config-colornick-description") + "\n" +
                "  cupdatei: " + cupdatei + "\n" +
                "\n" +
                "  # " + bundle.get("config-detectreactor-description") + "\n" +
                "  detectreactor: " + detectreactor + "\n" +
                "\n" +
                "  # " + bundle.get("config-scanresource-description") + "\n" +
                "  scanresource: " + scanresource + "\n" +
                "\n" +
                "  # " + bundle.get("config-antigrief-description") + "\n" +
                "  # " + bundle.get("config-antigrief-alert-description") + "\n" +
                "  antigrief: " + antigrief + "\n" +
                "  alert-action: " + alertaction + "\n" +
                "\n" +
                "  # " + bundle.get("config-exp-description") + "\n" +
                "  # " + bundle.get("config-exp-explimit-description") + "\n" +
                "  # " + bundle.get("config-exp-basexp-description") + "\n" +
                "  # " + bundle.get("config-exp-exponent-description") + "\n" +
                "  # " + bundle.get("config-exp-levelupalarm-description") + "\n" +
                "  # " + bundle.get("config-exp-minimal-level-description") + "\n" +
                "  explimit: " + explimit + "\n" +
                "  basexp: " + basexp + "\n" +
                "  exponent: " + exponent + "\n" +
                "  levelupalarm: " + levelupalarm + "\n" +
                "  alarm-minimal-level: " + alarmlevel + "\n" +
                "\n" +
                "  # " + bundle.get("config-banshare-description") + "\n" +
                "  # " + bundle.get("config-banshare-child-description") + "\n" +
                "  banshare: " + banshare + "\n" +
                "\n" +
                "  # " + bundle.get("config-bantrust-description") + "\n" +
                "  # " + bundle.get("config-bantrust-child-description") + "\n" +
                "  bantrust: " + bantrust + "\n" +
                "\n" +
                "  # " + bundle.get("config-query-description") + "\n" +
                "  # " + bundle.get("config-query-child-description") + "\n" +
                "  query: " + query + "\n" +
                "\n" +
                "  # " + bundle.get("config-antivpn-description") + "\n" +
                "  antivpn: " + antivpn + "\n" +
                "\n" +
                "  # " + bundle.get("config-enableantirush-description") + "\n" +
                "  # " + bundle.get("config-antirushtime-description") + "\n" +
                "  enable-antirush: " + antirush + "\n" +
                "  antirushtime: " + antirushtime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "\n" +
                "\n" +
                "  # " + bundle.get("config-vote-description") + "\n" +
                "  vote-enable: " + voteenable + "\n" +
                "\n" +
                "  # " + bundle.get("config-logging-description") + "\n" +
                "  logging: " + logging + "\n" +
                "\n" +
                "  # " + bundle.get("config-update-description") + "\n" +
                "  update: " + update + "\n" +
                "\n" +
                "  # " + bundle.get("config-database-description") + "\n" +
                "  # " + bundle.get("config-database-child1-description") + "\n" +
                "  # " + bundle.get("config-database-child2-description") + "\n" +
                "  internalDB: " + internalDB + "\n" +
                "  enable-db-server: " + DBServer + "\n" +
                "  dburl: " + DBurl + "\n" +
                "\n" +
                "  # " + bundle.get("config-old-database-migration-description") + "\n" +
                "  old-db-migration: " + OldDBMigration + "\n" +
                "  old-db-url: " + OldDBurl + "\n" +
                "  old-db-id: " + OldDBID + "\n" +
                "  old-db-pw: " + OldDBPW + "\n" +
                "\n" +
                "  # " + bundle.get("config-data-share-description") + "\n" +
                "  data-server-url: " + dataserverurl + "\n" +
                "  data-server-id: " + dataserverid + "\n" +
                "  data-server-password: " + dataserverpw + "\n" +
                "\n" +
                "  # " + bundle.get("config-login-description") + "\n" +
                "  # " + bundle.get("config-loginmethod-description") + "\n" +
                "  # " + bundle.get("config-validconnect-description") + "\n" +
                "  loginenable: " + loginenable + "\n" +
                "  loginmethod: " + passwordmethod + "\n" +
                "  validconnect: " + validconnect + "\n" +
                "\n" +
                "  # " + bundle.get("config-email-description") + "\n" +
                "  # " + bundle.get("config-email-child-description") + "\n" +
                "  email-smtp-server: " + emailserver + "\n" +
                "  email-smtp-port: " + emailport + "\n" +
                "  email-smtp-accountid: " + emailAccountID + "\n" +
                "  email-smtp-username: " + emailUsername + "\n" +
                "  email-smtp-password: " + emailPassword + "\n" +
                "\n" +
                "  # " + bundle.get("config-discord-description") + "\n" +
                "  # " + bundle.get("config-discord-role-description") + "\n" +
                "  discord-token: " + discordtoken + "\n" +
                "  discord-guild: " + discordguild + "\n" +
                "  discord-room: " + discordroom + "\n" +
                "  discord-link: " + discordlink + "\n" +
                "  discord-register-role: " + discordrole + "\n" +
                "  discord-command-prefix: " + discordprefix + "\n" +
                "\n" +
                "  # " + bundle.get("config-papago-description") + "\n" +
                "  # " + bundle.get("config-papago-child-description") + "\n" +
                "  enable-translate: " + translate + "\n" +
                "  clientId: " + translateid + "\n" +
                "  clientSecret: " + translatepw + "\n" +
                "\n" +
                "  # " + bundle.get("config-debug-description") + "\n" +
                "  debug: " + debug + "\n" +
                "  debugcode: " + debugcode + "\n" +
                "  crash-report: " + crashreport + "\n" +
                "\n" +
                "  # " + bundle.get("config-savetime-description") + "\n" +
                "  savetime: " + savetime.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "\n" +
                "\n" +
                "  # " + bundle.get("config-slotnumber-description") + "\n" +
                "  enable-rollback: " + rollback + "\n" +
                "  slotnumber: " + slotnumber + "\n" +
                "\n" +
                "  # " + bundle.get("config-auto-difficulty-description") + "\n" +
                "  auto-difficulty: " + autodifficulty + "\n" +
                "  easy: " + difficultyEasy + "\n" +
                "  normal: " + difficultyNormal + "\n" +
                "  hard: " + difficultyHard + "\n" +
                "  insane: " + difficultyInsane + "\n" +
                "\n" +
                "  # " + bundle.get("config-border-description") + "\n" +
                "  border: " + border + "\n" +
                "\n" +
                "  # " + bundle.get("config-spawnlimit-description") + "\n" +
                "  spawnlimit: " + spawnlimit + "\n" +
                "\n" +
                "  # " + bundle.get("config-prefix-description") + "\n" +
                "  prefix: \"" + prefix + "\"\n" +
                "\n" +
                "  # " + bundle.get("config-event-port-description") + "\n" +
                "  event-port: " + eventport + "\n" +
                "}";
        root.child("config.hjson").writeString(text);
    }
}
