package remake.core.plugin;

import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import remake.internal.Bundle;
import remake.internal.Log;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static remake.Main.*;
import static remake.PluginVars.config_version;

public class Config {
    public final int version;
    public final Locale language;
    public final boolean serverenable;
    public final int serverport;
    public final boolean clientenable;
    public final int clientport;
    public final String clienthost;
    public final boolean realname;
    public final boolean strictname;
    public final int cupdatei;
    public final boolean detectreactor;
    public final boolean scanresource;
    public final boolean antigrief;
    public final boolean alertaction;
    public final boolean explimit;
    public final double basexp;
    public final double exponent;
    public final boolean levelupalarm;
    public final int alarmlevel;
    public final boolean banshare;
    public final JsonArray bantrust;
    public final boolean query;
    public final boolean antivpn;
    public final boolean antirush;
    public final LocalTime antirushtime;
    public final boolean voteenable;
    public final boolean logging;
    public final boolean update;
    public final boolean internalDB;
    public final boolean DBServer;
    public final String DBurl;
    public final boolean OldDBMigration;
    public final String OldDBurl;
    public final String OldDBID;
    public final String OldDBPW;
    public final String dataserverurl;
    public final String dataserverid;
    public final String dataserverpw;
    public final boolean loginenable;
    public final String passwordmethod;
    public final boolean validconnect;
    public final String emailserver;
    public final int emailport;
    public final String emailAccountID;
    public final String emailUsername;
    public final String emailPassword;
    public final String discordtoken;
    public final Long discordguild;
    public final String discordroom;
    public final String discordlink;
    public final String discordrole;
    public final String discordprefix;
    public final boolean translate;
    public final String translateid;
    public final String translatepw;
    public final boolean debug;
    public final String debugcode;
    public final boolean crashreport;
    public final LocalTime savetime;
    public final boolean rollback;
    public final int slotnumber;
    public final boolean autodifficulty;
    public final int difficultyEasy;
    public final int difficultyNormal;
    public final int difficultyHard;
    public final int difficultyInsane;
    public final boolean border;
    public final int spawnlimit;
    public final String prefix;
    public final String eventport;

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

    public void update() {
        locale = tool.TextToLocale(obj.getString("language", locale.toString()));
        Bundle bundle = new Bundle(locale);

        if (obj.getInt("version", 0) < config_version) Log.info("config-updated");

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
                "  antirushtime: " + antirushtime.toString() + "\n" +
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
                "  savetime: " + savetime.toString() + "\n" +
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
