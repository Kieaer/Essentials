package essentials.utils;

import arc.Core;
import arc.util.ArcRuntimeException;
import essentials.Global;
import org.hjson.JsonArray;
import org.hjson.JsonObject;
import org.hjson.JsonValue;
import org.hjson.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.time.LocalTime;
import java.util.Enumeration;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static essentials.Global.*;
import static essentials.Main.data;
import static essentials.Main.root;
import static mindustry.Vars.net;

public class Config {
    private JsonObject obj = new JsonObject();
    public JsonObject PluginConfig = new JsonObject();

    public ExecutorService executorService = Executors.newFixedThreadPool(6, new Global.threadname("Essentials Thread"));
    public ExecutorService singleService = Executors.newSingleThreadExecutor(new Global.threadname("Essentials single thread"));
    int config_version = 12;

    public void main(){
        extract();
        try{
            JsonObject json = new JsonObject();

            try {
                json = JsonValue.readJSON(root.child("data/data.json").reader()).asObject();
            }catch (ParseException | ArcRuntimeException ignored){
                json.add("servername", Core.settings.getString("servername"));
                json.add("unexception",false);
                json.add("internalDB",true);
                root.child("data/data.json").writeString(json.toString());
            }

            PluginConfig = json;

            if(root.child("config.hjson").exists()) obj = JsonValue.readHjson(root.child("config.hjson").readString()).asObject();
            update();

            data.loadall();
            log(LogType.config,"config-language",this.getLanguage().getDisplayLanguage(locale));
            log(LogType.config,"config-loaded");
        } catch (IOException e){
            printError(e);
        }
    }

    void update(){
        locale = TextToLocale(obj.getString("language", locale.toString()));

        int antirushtime;
        try{
            antirushtime = obj.getInt("antirushtime",600);
        } catch (UnsupportedOperationException e){
            antirushtime = 600;
        }

        if(obj.getInt("version",0) < config_version) log(LogType.config,"config-updated");

        String text = "{\n"+
                "  # "+nbundle("config-version-description")+"\n" +
                "  version: "+getVersion()+"\n" +
                "\n" +
                "  # "+nbundle("config-language-description")+"\n" +
                "  language: "+getLanguage().toString()+"\n" +
                "\n" +
                "  # "+nbundle("config-server/client-description")+"\n" +
                "  # "+nbundle("config-server/client-child-description")+"\n" +
                "  server-enable: " + isServerenable() + "\n" +
                "  server-port: " + getServerport() + "\n" +
                "\n" +
                "  client-enable: " + isClientenable() + "\n" +
                "  client-port: " + getClientport() + "\n" +
                "  client-host: " + getClienthost() + "\n" +
                "\n" +
                "  # "+nbundle("config-realname-description")+"\n" +
                "  # "+nbundle("config-realname-child-description")+"\n" +
                "  # "+nbundle("config-realname-strict-description")+"\n" +
                "  realname: " + isRealname() + "\n" +
                "  strict-name: " + isStrictname() + "\n" +
                "\n" +
                "  # "+nbundle("config-colornick-description")+"\n" +
                "  cupdatei: " + getCupdatei() + "\n" +
                "\n" +
                "  # "+nbundle("config-detectreactor-description")+"\n" +
                "  detectreactor: " + isDetectreactor() + "\n" +
                "\n" +
                "  # "+nbundle("config-scanresource-description")+"\n" +
                "  scanresource: " + isScanresource() + "\n" +
                "\n" +
                "  # "+nbundle("config-antigrief-description")+"\n" +
                "  # "+nbundle("config-antigrief-alert-description")+"\n" +
                "  antigrief: "+ isAntigrief() + "\n" +
                "  alert-action: "+ isAlertaction() + "\n" +
                "\n" +
                "  # "+nbundle("config-exp-description")+"\n" +
                "  # "+nbundle("config-exp-explimit-description")+"\n" +
                "  # "+nbundle("config-exp-basexp-description")+"\n" +
                "  # "+nbundle("config-exp-exponent-description")+"\n" +
                "  # "+nbundle("config-exp-levelupalarm-description")+"\n" +
                "  # "+nbundle("config-exp-minimal-level-description")+"\n" +
                "  explimit: " + isExplimit() + "\n" +
                "  basexp: " + getBasexp() + "\n" +
                "  exponent: " + getExponent() + "\n" +
                "  levelupalarm: " + isLevelupalarm() + "\n" +
                "  alarm-minimal-level: " + getAlarmlevel() + "\n" +
                "\n" +
                "  # "+nbundle("config-banshare-description")+"\n" +
                "  # "+nbundle("config-banshare-child-description")+"\n" +
                "  banshare: " + isBanshare() + "\n" +
                "\n" +
                "  # "+nbundle("config-bantrust-description")+"\n" +
                "  # "+nbundle("config-bantrust-child-description")+"\n" +
                "  bantrust: " + getBantrust() + "\n" +
                "\n" +
                "  # "+nbundle("config-query-description")+"\n" +
                "  # "+nbundle("config-query-child-description")+"\n" +
                "  query: " + isQuery() + "\n" +
                "\n" +
                "  # "+nbundle("config-antivpn-description")+"\n" +
                "  antivpn: " + isAntivpn() + "\n" +
                "\n" +
                "  # "+nbundle("config-enableantirush-description")+"\n" +
                "  # "+nbundle("config-antirushtime-description")+"\n" +
                "  enable-antirush: " + isEnableantirush() + "\n" +
                "  antirushtime: " + antirushtime + "\n" +
                "\n" +
                "  # "+nbundle("config-vote-description")+"\n" +
                "  vote-enable: " + isVoteEnable() + "\n" +
                "\n" +
                "  # "+nbundle("config-logging-description")+"\n" +
                "  logging: " + isLogging() + "\n" +
                "\n" +
                "  # "+nbundle("config-update-description")+"\n" +
                "  update: " + isUpdate() + "\n" +
                "\n" +
                "  # "+nbundle("config-database-description")+"\n" +
                "  # "+nbundle("config-database-child1-description")+"\n" +
                "  # "+nbundle("config-database-child2-description")+"\n" +
                "  isInternalDB: " + isInternalDB() + "\n" +
                "  dburl: " + getDBurl() + "\n" +
                "  dbid: " + getDBid() + "\n" +
                "  dbpw: " + getDBpw() + "\n" +
                "\n" +
                "  # "+nbundle("config-data-share-description")+"\n" +
                "  data-server-url: " + getDataServerURL() + "\n" +
                "  data-server-id: " + getDataServerID() + "\n" +
                "  data-server-password: " + getDataServerPassword() + "\n" +
                "\n" +
                "  # "+nbundle("config-login-description")+"\n" +
                "  # "+nbundle("config-loginmethod-description")+"\n" +
                "  # "+nbundle("config-validconnect-description")+"\n" +
                "  loginenable: " + isLoginenable() + "\n" +
                "  loginmethod: " + getPasswordmethod() + "\n" +
                "  validconnect: " + isValidconnect() + "\n" +
                "\n" +
                "  # "+nbundle("config-email-description")+"\n" +
                "  # "+nbundle("config-email-child-description")+"\n" +
                "  email-smtp-server: " + getEmailServer() + "\n" +
                "  email-smtp-port: " + getEmailPort() + "\n" +
                "  email-smtp-accountid: " + getEmailAccountID() + "\n" +
                "  email-smtp-username: " + getEmailUsername() + "\n" +
                "  email-smtp-password: " + getEmailPassword() + "\n" +
                "\n" +
                "  # "+nbundle("config-discord-description")+"\n" +
                "  # "+nbundle("config-discord-role-description")+"\n" +
                "  discord-token: " + getDiscordToken() + "\n" +
                "  discord-guild: " + getDiscordGuild() + "\n" +
                "  discord-room: " + getDiscordRoom() + "\n" +
                "  discord-link: " + getDiscordLink() + "\n" +
                "  discord-register-role: " + getDiscordRole() +"\n" +
                "  discord-command-prefix: " + getDiscordPrefix() + "\n" +
                "\n" +
                "  # "+nbundle("config-papago-description")+"\n" +
                "  # "+nbundle("config-papago-child-description")+"\n" +
                "  enable-translate: " + isEnableTranslate() + "\n" +
                "  clientId: " + getClientId() + "\n" +
                "  clientSecret: " + getClientSecret() + "\n" +
                "\n" +
                "  # "+nbundle("config-debug-description")+"\n" +
                "  debug: " + isDebug() + "\n" +
                "  debugcode: " + getDebugCode() + "\n" +
                "  crash-report: " + isCrashReport() + "\n" +
                "\n" +
                "  # "+nbundle("config-savetime-description")+"\n" +
                "  savetime: " + getSavetime() + "\n" +
                "\n" +
                "  # "+nbundle("config-slotnumber-description")+"\n" +
                "  enable-rollback: " + isEnableRollback() + "\n" +
                "  slotnumber: " + getSlotnumber() + "\n" +
                "\n" +
                "  # "+nbundle("config-auto-difficulty-description")+"\n" +
                "  auto-difficulty: "+isAutodifficulty()+"\n" +
                "  easy: "+getEasy()+"\n" +
                "  normal: "+getNormal()+"\n" +
                "  hard: "+getHard()+"\n" +
                "  insane: "+getInsane()+"\n" +
                "\n" +
                "  # "+nbundle("config-border-description")+"\n" +
                "  border: "+isBorder()+"\n" +
                "\n" +
                "  # "+nbundle("config-spawnlimit-description")+"\n" +
                "  spawnlimit: "+getSpawnlimit() +"\n" +
                "\n" +
                "  # "+nbundle("config-prefix-description")+"\n"+
                "  prefix: \""+getPrefix()+"\"\n" +
                "\n" +
                "  # "+nbundle("config-event-port-description")+"\n"+
                "  event-port: "+getEventport()+"\n"+
                "}";
        root.child("config.hjson").writeString(text);
    }

    public String getClienthost(){
        return obj.getString("client-host","mindustry.kr");
    }

    public int getClientport(){
        return obj.getInt("client-port",25000);
    }

    public int getServerport(){
        return obj.getInt("server-port",25000);
    }

    public String getDataServerURL(){
        return obj.getString("data-server-url","none");
    }

    public String getDataServerID(){
        return obj.getString("data-server-id","none");
    }

    public String getDataServerPassword(){
        return obj.getString("data-server-password","none");
    }

    public boolean isRealname(){
        return obj.getBoolean("realname",false);
    }

    public boolean isStrictname() {
        return obj.getBoolean("strict-name",false);
    }

    public boolean isDetectreactor(){
        return obj.getBoolean("detectreactor",false);
    }

    public boolean isScanresource(){
        return obj.getBoolean("scanresource",false);
    }

    public boolean isServerenable(){
        return obj.getBoolean("server-enable",false);
    }

    public boolean isClientenable(){
        return obj.getBoolean("client-enable",false);
    }

    public double getBasexp(){
        return obj.getDouble("basexp",500);
    }

    public double getExponent(){
        return obj.getDouble("exponent",1.12f);
    }

    public int getCupdatei(){
        return obj.getInt("cupdatei",1000);
    }

    public boolean isBanshare(){
        return obj.getBoolean("banshare",false);
    }

    public JsonArray getBantrust(){
        try{
            return obj.get("bantrust") != null ? obj.get("bantrust").asArray() : new JsonArray().add("127.0.0.1").asArray();
        }catch (UnsupportedOperationException e){
            return new JsonArray().add("127.0.0.1").asArray();
        }
    }

    public boolean isAntivpn(){
        return obj.getBoolean("antivpn",false);
    }

    public boolean isQuery(){
        return obj.getBoolean("query",false);
    }

    public int getVersion(){
        return config_version;
        //return obj.getInt("version", config_version);
    }
    public Locale getLanguage(){
        return TextToLocale(obj.getString("language", locale.toString()));
    }

    public boolean isEnableantirush(){
        return obj.getBoolean("enable-antirush",false);
    }

    public boolean isVoteEnable(){
        return obj.getBoolean("vote-enable", true);
    }

    public LocalTime getAntirushtime() {
        return obj.get("antirushtime") != null ? LocalTime.of(0,0,0).plusSeconds(obj.get("antirushtime").asInt()) : LocalTime.of(0,10,0);
    }

    public boolean isAntigrief(){
        return obj.getBoolean("antigrief",false);
    }

    public boolean isAlertaction(){
        return obj.getBoolean("alert-action",false);
    }

    public boolean isExplimit(){
        return obj.getBoolean("explimit",false);
    }

    public boolean isLogging(){
        return obj.getBoolean("logging",false);
    }

    public boolean isUpdate(){
        return obj.getBoolean("update",true);
    }

    public boolean isLevelupalarm(){
        return obj.getBoolean("levelupalarm",false);
    }

    public int getAlarmlevel(){
        return obj.getInt("alarm-minimal-level",20);
    }

    public boolean isInternalDB(){
        return obj.getBoolean("internalDB",true);
    }

    public boolean isLoginenable(){
        return obj.getBoolean("loginenable",false);
    }

    public String getPasswordmethod(){
        return obj.getString("loginmethod","password");
    }

    public boolean isValidconnect(){
        return obj.getBoolean("validconnect",false);
    }

    public String getEmailServer(){
        return obj.getString("email-smtp-server", "smtp.gmail.com");
    }

    public int getEmailPort(){
        return obj.getInt("email-smtp-port", 587);
    }

    public String getEmailAccountID(){
        return obj.getString("email-smtp-accountid", "none");
    }

    public String getEmailUsername(){
        return obj.getString("email-smtp-username", "none");
    }

    public String getEmailPassword(){
        return obj.getString("email-smtp-password", "none");
    }

    public String getDiscordToken(){
        return obj.getString("discord-token","Put your discord bot token here");
    }

    public Long getDiscordGuild(){
        return obj.getLong("discord-guild",0L);
    }

    public String getDiscordRoom(){
        return obj.getString("discord-room","none");
    }

    public String getDiscordLink(){
        return obj.getString("discord-link","Put your discord invite link here");
    }

    public String getDiscordRole(){
        return obj.getString("discord-register-role","none");
    }

    public String getDiscordPrefix(){
        return obj.getString("discord-command-prefix","!");
    }

    public String getDBurl(){
        if(isInternalDB()){
            return "jdbc:h2:file:"+root.child("data/player").absolutePath()+"";
        } else {
            return obj.getString("dburl","none");
        }
    }

    public String getDBid(){
        return obj.getString("dbid","none");
    }

    public String getDBpw(){
        return obj.getString("dbpw","none");
    }

    public boolean isEnableTranslate(){
        return obj.getBoolean("enable-translate",false);
    }

    public String getClientId(){
        return obj.getString("clientId","none");
    }

    public String getClientSecret(){
        return obj.getString("clientSecret","none");
    }

    public boolean isDebug(){
        return obj.getBoolean("debug",false);
    }

    public String getDebugCode(){
        return obj.getString("debugcode","none");
    }

    public boolean isCrashReport(){
        return obj.getBoolean("crash-report",true);
    }

    public int getSavetime(){
        return obj.getInt("savetime",10);
    }

    public boolean isEnableRollback(){
        return obj.getBoolean("enable-rollback",false);
    }

    public int getSlotnumber(){
        return obj.getInt("slotnumber",1000);
    }

    public boolean isAutodifficulty(){
        return obj.getBoolean("auto-difficulty",false);
    }

    public int getEasy(){
        return obj.getInt("easy",2);
    }

    public int getNormal(){
        return obj.getInt("normal",4);
    }

    public int getHard(){
        return obj.getInt("hard",6);
    }

    public int getInsane(){
        return obj.getInt("insane",10);
    }

    public boolean isBorder(){
        return obj.getBoolean("border",false);
    }

    public int getSpawnlimit(){
        return obj.getInt("spawnlimit",30);
    }

    public String getPrefix(){
        return obj.getString("prefix","[green][Essentials] []");
    }

    public String getServername(){
        return PluginConfig.getString("servername","Essentials server");
    }

    public String getEventport(){
        return obj.getString("event-port","8000-8050");
    }

    public void extract(){
        final String path = "configs";
        File jarFile;
        try {
            jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            nlog(LogType.warn,"The plugin file or folder path is invalid. Please check your server path!");
            net.dispose();
            Core.app.exit();
            return;
        }

        if (jarFile.isFile()) {
            try {
                final JarFile jar = new JarFile(jarFile);
                final Enumeration<JarEntry> entries = jar.entries();
                while (entries.hasMoreElements()) {
                    String name = entries.nextElement().getName();
                    if (name.startsWith(path + "/")) {
                        if (!name.equals(path + "/")) {
                            if(!root.child("" + name.replaceFirst("configs/", "")).exists()) {
                                if (!name.contains(".")) {
                                    root.child("" + name.replaceFirst("configs/", "")).mkdirs();
                                    continue;
                                }
                                InputStream reader = getClass().getResourceAsStream("/" + name);
                                root.child("" + name.replaceFirst("configs/", "")).write(reader, false);
                            }
                        }
                    }
                }
                jar.close();
            } catch (IOException e) {
                printError(e);
            }
        }
    }
}