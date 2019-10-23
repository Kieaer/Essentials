package essentials;

import io.anuke.arc.Core;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static essentials.Global.printStackTrace;

public class EssentialConfig {
    public static String clienthost;
    public static int clientport;
    public int serverport;
    public boolean realname;
    public boolean detectreactor;
    public boolean serverenable;
    public boolean clientenable;
    public static double basexp;
    public static double exponent;
    public static int cupdatei;
    public boolean banshare;
    public boolean antivpn;
    public boolean query;
    private int version;
    private String language;
    public static boolean enableantirush;
    public static Calendar antirushtime;
    public boolean explimit;
    public boolean logging;
    public boolean update;
    static boolean levelupalarm;
    public static boolean sqlite;
    private String dburl;
    public boolean loginenable;
    public static String url;
    public static String dbid;
    public static String dbpw;
    public String apikey;
    public boolean debug;

    static ExecutorService executorService = Executors.newFixedThreadPool(6);

    public void main() {
        Map<String, Object> obj;
        if (!Core.settings.getDataDirectory().child("mods/Essentials/config.txt").exists()) {
            String text = "# Config version (Don't touch this!)\n" +
                    "version: 2\n\n" +

                    "# Plugin language\n" +
                    "language: en\n\n" +

                    "# Server / client port settings\n#It will enable server as server chat function.\n" +
                    "server-enable: false\n" +
                    "server-port: 25000\n\n" +

                    "client-enable: false\n" +
                    "client-port: 20000\n" +
                    "client-host: mindustry.kr\n\n" +

                    "# If turn on realname, even if the player changes the nickname, it will be set to the previous nickname.\n" +
                    "# If you want colornick features, must enable this.\n" +
                    "realname: true\n\n" +

                    "# Color nickname update interval. 1sec = 1000\n" +
                    "colornick update interval: 1000\n\n" +

                    "# If turn on detectreactor, send alert message when the thorium reactor is overheated and explodes.\n" +
                    "detectreactor: true\n\n" +

                    "# Experience value setting.\n# Base xp is required experience to level up from 1 to 2\n# exponent is EXP multiplier required for the next level.\n" +
                    "explimit: false\n" +
                    "basexp: 500\n" +
                    "exponent: 1.12f\n" +
                    "levelupalarm: false\n\n" +

                    "# Ban sharing server config\n# If you enable this, your ban list will send to another public servers.\n" +
                    "banshare: false\n\n" +

                    "# Server query config\n# If you enable this, You will be able to get server information from the server port.\n# Ranking page address is http://localhost:server_port/rank\n" +
                    "query: false\n\n" +

                    "# Enable Anti-VPN service.\n" +
                    "antivpn: true\n\n" +

                    "# Enable Anti PvP early time rushing. Time unit: 1 second\n" +
                    "enableantirush: true\n" +
                    "antirushtime: 10.00\n\n"+
                    
                    "# Logging enable\n" +
                    "logging: false\n\n" +

                    "# update check enable\n" +
                    "update: true\n\n" +

                    "# Database type setting (Default is SQLite)\n# Example - mariadb://localhost:3306/dbname\n# If you want to use MySQL/MariaDB, You must create a new database yourself.\n# dburl\n" +
                    "sqlite: true\n" +
                    "dburl: \n" +
                    "dbid: \n" +
                    "dbpw: \n\n" +

                    "# Login features setting\n" +
                    "loginenable: true\n\n" +

                    "# Google translate API Key\n" +
                    "# The api key can be obtained from cloud.google.com.\n" +
                    "apikey: \n\n" +

                    "# The error message is output immediately.\n" +
                    "debug: false";

            Core.settings.getDataDirectory().child("mods/Essentials/config.txt").writeString(text);
            Global.log("config file created!");
        }

        if (!Core.settings.getDataDirectory().child("mods/Essentials/Exp.txt").exists()) {
            String text = "# crafting\n" +
                    "graphite-press: 5\n" +
                    "multi-press: 15\n" +
                    "silicon-smelter: 5\n" +
                    "kiln: 8\n" +
                    "plastanium-compressor: 20\n" +
                    "phase-weaver: 35\n" +
                    "alloy-smelter: 30\n" +
                    "cryofluidmixer: 15\n" +
                    "blast-mixer: 8\n" +
                    "pyratite-mixer: 12\n" +
                    "melter: 3\n" +
                    "separator: 6\n" +
                    "spore-press: 5\n" +
                    "pulverizer: 2\n" +
                    "coal-centrifuge: 3\n" +
                    "incinerator: 2\n\n" +

                    "# Sandbox\n" +
                    "power-void: 0\n" +
                    "power-source: 0\n" +
                    "item-source: 0\n" +
                    "item-void: 0\n" +
                    "liquid-source: 0\n" +
                    "message: 0\n\n" +

                    "# Defence\n" +
                    "scrap-wall: 0\n" +
                    "scrap-wall-large: 0\n" +
                    "scrap-wall-huge: 0\n" +
                    "scrap-wall-gigantic: 0\n" +
                    "thruster: 0\n" +
                    "copper-wall: 1\n" +
                    "copper-wall-large: 4\n" +
                    "titanium-wall: 2\n" +
                    "titanium-wall-large: 8\n" +
                    "thorium-wall: 3\n" +
                    "thorium-wall-large: 12\n" +
                    "phase-wall: 4\n" +
                    "phase-wall-large: 16\n" +
                    "surge-wall: 5\n" +
                    "surge-wall-large: 20\n" +
                    "door: 2\n" +
                    "door-large: 6\n" +
                    "mender: 2\n" +
                    "mend-projector: 10\n" +
                    "overdrive-projector: 12\n" +
                    "force-projector: 35\n" +
                    "shock-mine: 5\n\n" +

                    "# Distribution\n" +
                    "conveyor: 1\n" +
                    "titanium-conveyor: 2\n" +
                    "armored-conveyor: 3\n" +
                    "junction: 1\n" +
                    "bridge-conveyor: 1\n" +
                    "phase-conveyor: 5\n" +
                    "sorter: 2\n" +
                    "router: 1\n" +
                    "distributor: 1\n" +
                    "overflow-gate: 2\n" +
                    "mass-driver: 15\n\n" +

                    "# Liquid\n" +
                    "mechanical-pump: 2\n" +
                    "rotary-pump: 8\n" +
                    "thermal-pump: 15\n" +
                    "conduit: 2\n" +
                    "pulse-conduit: 4\n" +
                    "liquid-router: 3\n" +
                    "liquid-tank: 10\n" +
                    "liquid-junction: 2\n" +
                    "bridge-conduit: 2\n" +
                    "phase-conduit: 5\n\n" +

                    "# Power\n" +
                    "power-node: 1\n" +
                    "power-node-large: 3\n" +
                    "surge-tower: 10\n" +
                    "battery: 3\n" +
                    "battery-large: 21\n" +
                    "combustion-generator: 5\n" +
                    "thermal-generator: 8\n" +
                    "turbine-generator: 12\n" +
                    "differential-generator: 20\n" +
                    "rtg-generator: 15\n" +
                    "solar-panel: 6\n" +
                    "solar-panel-large: 18\n" +
                    "thorium-reactor: 25\n" +
                    "impact-reactor: 200\n\n" +

                    "# Production\n" +
                    "mechanical-drill: 2\n" +
                    "pneumatic-drill: 5\n" +
                    "laser-drill: 10\n" +
                    "blast-drill: 15\n" +
                    "water-extractor: 3\n" +
                    "cultivator: 7\n" +
                    "oil-extractor: 15\n\n" +

                    "# Storage\n" +
                    "vault: 20\n" +
                    "container: 7\n" +
                    "unloader: 3\n\n" +

                    "# Turrets\n" +
                    "duo: 3\n" +
                    "scatter: 5\n" +
                    "scorch: 8\n" +
                    "hail: 10\n" +
                    "wave: 12\n" +
                    "lancer: 15\n" +
                    "arc: 18\n" +
                    "swarmer: 30\n" +
                    "salvo: 24\n" +
                    "fuse: 40\n" +
                    "ripple: 50\n" +
                    "cyclone: 60\n" +
                    "spectre: 100\n" +
                    "meltdown: 250\n\n" +

                    "# Units\n" +
                    "draug-factory: 8\n" +
                    "spirit-factory: 12\n" +
                    "phantom-factory: 18\n" +
                    "command-center: 30\n" +
                    "wraith-factory: 15\n" +
                    "ghoul-factory: 25\n" +
                    "revenant-factory: 40\n" +
                    "dagger-factory: 6\n" +
                    "crawler-factory: 8\n" +
                    "titan-factory: 12\n" +
                    "fortress-factory: 18\n" +
                    "repair-point: 2\n\n" +

                    "# Upgrade\n" +
                    "dart-mech-pad: 15\n" +
                    "delta-mech-pad: 30\n" +
                    "tau-mech-pad: 40\n" +
                    "omega-mech-pad: 150\n" +
                    "javelin-ship-pad: 35\n" +
                    "trident-ship-pad: 45\n" +
                    "glaive-ship-pad: 150";

            Core.settings.getDataDirectory().child("mods/Essentials/Exp.txt").writeString(text);
            Global.log("Exp config file created!");
        }

        if (!Core.settings.getDataDirectory().child("mods/Essentials/BlockReqExp.txt").exists()) {
            String text = "# crafting\n" +
                    "graphite-press: 3\n" +
                    "multi-press: 15\n" +
                    "silicon-smelter: 6\n" +
                    "kiln: 9\n" +
                    "plastanium-compressor: 16\n" +
                    "phase-weaver: 23\n" +
                    "alloy-smelter: 22\n" +
                    "cryofluidmixer: 22\n" +
                    "blast-mixer: 15\n" +
                    "pyratite-mixer: 6\n" +
                    "melter: 14\n" +
                    "separator: 18\n" +
                    "spore-press: 9\n" +
                    "pulverizer: 25\n" +
                    "coal-centrifuge: 13\n" +
                    "incinerator: 10\n\n" +

                    "# Sandbox\n" +
                    "power-void: 1\n" +
                    "power-source: 1\n" +
                    "item-source: 1\n" +
                    "item-void: 1\n" +
                    "liquid-source: 1\n" +
                    "message: 5\n\n" +

                    "# Defence\n" +
                    "scrap-wall: \1n" +
                    "scrap-wall-large: 1\n" +
                    "scrap-wall-huge: 1\n" +
                    "scrap-wall-gigantic: 1\n" +
                    "thruster: 1\n" +
                    "copper-wall: 3\n" +
                    "copper-wall-large: 6\n" +
                    "titanium-wall: 6\n" +
                    "titanium-wall-large: 8\n" +
                    "thorium-wall: 8\n" +
                    "thorium-wall-large: 12\n" +
                    "phase-wall: 16\n" +
                    "phase-wall-large: 20\n" +
                    "surge-wall: 12\n" +
                    "surge-wall-large: 16\n" +
                    "door: 9\n" +
                    "door-large: 12\n" +
                    "mender: 8\n" +
                    "mend-projector: 16\n" +
                    "overdrive-projector: 27\n" +
                    "force-projector: 20\n" +
                    "shock-mine: 16\n\n" +

                    "# Distribution\n" +
                    "conveyor: 1\n" +
                    "titanium-conveyor: 7\n" +
                    "armored-conveyor: 15\n" +
                    "junction: 1\n" +
                    "bridge-conveyor: 5\n" +
                    "phase-conveyor: 20\n" +
                    "sorter: 5\n" +
                    "router: 3\n" +
                    "distributor: 5\n" +
                    "overflow-gate: 7\n" +
                    "mass-driver: 30\n\n" +

                    "# Liquid\n" +
                    "mechanical-pump: 3\n" +
                    "rotary-pump: 17\n" +
                    "thermal-pump: 23\n" +
                    "conduit: 9\n" +
                    "pulse-conduit: 17\n" +
                    "liquid-router: 14\n" +
                    "liquid-tank: 17\n" +
                    "liquid-junction: 11\n" +
                    "bridge-conduit: 14\n" +
                    "phase-conduit: 23\n\n" +

                    "# Power\n" +
                    "power-node: 3\n" +
                    "power-node-large: 7\n" +
                    "surge-tower: 20\n" +
                    "battery: 5\n" +
                    "battery-large: 15\n" +
                    "combustion-generator: 6\n" +
                    "thermal-generator: 16\n" +
                    "turbine-generator: 12\n" +
                    "differential-generator: 23\n" +
                    "rtg-generator: 40\n" +
                    "solar-panel: 7\n" +
                    "solar-panel-large: 11\n" +
                    "thorium-reactor: 30\n" +
                    "impact-reactor: 50\n\n" +

                    "# Production\n" +
                    "mechanical-drill: 1\n" +
                    "pneumatic-drill: 6\n" +
                    "laser-drill: 12\n" +
                    "blast-drill: 16\n" +
                    "water-extractor: 16\n" +
                    "cultivator: 12\n" +
                    "oil-extractor: 20\n\n" +

                    "# Storage\n" +
                    "vault: 23\n" +
                    "container: 10\n" +
                    "unloader: 13\n\n" +

                    "# Turrets\n" +
                    "duo: 1\n" +
                    "scatter: 3\n" +
                    "scorch: 5\n" +
                    "hail: 8\n" +
                    "wave: 13\n" +
                    "lancer: 15\n" +
                    "arc: 10\n" +
                    "swarmer: 28\n" +
                    "salvo: 12\n" +
                    "fuse: 25\n" +
                    "ripple: 20\n" +
                    "cyclone: 30\n" +
                    "spectre: 40\n" +
                    "meltdown: 50\n\n" +

                    "# Units\n" +
                    "draug-factory: 6\n" +
                    "spirit-factory: 10\n" +
                    "phantom-factory: 16\n" +
                    "command-center: 14\n" +
                    "wraith-factory: 17\n" +
                    "ghoul-factory: 20\n" +
                    "revenant-factory: 25\n" +
                    "dagger-factory: 8\n" +
                    "crawler-factory: 13\n" +
                    "titan-factory: 22\n" +
                    "fortress-factory: 25\n" +
                    "repair-point: 20\n\n" +

                    "# Upgrade\n" +
                    "dart-mech-pad: 10\n" +
                    "delta-mech-pad: 15\n" +
                    "tau-mech-pad: 20\n" +
                    "omega-mech-pad: 25\n" +
                    "javelin-ship-pad: 20\n" +
                    "trident-ship-pad: 25\n" +
                    "glaive-ship-pad: 30\n";
            Core.settings.getDataDirectory().child("mods/Essentials/BlockReqExp.txt").writeString(text);
            Global.log("BlockReqExp config file created!");
        }

        if (!Core.settings.getDataDirectory().child("mods/Essentials/data.json").exists()) {
            JSONObject object = new JSONObject();
            object.put("banall", "true");
            Core.settings.getDataDirectory().child("mods/Essentials/data.json").writeString(String.valueOf(object));
        }

        if(!Core.settings.getDataDirectory().child("mods/Essentials/banned.json").exists()){
            JSONArray ban = new JSONArray();
            String json = ban.toString();
            Core.settings.getDataDirectory().child("mods/Essentials/banned.json").writeString(json);
            Global.log("banned file created!");
        }

        if(!Core.settings.getDataDirectory().child("mods/Essentials/motd.txt").exists()){
            String msg = "To edit this message, modify the [green]motd.txt[] file in the [green]config/mods/Essentials/[] folder.";
            Core.settings.getDataDirectory().child("mods/Essentials/motd.txt").writeString(msg);
            Global.log("motd file created.");
        }

        if(!Core.settings.getDataDirectory().child("mods/Essentials/motd_ko.txt").exists()){
            String msg = "이 메시지를 바꿀려면 [green]config/mods/Essentials/[] 폴더에서 [green]motd.txt[] 파일을 수정하세요.";
            Core.settings.getDataDirectory().child("mods/Essentials/motd_ko.txt").writeString(msg);
            Global.log("motd_ko file created.");
        }

        if(!Core.settings.getDataDirectory().child("mods/Essentials/blacklist.json").exists()){
            JSONArray blacklist = new JSONArray();
            String json = blacklist.toString();
            Core.settings.getDataDirectory().child("mods/Essentials/blacklist.json").writeString(json);
            Global.log("blacklist file created!");
        }

        if(!Core.settings.getDataDirectory().child("mods/Essentials/powerblock.json").exists()){
            Core.settings.getDataDirectory().child("mods/Essentials/powerblock.json").writeString("[]");
            Global.log("powerblock file created!");
        }

        // EssentialsLog
        if (!Core.settings.getDataDirectory().child("mods/Essentials/Logs/error.log").exists()) {
            Core.settings.getDataDirectory().child("mods/Essentials/Logs/error.log").writeString("");
            Global.log("error.log created.");
        }

        if (Core.settings.getDataDirectory().child("mods/Essentials/config.txt").exists()){
            Yaml yaml = new Yaml();
            obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/config.txt").readString()));
            // Config version
            if(obj.get("version") != null){
                version = Integer.parseInt(String.valueOf(obj.get("version")));
            } else {
                version = 3;
            }

            if(obj.get("language") != null){
                language = (String) obj.get("language");
            } else {
                language = "en";
            }

            if(obj.get("server-enable") != null){
                serverenable = Boolean.parseBoolean(String.valueOf(obj.get("server-enable")));
            } else {
                serverenable = false;
            }
            if(obj.get("server-port") != null){
                serverport = Integer.parseInt(String.valueOf(obj.get("server-port")));
            } else {
                serverport = 25000;
            }

            if(obj.get("client-enable") != null){
                clientenable = Boolean.parseBoolean(String.valueOf(obj.get("client-enable")));
            } else {
                clientenable = false;
            }
            if(obj.get("client-port") != null){
                clientport = Integer.parseInt(String.valueOf(obj.get("client-port")));
            } else {
                clientport = 20000;
            }
            if(obj.get("client-enable") != null){
                clienthost = (String) obj.get("client-host");
            } else {
                clienthost = "mindustry.kr";
            }

            if(obj.get("realname") != null){
                realname = Boolean.parseBoolean(String.valueOf(obj.get("realname")));
            } else {
                realname = true;
            }

            if(obj.get("colornick update interval") != null){
                cupdatei = Integer.parseInt(String.valueOf(obj.get("colornick update interval")));
            } else {
                cupdatei = 1000;
            }

            if(obj.get("detectreactor") != null){
                detectreactor = Boolean.parseBoolean(String.valueOf(obj.get("detectreactor")));
            } else {
                detectreactor = true;
            }

            if(obj.get("explimit") != null){
                explimit = Boolean.parseBoolean(String.valueOf(obj.get("explimit")));
            } else {
                explimit = false;
            }
            if(obj.get("basexp") != null){
                basexp = Double.parseDouble(String.valueOf(obj.get("basexp")));
            } else {
                basexp = 500;
            }
            if(obj.get("exponent") != null){
                exponent = Double.parseDouble(String.valueOf(obj.get("exponent")));
            } else {
                exponent = 1.12f;
            }
            if(obj.get("levelupalarm") != null){
                levelupalarm = Boolean.parseBoolean(String.valueOf(obj.get("levelupalarm")));
            } else {
                levelupalarm = false;
            }

            if(obj.get("banshare") != null){
                banshare = Boolean.parseBoolean(String.valueOf(obj.get("banshare")));
            } else {
                banshare = false;
            }

            if(obj.get("query") != null){
                query = Boolean.parseBoolean(String.valueOf(obj.get("query")));
            } else {
                query = false;
            }

            if(obj.get("antivpn") != null){
                antivpn = Boolean.parseBoolean(String.valueOf(obj.get("antivpn")));
            } else {
                antivpn = true;
            }

            if(obj.get("enableantirush") != null){
                enableantirush = Boolean.parseBoolean(String.valueOf(obj.get("enableantirush")));
            } else {
                enableantirush = true;
            }
            if(obj.get("antirushtime") != null){
                try{
                    SimpleDateFormat format = new SimpleDateFormat("mm.ss");
                    Calendar cal;
                    Date d = format.parse(String.valueOf(obj.get("antirushtime")));
                    cal = Calendar.getInstance();
                    cal.setTime(d);
                    antirushtime = cal;
                } catch (Exception e){
                    printStackTrace(e);
                    Global.loge("Invalid settings! - antirushtime");
                    Global.loge("Correct value format is mm.ss (Example - 10.00 -> 10minute, 00.30 -> 30seconds)");
                }
            } else {
                try{
                    SimpleDateFormat format = new SimpleDateFormat("mm.ss");
                    Calendar cal;
                    Date d = format.parse("05.00");
                    cal = Calendar.getInstance();
                    cal.setTime(d);
                    antirushtime = cal;
                } catch (Exception e){
                    printStackTrace(e);
                }
            }

            if(obj.get("update") != null){
                update = Boolean.parseBoolean(String.valueOf(obj.get("update")));
            } else {
                update = true;
            }

            if(obj.get("logging") != null){
                logging = Boolean.parseBoolean(String.valueOf(obj.get("logging")));
            } else {
                logging = false;
            }

            if(obj.get("sqlite") != null){
                sqlite = Boolean.parseBoolean(String.valueOf(obj.get("sqlite")));
                if(sqlite){
                    url = "jdbc:sqlite:"+Core.settings.getDataDirectory().child("mods/Essentials/player.sqlite3");
                } else {
                    dburl = (String) obj.get("dburl");
                    url = "jdbc:"+dburl;
                }
            } else {
                sqlite = true;
                url = "jdbc:sqlite:" + Core.settings.getDataDirectory().child("mods/Essentials/player.sqlite3");
            }

            if(obj.get("dbid") != null){
                dbid = (String) obj.get("dbid");
            } else {
                dbid = "";
            }

            if(obj.get("dbpw") != null){
                dbpw = (String) obj.get("dbpw");
            } else {
                dbpw = "";
            }

            if(obj.get("loginenable") != null){
                loginenable = Boolean.parseBoolean(String.valueOf(obj.get("loginenable")));
            } else {
                loginenable = true;
            }

            if(obj.get("apikey") != null){
                apikey = (String) obj.get("apikey");
            } else {
                apikey = "";
            }

            if(obj.get("debug") != null){
                debug = Boolean.parseBoolean(String.valueOf(obj.get("debug")));
            } else {
                debug = false;
            }
            Global.log("config file loaded!");
        }

        if(version < 2){
            Yaml yaml = new Yaml();
            obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("mods/Essentials/config.txt").readString()));

            String text = "# Config version (Don't touch this!)\n" +
                    "version: 3\n\n" +

                    "# Plugin language\n" +
                    "language: en\n\n" +

                    "# Server / client port settings\n#It will enable server as server chat function.\n" +
                    "server-enable: "+this.serverenable+"\n" +
                    "server-port: "+this.serverport+"\n\n" +

                    "client-enable: "+this.clientenable+"\n" +
                    "client-port: "+this.clientport+"\n" +
                    "client-host: "+this.clienthost+"\n\n" +

                    "# If turn on realname, even if the player changes the nickname, it will be set to the previous nickname.\n" +
                    "# If you want colornick features, must enable this.\n" +
                    "realname: "+this.realname+"\n\n" +

                    "# Color nickname update interval. 1sec = 1000\n" +
                    "colornick update interval: "+ cupdatei+"\n\n" +

                    "# If turn on detectreactor, send alert message when the thorium reactor is overheated and explodes.\n" +
                    "detectreactor: "+this.detectreactor+"\n\n" +

                    "# Experience value setting.\n# Base xp is required experience to level up from 1 to 2\n# exponent is EXP multiplier required for the next level.\n\n" +
                    "explimit: "+this.explimit+"\n" +
                    "basexp: "+ basexp+"\n" +
                    "exponent: "+ exponent+"\n" +
                    "levelupalarm: "+ levelupalarm+"\n\n" +

                    "# Ban sharing server config\n# If you enable this, your ban list will send to another public servers.\n" +
                    "banshare: "+this.banshare+"\n\n" +

                    "# Server query config\n# If you enable this, You will be able to get server information from the server port.\n# Ranking page address is http://localhost:server_port/rank\n" +
                    "query: "+this.query+"\n\n" +

                    "# Enable Anti-VPN service.\n" +
                    "antivpn: "+this.antivpn+"\n\n" +

                    "# Enable Anti PvP early time rushing\n" +
                    "enableantirush: "+enableantirush+"\n" +
                    "antirushtime: "+obj.get("antirushtime")+"\n\n"+
                    
                    "# Logging enable\n" +
                    "logging: "+this.logging+"\n\n" +

                    "# Update check enable\n" +
                    "update: "+this.update+"\n\n" +

                    "# Database type setting (Default is SQLite)\n# Example - mariadb://localhost:3306/dbname\n#If you want to use MySQL/MariaDB, You must create a new database yourself.\n" +
                    "sqlite: "+this.sqlite+"\n" +
                    "dburl: "+this.dburl+"\n" +
                    "dbid: "+this.dbid+"\n" +
                    "dbpw: "+this.dbpw+"\n\n" +

                    "# Login features setting\n" +
                    "loginenable: "+this.loginenable+"\n\n" +

                    "# Google translate API Key\n" +
                    "# The api key can be obtained from cloud.google.com.\n" +
                    "apikey: "+this.apikey+"\n\n" +

                    "# The error message is output immediately.\n" +
                    "debug: "+this.debug;
            Core.settings.getDataDirectory().child("mods/Essentials/config.txt").writeString(text);
            Global.log("config file updated!");
        }
    }
}