package essentials.utils;

import arc.Core;
import arc.files.Fi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import essentials.Global;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static essentials.Global.*;
import static mindustry.Vars.net;

public class Config {
    private Map<String, Object> obj;
    private Fi path = Core.settings.getDataDirectory().child("mods/Essentials/data/data.json");
    public static JsonObject Data = new JsonObject();
    public static JsonArray jumpzone = new JsonArray();
    public static JsonArray jumpcount = new JsonArray();
    public static JsonArray jumpall = new JsonArray();
    public static JsonArray blacklist = new JsonArray();
    public static JsonArray banned = new JsonArray();

    public static ExecutorService executorService = Executors.newFixedThreadPool(6, new Global.threadname("Essentials Thread"));
    public static ExecutorService singleService = Executors.newSingleThreadExecutor(new Global.threadname("Essentials single thread"));
    static int version = 9;

    public Config(){
        Yaml yaml = new Yaml();
        if (!Core.settings.getDataDirectory().child("mods/Essentials/config.yml").exists()) {
            Core.settings.getDataDirectory().child("mods/Essentials/config.yml").writeString("language: en");
        }
        obj = yaml.load(Core.settings.getDataDirectory().child("mods/Essentials/config.yml").readString());
    }

    public void main() {
        validfile();
        try{
            if(!path.exists()){
                Core.settings.getDataDirectory().child("mods/Essentials/data/data.json").writeString("{}");
            }

            JsonObject data = JsonParser.object().from(path.read());

            if(data.isEmpty()){
                JsonArray empty = new JsonArray();
                data.put("banned",empty);
                data.put("blacklist",empty);
                data.put("jumpzone",empty);
                data.put("jumpall",empty);
                data.put("jumpcount",empty);
                data.put("servername", Core.settings.getString("servername"));
                new ObjectMapper().writeValue(Core.settings.getDataDirectory().child("mods/Essentials/data/data.json").file(), data);
            }
            Data = data;

            update();

            Yaml yaml = new Yaml();
            obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/config.yml").readString()));

            jumpzone = JsonParser.object().from(path.read()).getArray("jumpzone");
            jumpcount = JsonParser.object().from(path.read()).getArray("jumpcount");
            jumpall = JsonParser.object().from(path.read()).getArray("jumpall");
            blacklist = JsonParser.object().from(path.read()).getArray("blacklist");
            banned = JsonParser.object().from(path.read()).getArray("banned");
        } catch (JsonParserException | IOException e){
            printStackTrace(e);
        }
    }

    void update(){
        String bantrust = obj.containsKey("bantrust") ? (String) obj.get("bantrust") : "127.0.0.1";
        String antirushtime = obj.containsKey("antirusttime") ? String.valueOf(obj.get("antirushtime")) : "10:00";
        String prefix = obj.containsKey("prefix") ? "\"" + obj.get("prefix") + "\"" : "\"[green][Essentials] []\"";
        if(obj.containsKey("version")){
            if((int) obj.get("version") < version){
                log("config","config-updated");
            }
        }

        String text = "# "+nbundle("config-version-description")+"\n" +
                "version: "+version+"\n" +
                "\n" +
                "# "+nbundle("config-language-description")+"\n" +
                "language: "+getLanguage()+"\n" +
                "\n" +
                "# "+nbundle("config-server/client-description")+"\n" +
                "# "+nbundle("config-server/client-child-description")+"\n" +
                "server-enable: " + isServerenable() + "\n" +
                "server-port: " + getServerport() + "\n" +
                "\n" +
                "client-enable: " + isClientenable() + "\n" +
                "client-port: " + getClientport() + "\n" +
                "client-host: " + getClienthost() + "\n" +
                "\n" +
                "# "+nbundle("config-realname-description")+"\n" +
                "# "+nbundle("config-realname-child-description")+"\n" +
                "# "+nbundle("config-realname-strict-description")+"\n" +
                "realname: " + isRealname() + "\n" +
                "strict-name: " + isStrictname() + "\n" +
                "\n" +
                "# "+nbundle("config-colornick-description")+"\n" +
                "colornick update interval: " + getCupdatei() + "\n" +
                "\n" +
                "# "+nbundle("config-detectreactor-description")+"\n" +
                "detectreactor: " + isDetectreactor() + "\n" +
                "\n" +
                "# "+nbundle("config-scanresource-description")+"\n" +
                "scanresource: " + isScanresource() + "\n" +
                "\n" +
                "# "+nbundle("config-antigrief-description")+"\n" +
                "# "+nbundle("config-blockdetect-description")+"\n" +
                "# "+nbundle("config-alertdeposit-description")+"\n" +
                "antigrief: "+ isAntigrief() + "\n" +
                "blockdetect: "+ isBlockdetect() + "\n" +
                "alertdeposit: " + isAlertdeposit() + "\n" +
                "\n" +
                "# "+nbundle("config-exp-description")+"\n" +
                "# "+nbundle("config-exp-explimit-description")+"\n" +
                "# "+nbundle("config-exp-basexp-description")+"\n" +
                "# "+nbundle("config-exp-exponent-description")+"\n" +
                "# "+nbundle("config-exp-levelupalarm-description")+"\n" +
                "# "+nbundle("config-exp-minimal-level-description")+"\n" +
                "explimit: " + isExplimit() + "\n" +
                "basexp: " + getBasexp() + "\n" +
                "exponent: " + getExponent() + "\n" +
                "levelupalarm: " + isLevelupalarm() + "\n" +
                "alarm-minimal-level: " + getAlarmlevel() + "\n" +
                "\n" +
                "# "+nbundle("config-banshare-description")+"\n" +
                "# "+nbundle("config-banshare-child-description")+"\n" +
                "banshare: " + isBanshare() + "\n" +
                "\n" +
                "# "+nbundle("config-bantrust-description")+"\n" +
                "# "+nbundle("config-bantrust-child-description")+"\n" +
                "bantrust: " + bantrust + "\n" +
                "\n" +
                "# "+nbundle("config-query-description")+"\n" +
                "# "+nbundle("config-query-child-description")+"\n" +
                "query: " + isQuery() + "\n" +
                "\n" +
                "# "+nbundle("config-antivpn-description")+"\n" +
                "antivpn: " + isAntivpn() + "\n" +
                "\n" +
                "# "+nbundle("config-enableantirush-description")+"\n" +
                "# "+nbundle("config-antirushtime-description")+"\n" +
                "enableantirush: " + isEnableantirush() + "\n" +
                "antirushtime: " + antirushtime + "\n" +
                "\n" +
                "# "+nbundle("config-logging-description")+"\n" +
                "logging: " + isLogging() + "\n" +
                "\n" +
                "# "+nbundle("config-update-description")+"\n" +
                "update: " + isUpdate() + "\n" +
                "\n" +
                "# "+nbundle("config-database-description")+"\n" +
                "# "+nbundle("config-database-child1-description")+"\n" +
                "# "+nbundle("config-database-child2-description")+"\n" +
                "sqlite: " + isSqlite() + "\n" +
                "dburl: " + getDBurl() + "\n" +
                "dbid: " + getDBid() + "\n" +
                "dbpw: " + getDBpw() + "\n" +
                "\n" +
                "# "+nbundle("config-login-description")+"\n" +
                "# "+nbundle("config-loginmethod-description")+"\n" +
                "# "+nbundle("config-validconnect-description")+"\n" +
                "loginenable: " + isLoginenable() + "\n" +
                "loginmethod: " + getPasswordmethod() + "\n" +
                "validconnect: " + isValidconnect() + "\n" +
                "\n" +
                "# "+nbundle("config-discord-description")+"\n" +
                "# "+nbundle("config-discord-role-description")+"\n" +
                "discord-token: " + getDiscordToken() + "\n" +
                "discord-guild: " + getDiscordGuild() + "\n" +
                "discord-room: " + getDiscordRoom() + "\n" +
                "discord-link: " + getDiscordLink() + "\n" +
                "discord-register-role: " + getDiscordRole() +"\n" +
                "discord-command-prefix: " + getDiscordPrefix() + "\n" +
                "\n" +
                "# "+nbundle("config-papago-description")+"\n" +
                "# "+nbundle("config-papago-child-description")+"\n" +
                "enable-translate: " + isEnableTranslate() + "\n" +
                "clientId: " + getClientId() + "\n" +
                "clientSecret: " + getClientSecret() + "\n" +
                "\n" +
                "# "+nbundle("config-debug-description")+"\n" +
                "debug: " + isDebug() + "\n" +
                "\n" +
                "# "+nbundle("config-savetime-description")+"\n" +
                "savetime: " + getSavetime() + "\n" +
                "\n" +
                "# "+nbundle("config-slotnumber-description")+"\n" +
                "enable-rollback:" + isEnableRollback() + "\n" +
                "slotnumber: " + getSlotnumber() + "\n" +
                "\n" +
                "# "+nbundle("config-auto-difficulty-description")+"\n" +
                "auto-difficulty: "+isAutodifficulty()+"\n" +
                "easy: "+getEasy()+"\n" +
                "normal: "+getNormal()+"\n" +
                "hard: "+getHard()+"\n" +
                "insane: "+getInsane()+"\n" +
                "\n" +
                "# "+nbundle("config-spawnlimit-description")+"\n" +
                "spawnlimit: "+getSpawnlimit() +"\n" +
                "\n" +
                "# "+nbundle("config-prefix-description")+"\n"+
                "prefix: "+prefix+"\n" +
                "\n" +
                "# "+nbundle("config-event-port-description")+"\n"+
                "event-port: "+getEventport();
        Core.settings.getDataDirectory().child("mods/Essentials/config.yml").writeString(text);

        log("config","config-loaded");
    }

    public String getClienthost(){
        return obj.containsKey("client-enable") ? (String) obj.get("client-host") : "mindustry.kr";
    }

    public int getClientport(){
        return obj.containsKey("client-port") && (int) obj.get("client-port") != 20000 ? (int) obj.get("client-port") : 25000;
    }

    public int getServerport(){
        return obj.containsKey("server-port") ? (int) obj.get("server-port") : 25000;
    }

    public boolean isRealname(){
        return obj.containsKey("realname") && (boolean) obj.get("realname");
    }

    public boolean isStrictname() {
        return obj.containsKey("strict-name") && (boolean) obj.get("strict-name");
    }

    public boolean isDetectreactor(){
        return obj.containsKey("detectreactor") && (boolean) obj.get("detectreactor");
    }

    public boolean isScanresource(){
        return obj.containsKey("scanresource") && (boolean) obj.get("scanresource");
    }

    public boolean isServerenable(){
        return obj.containsKey("server-enable") && (boolean) obj.get("server-enable");
    }

    public boolean isClientenable(){
        return obj.containsKey("client-enable") && (boolean) obj.get("client-enable");
    }

    public double getBasexp(){
        return obj.containsKey("basexp") ? Double.parseDouble(String.valueOf(obj.get("basexp"))) : 500;
    }

    public double getExponent(){
        return obj.containsKey("exponent") ? Double.parseDouble(String.valueOf(obj.get("exponent"))) : 1.12f;
    }

    public int getCupdatei(){
        return obj.containsKey("cupdatei") ? (int) obj.get("cupdatei") : 1000;
    }

    public boolean isBanshare(){
        return obj.containsKey("banshare") && (boolean) obj.get("banshare");
    }

    public String[] getBantrust(){
        if(obj.containsKey("bantrust")) {
            String ban = (String) obj.get("bantrust");
            return ban.split(",");
        } else {
            return new String[0];
        }
    }

    public boolean isAntivpn(){
        return obj.containsKey("antivpn") && (boolean) obj.get("antivpn");
    }

    public boolean isQuery(){
        return obj.containsKey("query") && (boolean) obj.get("query");
    }

    public String getLanguage(){
        return obj.containsKey("language") ? (String) obj.get("language") : "en";
    }

    public boolean isEnableantirush(){
        return obj.containsKey("enableantirush") && (boolean) obj.get("enableantirush");
    }

    public Calendar getAntirushtime() {
        SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        Calendar cal = Calendar.getInstance();
        if(obj.containsKey("antirushtime")) {
            try {
                Date d = format.parse(String.valueOf(obj.get("antirushtime")));
                cal.setTime(d);
            } catch (Exception e) {
                printStackTrace(e);
                nlog("warn","Invalid settings! - antirushtime");
                nlog("warn","Correct value format is mm:ss (Example - 10:00 -> 10minute, 00:30 -> 30seconds)");
            }
        } else {
            try{
                Date d = format.parse("10:00");
                cal.setTime(d);
            } catch (Exception e){
                printStackTrace(e);
            }
        }
        return cal;
    }

    public boolean isAntigrief(){
        return obj.containsKey("antigrief") && (boolean) obj.get("antigrief");
    }

    public boolean isBlockdetect(){
        return obj.containsKey("blockdetect") && (boolean) obj.get("blockdetect");
    }

    public boolean isAlertdeposit(){
        return obj.containsKey("alertdeposit") && (boolean) obj.get("alertdeposit");
    }

    public boolean isExplimit(){
        return obj.containsKey("explimit") && (boolean) obj.get("explimit");
    }

    public boolean isLogging(){
        return obj.containsKey("logging") && (boolean) obj.get("logging");
    }

    public boolean isUpdate(){
        return !obj.containsKey("update") || (boolean) obj.get("update");
    }

    public boolean isLevelupalarm(){
        return obj.containsKey("levelupalarm") && (boolean) obj.get("levelupalarm");
    }

    public int getAlarmlevel(){
        return obj.containsKey("alarm-minimal-level") ? (int) obj.get("alarm-minimal-level") : 20;
    }

    public boolean isSqlite(){
        return !obj.containsKey("sqlite") || (boolean) obj.get("sqlite");
    }

    public boolean isLoginenable(){
        return obj.containsKey("loginenable") && (boolean) obj.get("loginenable");
    }

    public String getPasswordmethod(){
        return obj.containsKey("loginmethod") ? (String) obj.get("loginmethod") : "password";
    }

    public boolean isValidconnect(){
        return obj.containsKey("validconnect") && (boolean) obj.get("validconnect");
    }

    public String getDiscordToken(){
        return obj.containsKey("discord-token") ? (String) obj.get("discord-token") : "Put your discord bot token here";
    }

    public Long getDiscordGuild(){
        return obj.containsKey("discord-guild") ? Long.parseLong(String.valueOf(obj.get("discord-guild"))) : 0L;
    }

    public Long getDiscordRoom(){
        return obj.containsKey("discord-room") ? Long.parseLong(String.valueOf(obj.get("discord-room"))) : 0L;
    }

    public String getDiscordLink(){
        return obj.containsKey("discord-link") ? (String) obj.get("discord-link") : "Put your discord invite link here";
    }

    public String getDiscordRole(){
        return obj.containsKey("discord-register-role") ? (String) obj.get("discord-register-role") : "";
    }

    public String getDiscordPrefix(){
        return obj.containsKey("discord-command-prefix") ? (String) obj.get("discord-command-prefix") : "!";
    }

    public String getDBurl(){
        if(isSqlite()){
            return "jdbc:sqlite:"+Core.settings.getDataDirectory().child("mods/Essentials/data/player.sqlite3");
        } else {
            return obj.containsKey("dburl") ? (String) obj.get("dburl") : "";
        }
    }

    public String getDBid(){
        return obj.containsKey("dbid") ? (String) obj.get("dbid") : "";
    }

    public String getDBpw(){
        return obj.containsKey("dbpw") ? (String) obj.get("dbpw") : "";
    }

    public boolean isEnableTranslate(){
        return obj.containsKey("enable-translate") && (boolean) obj.get("enable-translate");
    }

    public String getClientId(){
        return obj.containsKey("clientId") ? (String) obj.get("clientId") : "";
    }

    public String getClientSecret(){
        return obj.containsKey("clientSecret") ? (String) obj.get("clientSecret") : "";
    }

    public boolean isDebug(){
        return obj.containsKey("debug") && (boolean) obj.get("debug");
    }

    public int getSavetime(){
        return obj.containsKey("savetime") ? (int) obj.get("savetime") : 10;
    }

    public boolean isEnableRollback(){
        return obj.containsKey("enable-rollback") && (boolean) obj.get("enable-rollback");
    }

    public int getSlotnumber(){
        return obj.containsKey("slotnumber") ? (int) obj.get("slotnumber") : 1000;
    }

    public boolean isAutodifficulty(){
        return obj.containsKey("auto-difficulty") && (boolean) obj.get("auto-difficulty");
    }

    public int getEasy(){
        return obj.containsKey("easy") ? (int) obj.get("easy") : 2;
    }

    public int getNormal(){
        return obj.containsKey("normal") ? (int) obj.get("normal") : 4;
    }

    public int getHard(){
        return obj.containsKey("hard") ? (int) obj.get("hard") : 6;
    }

    public int getInsane(){
        return obj.containsKey("insane") ? (int) obj.get("insane") : 10;
    }

    public int getSpawnlimit(){
        return obj.containsKey("spawnlimit") ? (int) obj.get("spawnlimit") : 10;
    }

    public String getPrefix(){
        return obj.containsKey("prefix") ? String.valueOf(obj.get("prefix")) : "[green][Essentials] []";
    }

    public String getServername(){
        try {
            return JsonParser.object().from(path.read()).getString("servername");
        } catch (JsonParserException e) {
            printStackTrace(e);
        }
        return "";
    }

    public String getEventport(){
        return obj.containsKey("event-port") ? (String) obj.get("event-port") : "8000-8050";
    }

    private void validfile(){
        final String path = "configs";
        File jarFile;
        try {
            jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            nlog("warn","The plugin file or folder path is invalid. Please check your server path!");
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
                            if(!Core.settings.getDataDirectory().child("mods/Essentials/" + name.replaceFirst("configs/", "")).exists()) {
                                if (!name.contains(".")) {
                                    Core.settings.getDataDirectory().child("mods/Essentials/" + name.replaceFirst("configs/", "")).mkdirs();
                                    continue;
                                }
                                InputStream reader = getClass().getResourceAsStream("/" + name);
                                Core.settings.getDataDirectory().child("mods/Essentials/" + name.replaceFirst("configs/", "")).write(reader, false);
                            }
                        }
                    }
                }
                jar.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}