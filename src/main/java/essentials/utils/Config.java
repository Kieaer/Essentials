package essentials.utils;

import essentials.Global;
import io.anuke.arc.Core;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
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

import static essentials.Global.nbundle;
import static essentials.Global.printStackTrace;
import static io.anuke.mindustry.Vars.net;

public class Config {
    private Map<String, Object> obj;
    static int version = 7;

    public Config(){
        if(Core.settings.getDataDirectory().child("mods/Essentials/config.yml").exists()) {
            Yaml yaml = new Yaml();
            obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/config.yml").readString()));
        }
    }

    public static JSONArray jumpzone;
    public static JSONArray jumpcount;
    public static JSONArray jumpall;

    public static ExecutorService executorService = Executors.newFixedThreadPool(6);

    public String getClienthost(){
        return obj != null && obj.get("client-enable") != null ? (String) obj.get("client-host") : "mindustry.kr";
    }

    public int getClientport(){
        return obj != null && obj.get("client-port") != null ? (int) obj.get("client-port") : 20000;
    }

    public int getServerport(){
        return obj != null && obj.get("server-port") != null ? (int) obj.get("server-port") : 25000;
    }

    public boolean isRealname(){
        return obj != null && obj.get("realname") == null && (boolean) obj.get("realname");
    }

    public boolean isDetectreactor(){
        return obj != null && obj.get("detectreactor") != null && (boolean) obj.get("detectreactor");
    }

    public boolean isScanresource(){
        return obj != null && obj.get("scanresource") != null && (boolean) obj.get("scanresource");
    }

    public boolean isServerenable(){
        return obj != null && obj.get("server-enable") != null && (boolean) obj.get("server-enable");
    }

    public boolean isClientenable(){
        return obj != null && obj.get("client-enable") != null && (boolean) obj.get("client-enable");
    }

    public double getBasexp(){
        return obj != null && obj.get("basexp") != null ? Double.parseDouble(String.valueOf(obj.get("basexp"))) : 500;
    }

    public double getExponent(){
        return obj != null && obj.get("exponent") != null ? Double.parseDouble(String.valueOf(obj.get("exponent"))) : 1.12f;
    }

    public int getCupdatei(){
        return obj != null && obj.get("cupdatei") != null ? (int) obj.get("cupdatei") : 1000;
    }

    public boolean isBanshare(){
        return obj != null && obj.get("banshare") == null && (boolean) obj.get("banshare");
    }

    public String[] getBantrust(){
        if(obj != null && obj.get("bantrust") != null) {
            String ban = (String) obj.get("bantrust");
            return ban.split(",");
        } else {
            return new String[0];
        }
    }

    public boolean isAntivpn(){
        return obj != null && obj.get("antivpn") == null && (boolean) obj.get("antivpn");
    }

    public boolean isQuery(){
        return obj != null && obj.get("query") != null && (boolean) obj.get("query");
    }

    public String getLanguage(){
        return obj != null && obj.get("language") != null ? (String) obj.get("language") : "en";
    }

    public boolean isEnableantirush(){
        return obj != null &&obj.get("enableantirush") == null && (boolean) obj.get("enableantirush");
    }

    public Calendar getAntirushtime() {
        SimpleDateFormat format = new SimpleDateFormat("mm:ss");
        Calendar cal = Calendar.getInstance();
        if(obj != null && obj.get("antirushtime") != null) {
            try {
                Date d = format.parse(String.valueOf(obj.get("antirushtime")));
                cal.setTime(d);
            } catch (Exception e) {
                printStackTrace(e);
                Global.normal("Invalid settings! - antirushtime");
                Global.normal("Correct value format is mm:ss (Example - 10:00 -> 10minute, 00:30 -> 30seconds)");
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
        return obj != null && obj.get("antigrief") == null && (boolean) obj.get("antigrief");
    }

    public boolean isBlockdetect(){
        return obj != null && obj.get("blockdetect") != null && (boolean) obj.get("blockdetect");
    }

    public boolean isAlertdeposit(){
        return obj != null && obj.get("alertdeposit") != null && (boolean) obj.get("alertdeposit");
    }

    public boolean isExplimit(){
        return obj != null && obj.get("explimit") != null && (boolean) obj.get("explimit");
    }

    public boolean isLogging(){
        return obj != null && obj.get("logging") != null && (boolean) obj.get("logging");
    }

    public boolean isUpdate(){
        return obj == null || obj.get("update") == null || (boolean) obj.get("update");
    }

    public boolean isLevelupalarm(){
        return obj != null && obj.get("levelupalarm") != null && (boolean) obj.get("levelupalarm");
    }

    public int getAlarmlevel(){
        return obj != null && obj.get("alarm-minimal-level") != null ? (int) obj.get("alarm-minimal-level") : 20;
    }

    public boolean isSqlite(){
        return obj == null || obj.get("sqlite") == null || (boolean) obj.get("sqlite");
    }

    public boolean isLoginenable(){
        return obj == null || obj.get("loginenable") == null || (boolean) obj.get("loginenable");
    }

    public String getPasswordmethod(){
        return obj != null && obj.get("loginmethod") != null ? (String) obj.get("loginmethod") : "password";
    }

    public boolean isValidconnect(){
        return obj == null || obj.get("validconnect") == null || (boolean) obj.get("validconnect");
    }

    public String getDBurl(){
        if(isSqlite()){
            return "jdbc:sqlite:"+Core.settings.getDataDirectory().child("mods/Essentials/data/player.sqlite3");
        } else {
            return obj != null && obj.get("dburl") != null ? (String) obj.get("dburl") : "";
        }
    }

    public String getDBid(){
        return obj != null && obj.get("dbid") != null ? (String) obj.get("dbid") : "";
    }

    public String getDBpw(){
        return obj != null && obj.get("dbpw") != null ? (String) obj.get("dbpw") : "";
    }

    public String getClientId(){
        return obj != null && obj.get("clientId") != null ? (String) obj.get("clientId") : "";
    }

    public String getClientSecret(){
        return obj != null && obj.get("clientSecret") != null ? (String) obj.get("clientSecret") : "";
    }

    public boolean isDebug(){
        return obj != null && obj.get("debug") != null && (boolean) obj.get("debug");
    }

    public int getSavetime(){
        return obj != null && obj.get("savetime") != null ? (int) obj.get("savetime") : 10;
    }

    public int getSlotnumber(){
        return obj != null && obj.get("slotnumber") != null ? (int) obj.get("slotnumber") : 1000;
    }

    private int getVersion(){
        return obj != null && obj.get("version") != null ? (int) obj.get("version") : version;
    }

    public boolean isAutodifficulty(){
        return obj != null && obj.get("auto-difficulty") != null && (boolean) obj.get("auto-difficulty");
    }

    public int getEasy(){
        return obj != null && obj.get("easy") != null ? (int) obj.get("easy") : 2;
    }

    public int getNormal(){
        return obj != null && obj.get("normal") != null ? (int) obj.get("normal") : 4;
    }

    public int getHard(){
        return obj != null && obj.get("hard") != null ? (int) obj.get("hard") : 6;
    }

    public int getInsane(){
        return obj != null && obj.get("insane") != null ? (int) obj.get("insane") : 10;
    }

    public int getSpawnlimit(){
        return obj != null && obj.get("spawnlimit") != null ? (int) obj.get("spawnlimit") : 10;
    }

    public String getServername(){
        JSONObject data = new JSONObject(new JSONTokener(Core.settings.getDataDirectory().child("mods/Essentials/data/data.json").readString()));
        return data.getString("servername");
    }

    private void validfile(){
        final String path = "configs";
        File jarFile;
        try {
            jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            Global.normal("The plugin file or folder path is invalid. Please check your server path!");
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

    public void main() {
        validfile();

        JSONObject data = new JSONObject(new JSONTokener(Core.settings.getDataDirectory().child("mods/Essentials/data/data.json").readString()));
        if(!data.has("servername")){
            data.put("servername", Core.settings.getString("servername"));
            Core.settings.getDataDirectory().child("mods/Essentials/data/data.json").writeString(data.toString());
        }

        update();

        Yaml yaml = new Yaml();
        obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/config.yml").readString()));

        // 서버간 이동 타일 불러오기
        jumpzone = new JSONArray(new JSONTokener(Core.settings.getDataDirectory().child("mods/Essentials/data/jumpdata.json").readString()));
        jumpcount = new JSONArray(new JSONTokener(Core.settings.getDataDirectory().child("mods/Essentials/data/jumpcount.json").readString()));
        jumpall = new JSONArray(new JSONTokener(Core.settings.getDataDirectory().child("mods/Essentials/data/jumpall.json").readString()));
    }

    void update(){
        String bantrust,antirushtime;
        if(obj != null && obj.get("bantrust") != null){
            bantrust = (String) obj.get("bantrust");
        } else {
            bantrust = "";
        }
        if(obj != null && obj.get("antirushtime") != null) {
            antirushtime = String.valueOf(obj.get("antirushtime"));
        } else {
            antirushtime = "10:00";
        }

        String text = "# "+nbundle("config-version-description")+"\n" +
                "version: "+getVersion()+"\n" +
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
                "realname: " + isRealname() + "\n" +
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
                "# "+nbundle("config-papago-description")+"\n" +
                "# "+nbundle("config-papago-child-description")+"\n" +
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
                "slotnumber: " + getSlotnumber() + "\n" +
                "\n" +
                "# "+nbundle("config-auto-difficulty-description")+"\n" +
                "auto-difficulty: false\n" +
                "easy: "+getEasy()+"\n" +
                "normal: "+getNormal()+"\n" +
                "hard: "+getHard()+"\n" +
                "insane: "+getInsane()+"\n" +
                "\n" +
                "# "+nbundle("config-spawnlimit-description")+"\n" +
                "spawnlimit: "+getSpawnlimit();
        Core.settings.getDataDirectory().child("mods/Essentials/config.yml").writeString(text);

        Global.config("config-loaded");
        if(getVersion() < version){
            Global.config("config-updated");
        }
    }
}