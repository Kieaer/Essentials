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
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static essentials.Global.printStackTrace;
import static io.anuke.mindustry.Vars.net;

public class Config {
    private Map<String, Object> obj;

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
        return obj.get("client-enable") != null ? (String) obj.get("client-host") : "mindustry.kr";
    }

    public int getClientport(){
        return obj.get("client-port") != null ? (int) obj.get("client-port") : 20000;
    }

    public int getServerport(){
        return obj.get("server-port") != null ? (int) obj.get("server-port") : 25000;
    }

    public boolean isRealname(){
        return obj.get("realname") == null || (boolean) obj.get("realname");
    }

    public boolean isDetectreactor(){
        return obj.get("detectreactor") != null && (boolean) obj.get("detectreactor");
    }

    public boolean isScanresource(){
        return obj.get("scanresource") != null && (boolean) obj.get("scanresource");
    }

    public boolean isServerenable(){
        return obj.get("server-enable") != null && (boolean) obj.get("server-enable");
    }

    public boolean isClientenable(){
        return obj.get("client-enable") != null && (boolean) obj.get("client-enable");
    }

    public double getBasexp(){
        return obj.get("basexp") != null ? Double.parseDouble(String.valueOf(obj.get("basexp"))) : 500;
    }

    public double getExponent(){
        return obj.get("exponent") != null ? Double.parseDouble(String.valueOf(obj.get("exponent"))) : 1.12f;
    }

    public int getCupdatei(){
        return obj.get("cupdatei") != null ? (int) obj.get("cupdatei") : 1000;
    }

    public boolean isBanshare(){
        return obj.get("banshare") == null || (boolean) obj.get("banshare");
    }

    public String[] getBantrust(){
        if(obj.get("bantrust") != null) {
            String ban = (String) obj.get("bantrust");
            return ban.split(",");
        } else {
            return new String[0];
        }
    }

    public boolean isAntivpn(){
        return obj.get("antivpn") == null || (boolean) obj.get("antivpn");
    }

    public boolean isQuery(){
        return obj.get("query") != null || (boolean) obj.get("query");
    }

    public String getLanguage(){
        return obj.get("language") != null ? (String) obj.get("language") : "en";
    }

    public boolean isEnableantirush(){
        return obj.get("enableantirush") == null || (boolean) obj.get("enableantirush");
    }

    public Calendar getAntirushtime() {
        SimpleDateFormat format = new SimpleDateFormat("mm.ss");
        Calendar cal = Calendar.getInstance();
        if(obj.get("antirushtime") != null) {
            try {
                Date d = format.parse(String.valueOf(obj.get("antirushtime")));
                cal.setTime(d);
            } catch (Exception e) {
                printStackTrace(e);
                Global.normal("Invalid settings! - antirushtime");
                Global.normal("Correct value format is mm.ss (Example - 10.00 -> 10minute, 00.30 -> 30seconds)");
            }
        } else {
            try{
                Date d = format.parse("10.00");
                cal.setTime(d);
            } catch (Exception e){
                printStackTrace(e);
            }
        }
        return cal;
    }

    public boolean isAntigrief(){
        return obj.get("antigrief") == null && (boolean) obj.get("antigrief");
    }

    public boolean isBlockdetect(){
        return obj.get("blockdetect") != null && (boolean) obj.get("blockdetect");
    }

    public boolean isAlertdeposit(){
        return obj.get("alertdeposit") != null && (boolean) obj.get("alertdeposit");
    }

    public boolean isExplimit(){
        return obj.get("explimit") != null && (boolean) obj.get("explimit");
    }

    public boolean isLogging(){
        return obj.get("logging") != null && (boolean) obj.get("logging");
    }

    public boolean isUpdate(){
        return obj.get("update") != null || (boolean) obj.get("update");
    }

    public boolean isLevelupalarm(){
        return obj.get("levelupalarm") != null && (boolean) obj.get("levelupalarm");
    }

    public int getAlarmlevel(){
        return obj.get("alarm-minimal-level") != null ? (int) obj.get("alarm-minimal-level") : 20;
    }

    public boolean isSqlite(){
        return obj.get("sqlite") == null || (boolean) obj.get("sqlite");
    }

    public boolean isLoginenable(){
        return obj.get("loginenable") == null || (boolean) obj.get("loginenable");
    }

    public String getPasswordmethod(){
        return obj.get("loginmethod") != null ? (String) obj.get("loginmethod") : "password";
    }

    public boolean isValidconnect(){
        return obj.get("validconnect") == null || (boolean) obj.get("validconnect");
    }

    public String getDBurl(){
        if(isSqlite()){
            return "jdbc:sqlite:"+Core.settings.getDataDirectory().child("mods/Essentials/data/player.sqlite3");
        } else {
            return obj.get("dburl") != null ? (String) obj.get("dburl") : "";
        }
    }

    public String getDBid(){
        return obj.get("dbid") != null ? (String) obj.get("dbid") : "";
    }

    public String getDBpw(){
        return obj.get("dbpw") != null ? (String) obj.get("dbpw") : "";
    }

    public String getClientId(){
        return obj.get("clientId") != null ? (String) obj.get("clientId") : "";
    }

    public String getClientSecret(){
        return obj.get("clientSecret") != null ? (String) obj.get("clientSecret") : "";
    }

    public boolean isDebug(){
        return obj.get("debug") != null && (boolean) obj.get("debug");
    }

    public int getSavetime(){
        return obj.get("savetime") != null ? (int) obj.get("savetime") : 10;
    }

    public int getSlotnumber(){
        return obj.get("slotnumber") != null ? (int) obj.get("slotnumber") : 1000;
    }

    private int getVersion(){
        return obj.get("version") != null ? (int) obj.get("version") : 4;
    }

    public boolean isAutodifficulty(){
        return obj.get("auto-difficulty") != null && (boolean) obj.get("auto-difficulty");
    }

    public int getEasy(){
        return obj.get("easy") != null ? (int) obj.get("easy") : 2;
    }

    public int getNormal(){
        return obj.get("normal") != null ? (int) obj.get("normal") : 4;
    }

    public int getHard(){
        return obj.get("hard") != null ? (int) obj.get("hard") : 6;
    }

    public int getInsane(){
        return obj.get("insane") != null ? (int) obj.get("insane") : 10;
    }

    public int getSpawnlimit(){
        return obj.get("spawnlimit") != null ? (int) obj.get("spawnlimit") : 10;
    }

    public String getServername(){
        JSONObject data = new JSONObject(new JSONTokener(Core.settings.getDataDirectory().child("mods/Essentials/data/data.json").readString()));
        return data.getString("servername");
    }

    private void validfile(){
        final String path = "configs";
        final File jarFile = new File(getClass().getProtectionDomain().getCodeSource().getLocation().getPath());

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
                                if (name.contains(path + "/config_en.yml")) {
                                    if(!Core.settings.getDataDirectory().child("mods/Essentials/config.yml").exists()) {
                                        Core.settings.getDataDirectory().child("mods/Essentials/config.yml").write(reader, false);
                                    }
                                } else if (!name.contains(path + "/config_ko.yml")) {
                                    Core.settings.getDataDirectory().child("mods/Essentials/" + name.replaceFirst("configs/", "")).write(reader, false);
                                }
                            }
                        }
                    }
                }
                jar.close();

                Yaml yaml = new Yaml();
                obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/config.yml").readString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            // If folder name has space
            Global.normal("The plugin file or folder path is invalid. Please check your server path!");
            net.dispose();
            Core.app.exit();
        }
    }

    public String checkfeatures(){
        StringBuilder features = new StringBuilder();
        if(getLanguage().equals("ko")){
            features.append("활성화된 기능: ");
            if(isServerenable()) features.append("서버, ");
            if(isClientenable()) features.append("클라이언트, ");
            if(isRealname()) features.append("고정닉, ");
            if(isDetectreactor()) features.append("토륨 원자로 감지, ");
            if(isScanresource()) features.append("빠른 자원소모 감지, ");
            if(isExplimit()) features.append("경험치 제한");
            if(isBanshare()) features.append("밴 공유, ");
            if(isQuery()) features.append("요청기능, ");
            if(isAntivpn()) features.append("VPN 차단, ");
            if(isEnableantirush()) features.append("PvP 초반 러시 금지, ");
            if(isLogging()) features.append("로그 기능, ");
            if(isLoginenable()) features.append("로그인 기능, ");
            if(isAutodifficulty()) features.append("자동 난이도 설정, ");
            if(isAntigrief()) features.append("테러방지, ");
            if(isBlockdetect() || isAlertdeposit()) features.append("블록 활동 알림, ");
            if(isDebug()) features.append("디버그");
        } else {
            features.append("Enabled features: ");
            if(isServerenable()) features.append("Network Server, ");
            if(isClientenable()) features.append("Network Client, ");
            if(isRealname()) features.append("Realname, ");
            if(isDetectreactor()) features.append("Detect reactor, ");
            if(isScanresource()) features.append("Scan using resource, ");
            if(isExplimit()) features.append("Exp limit");
            if(isBanshare()) features.append("Ban sharing, ");
            if(isQuery()) features.append("Query, ");
            if(isAntivpn()) features.append("Anti-VPN, ");
            if(isEnableantirush()) features.append("PvP Anti-rush, ");
            if(isLogging()) features.append("Logging, ");
            if(isLoginenable()) features.append("Login, ");
            if(isAutodifficulty()) features.append("Auto difficulty, ");
            if(isAntigrief()) features.append("Anti grief, ");
            if(isBlockdetect() || isAlertdeposit()) features.append("Block activity alerm, ");
            if(isDebug()) features.append("Debug");
        }
        return features.toString().substring(0,features.length()-2);
    }

    public void main() {
        validfile();

        JSONObject data = new JSONObject(new JSONTokener(Core.settings.getDataDirectory().child("mods/Essentials/data/data.json").readString()));
        if(!data.has("servername")){
            data.put("servername", Core.settings.getString("servername"));
            Core.settings.getDataDirectory().child("mods/Essentials/data/data.json").writeString(data.toString());
        }

        String text;
        if (getLanguage().equals("ko")) {
            text = "# 플러그인 버전 (절대 수정하지 마세요!)\n" +
                    "version: 6\n" +
                    "\n" +
                    "# 플러그인 언어 (지원하는 언어 - ko, en)\n" +
                    "language: ko\n" +
                    "\n" +
                    "# 서버/클라이언트 포트 설정\n" +
                    "# 이것은 플러그인의 네트워크 기능에 사용됩니다.\n" +
                    "server-enable: " + isServerenable() + "\n" +
                    "server-port: " + getServerport() + "\n" +
                    "\n" +
                    "client-enable: " + isClientenable() + "\n" +
                    "client-port: " + getClientport() + "\n" +
                    "client-host: " + getClienthost() + "\n" +
                    "\n" +
                    "# realname를 켜면 플레이어가 닉네임을 변경하더라도 이전 닉네임으로 설정됩니다.\n" +
                    "# 컬러닉 기능을 원하는 경우 이것을 활성화해야 합니다.\n" +
                    "realname: " + isRealname() + "\n" +
                    "\n" +
                    "# 컬러닉 갱신 시간설정. 1초 = 1000\n" +
                    "colornick update interval: " + getCupdatei() + "\n" +
                    "\n" +
                    "# 원자로 감지를 켜면 토륨 원자로가 과열되어 폭발 하기 직전일 때 즉시 블럭이 파괴됩니다.\n" +
                    "detectreactor: " + isDetectreactor() + "\n" +
                    "\n" +
                    "# 빠른 자원소모 감지를 켜면 한 자원이 매우 빠르게 소모가 되고 있을때, 그 자원을 사용하고 있는 플레이어의 명단을 띄워줍니다.\n" +
                    "scanresource: " + isScanresource() + "\n" +
                    "\n" +
                    "# 이 기능을 켜면 각 플레이어의 블럭 설치/파괴에 대한 테러방지 기능이 작동합니다.\n" +
                    "# blockdetect 는 블록의 설정을 바꿨을 때 메세지로 알려주는 기능입니다.\n" +
                    "# alertdeposit 는 아이템을 직접 집어서 다른 블록에 넣었을 때 메세지로 알려주는 기능입니다.\n" +
                    "antigrief: "+ isAntigrief() + "\n" +
                    "blockdetect: "+ isBlockdetect() + "\n" +
                    "alertdeposit: " + isAlertdeposit() + "\n" +
                    "\n" +
                    "# 경험치 값 설정.\n" +
                    "# explimit를 켜면, 플레이어가 건설하려는 블록 요구 레벨이 되지 않을경우 건설 자체를 취소시킵니다.\n" +
                    "# Base xp는 레벨 1에서 2로 오르는데 필요한 경험치 수치입니다.\n" +
                    "# exponent는 다음 레벨로 올리기 위한 요구 경험치 배수입니다.\n" +
                    "# levelupalarm 를 활성화 하면 일정레벨 이상에서 레벨이 오를때, 메세지로 띄워줍니다.\n" +
                    "explimit: " + isExplimit() + "\n" +
                    "basexp: " + getBasexp() + "\n" +
                    "exponent: " + getExponent() + "\n" +
                    "levelupalarm: " + isLevelupalarm() + "\n" +
                    "alarm-minimal-level: " + getAlarmlevel() + "\n" +
                    "\n" +
                    "# 밴 공유서버 설정\n" +
                    "# 이 기능을 켜면, 다른 공용 서버와 밴 목록을 공유하게 됩니다.\n" +
                    "banshare: " + isBanshare() + "\n" +
                    "\n" +
                    "# 신뢰가능한 밴 공유 IP 설정\n" +
                    "# 예시 - 127.0.0.1,localhost,192.168.0.0\n" +
                    "bantrust: " + obj.get("bantrust") + "\n" +
                    "\n" +
                    "# 서버 요청 설정\n" +
                    "# 이 기능을 켜면 서버 포트에서 서버 정보를 얻어올 수 있게 됩니다.\n" +
                    "# 랭킹 사이트는 http://localhost:서버포트/rank/kr 으로 들어가면 됩니다.\n" +
                    "query: " + isQuery() + "\n" +
                    "\n" +
                    "# 이 기능을 켜면 VPN 서비스가 켜집니다.\n" +
                    "antivpn: " + isAntivpn() + "\n" +
                    "\n" +
                    "# 이 기능을 켜면 PvP 초반 러시 방지기능을 활성화 합니다. 시간 단위: 1초\n" +
                    "enableantirush: " + isEnableantirush() + "\n" +
                    "antirushtime: " + obj.get("antirushtime") + "\n" +
                    "\n" +
                    "# 서버 로그 활성화 (이 기능을 켜면 많은 디스크 작업이 일어납니다!)\n" +
                    "logging: " + isLogging() + "\n" +
                    "\n" +
                    "# 플러그인 업데이트 확인 기능\n" +
                    "update: " + isUpdate() + "\n" +
                    "\n" +
                    "# 데이터베이스 종류 설정 (Default is SQLite)\n" +
                    "# 예시 - jdbc:mariadb://localhost:3306/DB이름\n" +
                    "# 만약 MySQL/MariaDB 를 사용하고 싶다면, SQLite를 비활성화 하고 새 데이터베이스를 직접 만드셔야 합니다!\n" +
                    "sqlite: " + isSqlite() + "\n" +
                    "dburl: " + getDBurl() + "\n" +
                    "dbid: " + getDBid() + "\n" +
                    "dbpw: " + getDBpw() + "\n" +
                    "\n" +
                    "# 로그인 기능 설정\n" +
                    "# loginmethod 는 계정 등록방식 입니다. (password, email 방식이 가능함)\n" +
                    "# validconnect 는 해당 플레이어가 서버 이미 있을경우 강퇴하는 기능입니다.\n" +
                    "loginenable: " + isLoginenable() + "\n" +
                    "loginmethod: " + getPasswordmethod() + "\n" +
                    "validconnect: " + isValidconnect() + "\n" +
                    "\n" +
                    "# 파파고 번역 API 키\n" +
                    "# 이 키는 developers.naver.com 에서 유료로 얻을 수 있습니다.\n" +
                    "clientId: " + getClientId() + "\n" +
                    "clientSecret: " + getClientSecret() + "\n" +
                    "\n" +
                    "# 이 기능을 켜면 오류 메세지가 저장되지 않고 즉시 콘솔로 출력됩니다.\n" +
                    "debug: " + isDebug() + "\n" +
                    "\n" +
                    "# 맵 자동저장 시간. 시간 단위는 1분입니다.\n" +
                    "savetime: " + getSavetime() + "\n" +
                    "\n" +
                    "# 빽섭할 맵 저장 슬롯\n" +
                    "# 예시 - 만약 값을 1000으로 설정한다면, 빽섭할 맵의 파일명이 1000.msav 으로 저장됩니다.\n" +
                    "slotnumber: " + getSlotnumber() + "\n" +
                    "\n" +
                    "# 플레이어 인원별 난이도 조절\n" +
                    "auto-difficulty: false\n" +
                    "easy: "+getEasy()+"\n" +
                    "normal: "+getNormal()+"\n" +
                    "hard: "+getHard()+"\n" +
                    "insane: "+getInsane()+"\n" +
                    "\n" +
                    "# spawn 명령어 유닛 개수 제한\n" +
                    "spawnlimit: "+getSpawnlimit();
        } else {
            text = "# Config version (Don't touch this!)\n" +
                    "version: 6\n" +
                    "\n" +
                    "# Plugin language (Supported language - ko, en)\n" +
                    "language: en\n" +
                    "\n" +
                    "# Server/client port settings\n" +
                    "# This's used for the network function of the plugin.\n" +
                    "server-enable: " + isServerenable() + "\n" +
                    "server-port: " + getServerport() + "\n" +
                    "\n" +
                    "client-enable: " + isClientenable() + "\n" +
                    "client-port: " + getClientport() + "\n" +
                    "client-host: " + getClienthost() + "\n" +
                    "\n" +
                    "# If turn on realname, even if the player changes the nickname, it will be set to the previous nickname.\n" +
                    "# If you want colornick features, must enable this.\n" +
                    "realname: " + isRealname() + "\n" +
                    "\n" +
                    "# Color nickname update interval. 1sec = 1000\n" +
                    "colornick update interval: " + getCupdatei() + "\n" +
                    "\n" +
                    "# If turn on detectreactor, destory reactor when the thorium reactor is overheated.\n" +
                    "detectreactor: " + isDetectreactor() + "\n" +
                    "\n" +
                    "# if turn on scanresource, show message a list of players who are using a resource when it is consuming a resource very quickly.\n" +
                    "scanresource: " + isScanresource() + "\n" +
                    "\n" +
                    "# Turn this feature on to activate anti-grief against block place/break of each player.\n" +
                    "# blockdetect is a function that notifies players when a any block config is changed.\n" +
                    "# alertdeposit is a feature that notifies you when an item is picked up and put into another block.\n" +
                    "antigrief: "+ isAntigrief() + "\n" +
                    "blockdetect: "+ isBlockdetect() +"\n" +
                    "alertdeposit: " + isAlertdeposit() + "\n" +
                    "\n" +
                    "# Experience value setting.\n" +
                    "# When turn on explimit, cancels the construction itself if the player doesn't reach the level of the block they are trying to build.\n" +
                    "# Base xp is required experience to level up from 1 to 2\n" +
                    "# exponent is EXP multiplier required for the next level.\n" +
                    "# When turn on levelupalarm, a message is displayed when the level rises above a certain level.\n" +
                    "explimit: " + isExplimit() + "\n" +
                    "basexp: " + getBasexp() + "\n" +
                    "exponent: " + getExponent() + "\n" +
                    "levelupalarm: " + isLevelupalarm() + "\n" +
                    "alarm-minimal-level: " + getAlarmlevel() + "\n" +
                    "\n" +
                    "# Ban sharing server config\n" +
                    "# If you enable this, your ban list will send to another public servers.\n" +
                    "banshare: " + isBanshare() + "\n" +
                    "\n" +
                    "# Ban sharing trust list\n" +
                    "# Example - 127.0.0.1,localhost,192.168.0.0\n" +
                    "bantrust: " + obj.get("bantrust") + "\n" +
                    "\n" +
                    "# Server query config\n" +
                    "# If you enable this, You will be able to get server information from the server port.\n" +
                    "# Ranking page address is http://localhost:server_port/rank\n" +
                    "query: " + isQuery() + "\n" +
                    "\n" +
                    "# Enable Anti-VPN service.\n" +
                    "antivpn: " + isAntivpn() + "\n" +
                    "\n" +
                    "# Enable Anti PvP early time rushing. Time unit: 1 second\n" +
                    "enableantirush: " + isEnableantirush() + "\n" +
                    "antirushtime: " + obj.get("antirushtime") + "\n" +
                    "\n" +
                    "# Logging enable (This features may take heavy disk read/write work!)\n" +
                    "logging: " + isLogging() + "\n" +
                    "\n" +
                    "# update check enable\n" +
                    "update: " + isUpdate() + "\n" +
                    "\n" +
                    "# Database type setting (Default is SQLite)\n" +
                    "# Example - jdbc:mariadb://localhost:3306/dbname\n" +
                    "# If you want to use MySQL/MariaDB, You must disable sqlite and create a new database yourself.\n" +
                    "sqlite: " + isSqlite() + "\n" +
                    "dburl: " + getDBurl() + "\n" +
                    "dbid: " + getDBid() + "\n" +
                    "dbpw: " + getDBpw() + "\n" +
                    "\n" +
                    "# Login features setting\n" +
                    "# loginmethod is account register method. (password, email method avaliable.)\n" +
                    "# validconnect is a feature that kick if the player already exists on the server.\n" +
                    "loginenable: " + isLoginenable() + "\n" +
                    "loginmethod: " + getPasswordmethod() + "\n" +
                    "validconnect: " + isValidconnect() + "\n" +
                    "\n" +
                    "# Papago translate API Key\n" +
                    "# The api key can be obtained from developers.naver.com.\n" +
                    "clientId: " + getClientId() + "\n" +
                    "clientSecret: " + getClientSecret() + "\n" +
                    "\n" +
                    "# The error message is output immediately.\n" +
                    "debug: " + isDebug() + "\n" +
                    "\n" +
                    "# Map auto save time. Time unit: 1 minute\n" +
                    "savetime: " + getSavetime() + "\n" +
                    "\n" +
                    "# Rollback map save slot number.\n" +
                    "# Example - if set value to 1000, rollback map name will renamed to 1000.msav\n" +
                    "slotnumber: " + getSlotnumber() + "\n" +
                    "\n" +
                    "# Set the difficulty per player\n" +
                    "auto-difficulty: false\n" +
                    "easy: "+getEasy()+"\n" +
                    "normal: "+getNormal()+"\n" +
                    "hard: "+getHard()+"\n" +
                    "insane: "+getInsane()+"\n" +
                    "\n" +
                    "# Set spawn command unit limit\n" +
                    "spawnlimit: "+getSpawnlimit();
        }
        Core.settings.getDataDirectory().child("mods/Essentials/config.yml").writeString(text);

        Global.config("config-loaded");
        if(getVersion() < 6){
            Global.config("config-updated");
        }

        // 서버간 이동 타일 불러오기
        jumpzone = new JSONArray(new JSONTokener(Core.settings.getDataDirectory().child("mods/Essentials/data/jumpdata.json").readString()));
        jumpcount = new JSONArray(new JSONTokener(Core.settings.getDataDirectory().child("mods/Essentials/data/jumpcount.json").readString()));
        jumpall = new JSONArray(new JSONTokener(Core.settings.getDataDirectory().child("mods/Essentials/data/jumpall.json").readString()));
    }
}