package essentials;

import io.anuke.arc.Core;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class EssentialConfig {
    public static String clienthost;
    public static int clientport;
    public static int serverport;
    static boolean realname;
    static boolean detectreactor;
    static boolean serverenable;
    static boolean clientenable;
    static double basexp;
    static double exponent;
    public static int cupdatei;
    public static boolean banshare;
    static boolean antivpn;
    private static boolean webhookenable;
    private static String discordurl;
    public static boolean query;
    private static int version;
    private static String language;
    static boolean enableantirush;
    static Calendar antirushtime;

    public static void main() {
        Map<String, Object> obj;
        if (!Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").exists()) {
            String text = "# Config version (Don't touch this!)\n" +
                    "version: 3\n\n" +

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
                    "basexp: 500\n" +
                    "exponent: 1.12f\n\n" +

                    "# Ban sharing server config\n# If you enable this, your ban list will send to another public servers.\n" +
                    "banshare: false\n\n" +

                    "# Server query config\n# If you enable this, You will be able to get server information from the server port.\n" +
                    "query: false\n\n" +

                    "# Enable Anti-VPN service.\n" +
                    "antivpn: true\n\n" +

                    "# Enable Anti PvP early time rushing. Time unit: 1 second\n" +
                    "enableantirush: true\n" +
                    "antirushtime: 10.00";

            Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").writeString(text);
            Global.log("config file created!");
        }

        if (!Core.settings.getDataDirectory().child("plugins/Essentials/Exp.txt").exists()) {
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

            Core.settings.getDataDirectory().child("plugins/Essentials/Exp.txt").writeString(text);
            Global.log("Exp config file created!");
        }

        if (!Core.settings.getDataDirectory().child("plugins/Essentials/BlockReqExp.txt").exists()) {
            String text = "# crafting\n" +
                    "graphite-press: 5\n" +
                    "multi-press: 35\n" +
                    "silicon-smelter: 10\n" +
                    "kiln: 20\n" +
                    "plastanium-compressor: 35\n" +
                    "phase-weaver: 50\n" +
                    "alloy-smelter: 65\n" +
                    "cryofluidmixer: 65\n" +
                    "blast-mixer: 20\n" +
                    "pyratite-mixer: 10\n" +
                    "melter: 50\n" +
                    "separator: 65\n" +
                    "spore-press: 20\n" +
                    "pulverizer: 75\n" +
                    "coal-centrifuge: 35\n" +
                    "incinerator: 35\n\n" +

                    "# Sandbox\n" +
                    "power-void: 65\n" +
                    "power-source: 65\n" +
                    "item-source: 65\n" +
                    "item-void: 65\n" +
                    "liquid-source: 65\n" +
                    "message: 5\n\n" +

                    "# Defence\n" +
                    "scrap-wall: 65\n" +
                    "scrap-wall-large: 65\n" +
                    "scrap-wall-huge: 65\n" +
                    "scrap-wall-gigantic: 65\n" +
                    "thruster: 65\n" +
                    "copper-wall: 5\n" +
                    "copper-wall-large: 10\n" +
                    "titanium-wall: 10\n" +
                    "titanium-wall-large: 20\n" +
                    "thorium-wall: 20\n" +
                    "thorium-wall-large: 35\n" +
                    "phase-wall: 50\n" +
                    "phase-wall-large: 65\n" +
                    "surge-wall: 35\n" +
                    "surge-wall-large: 50\n" +
                    "door: 20\n" +
                    "door-large: 35\n" +
                    "mender: 20\n" +
                    "mend-projector: 35\n" +
                    "overdrive-projector: 65\n" +
                    "force-projector: 50\n" +
                    "shock-mine: 35\n\n" +

                    "# Distribution\n" +
                    "conveyor: 1\n" +
                    "titanium-conveyor: 20\n" +
                    "armored-conveyor: 35\n" +
                    "junction: 5\n" +
                    "bridge-conveyor: 10\n" +
                    "phase-conveyor: 35\n" +
                    "sorter: 20\n" +
                    "router: 10\n" +
                    "distributor: 20\n" +
                    "overflow-gate: 35\n" +
                    "mass-driver: 50\n\n" +

                    "# Liquid\n" +
                    "mechanical-pump: 5\n" +
                    "rotary-pump: 50\n" +
                    "thermal-pump: 65\n" +
                    "conduit: 10\n" +
                    "pulse-conduit: 50\n" +
                    "liquid-router: 35\n" +
                    "liquid-tank: 50\n" +
                    "liquid-junction: 65\n" +
                    "bridge-conduit: 35\n" +
                    "phase-conduit: 65\n\n" +

                    "# Power\n" +
                    "power-node: 10\n" +
                    "power-node-large: 20\n" +
                    "surge-tower: 35\n" +
                    "battery: 20\n" +
                    "battery-large: 35\n" +
                    "combustion-generator: 5\n" +
                    "thermal-generator: 35\n" +
                    "turbine-generator: 20\n" +
                    "differential-generator: 50\n" +
                    "rtg-generator: 75\n" +
                    "solar-panel: 20\n" +
                    "solar-panel-large: 35\n" +
                    "thorium-reactor: 65\n" +
                    "impact-reactor: 75\n\n" +

                    "# Production\n" +
                    "mechanical-drill: 1\n" +
                    "pneumatic-drill: 10\n" +
                    "laser-drill: 20\n" +
                    "blast-drill: 35\n" +
                    "water-extractor: 35\n" +
                    "cultivator: 20\n" +
                    "oil-extractor: 50\n\n" +

                    "# Storage\n" +
                    "vault: 35\n" +
                    "container: 20\n" +
                    "unloader: 65\n\n" +

                    "# Turrets\n" +
                    "duo: 1\n" +
                    "scatter: 5\n" +
                    "scorch: 5\n" +
                    "hail: 10\n" +
                    "wave: 20\n" +
                    "lancer: 20\n" +
                    "arc: 10\n" +
                    "swarmer: 35\n" +
                    "salvo: 20\n" +
                    "fuse: 50\n" +
                    "ripple: 35\n" +
                    "cyclone: 50\n" +
                    "spectre: 65\n" +
                    "meltdown: 70\n\n" +

                    "# Units\n" +
                    "draug-factory: 10\n" +
                    "spirit-factory: 20\n" +
                    "phantom-factory: 35\n" +
                    "command-center: 35\n" +
                    "wraith-factory: 35\n" +
                    "ghoul-factory: 50\n" +
                    "revenant-factory: 65\n" +
                    "dagger-factory: 20\n" +
                    "crawler-factory: 35\n" +
                    "titan-factory: 50\n" +
                    "fortress-factory: 65\n" +
                    "repair-point: 50\n\n" +

                    "# Upgrade\n" +
                    "dart-mech-pad: 10\n" +
                    "delta-mech-pad: 20\n" +
                    "tau-mech-pad: 35\n" +
                    "omega-mech-pad: 50\n" +
                    "javelin-ship-pad: 35\n" +
                    "trident-ship-pad: 50\n" +
                    "glaive-ship-pad: 65";
            Core.settings.getDataDirectory().child("plugins/Essentials/BlockReqExp.txt").writeString(text);
            Global.log("BlockReqExp config file created!");
        }

        if (!Core.settings.getDataDirectory().child("plugins/Essentials/data.json").exists()) {
            JSONObject object = new JSONObject();
            object.put("banall", "true");
            Core.settings.getDataDirectory().child("plugins/Essentials/data.json").writeString(String.valueOf(object));
        }

        if(!Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").exists()){
            JSONObject ban = new JSONObject();
            String json = ban.toString();
            Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").writeString(json);
            Global.log("banned file created!");
        }

        if(!Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").exists()){
            String msg = "To edit this message, modify the [green]motd.txt[] file in the [green]config/plugins/Essentials/[] folder.";
            Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").writeString(msg);
            Global.log("motd file created.");
        }

        if(!Core.settings.getDataDirectory().child("plugins/Essentials/blacklist.json").exists()){
            JSONArray blacklist = new JSONArray();
            String json = blacklist.toString();
            Core.settings.getDataDirectory().child("plugins/Essentials/blacklist.json").writeString(json);
            Global.log("blacklist file created!");
        }

        if (Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").exists()){
            Yaml yaml = new Yaml();
            obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").readString()));
            // Config version
            version = Integer.parseInt(String.valueOf(obj.get("version")));

            language = (String) obj.get("language");

            serverenable = Boolean.parseBoolean(String.valueOf(obj.get("server-enable")));
            serverport = Integer.parseInt(String.valueOf(obj.get("server-port")));

            clientenable = Boolean.parseBoolean(String.valueOf(obj.get("client-enable")));
            clienthost = (String) obj.get("client-host");
            clientport = Integer.parseInt(String.valueOf(obj.get("client-port")));

            realname = Boolean.parseBoolean(String.valueOf(obj.get("realname")));

            cupdatei = Integer.parseInt(String.valueOf(obj.get("colornick update interval")));

            detectreactor = Boolean.parseBoolean(String.valueOf(obj.get("detectreactor")));

            basexp = Double.parseDouble(String.valueOf(obj.get("basexp")));
            exponent = Double.parseDouble(String.valueOf(obj.get("exponent")));

            banshare = Boolean.parseBoolean(String.valueOf(obj.get("banshare")));

            query = Boolean.parseBoolean(String.valueOf(obj.get("query")));

            antivpn = Boolean.parseBoolean(String.valueOf(obj.get("antivpn")));

            webhookenable = Boolean.parseBoolean(String.valueOf(obj.get("webhookenable")));
            discordurl = (String) obj.get("discordurl");

            enableantirush = Boolean.parseBoolean(String.valueOf(obj.get("enableantirush")));

            try{
                SimpleDateFormat format = new SimpleDateFormat("mm.ss");
                Calendar cal;
                Date d = format.parse(String.valueOf(obj.get("antirushtime")));
                cal = Calendar.getInstance();
                cal.setTime(d);
                antirushtime = cal;
            } catch (Exception e){
                e.printStackTrace();
                Global.loge("Invalid settings! - antirushtime");
                Global.loge("Correct value format is mm.ss (Example - 10.00 -> 10minute, 00.30 -> 30seconds)");
            }

            Global.log("config file loaded!");
        }

        if(version < 3){
            Yaml yaml = new Yaml();
            obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").readString()));

            String text = "# Config version (Don't touch this!)\n" +
                    "version: 3\n\n" +

                    "# Plugin language\n" +
                    "language: en\n\n" +

                    "# Server / client port settings\n#It will enable server as server chat function.\n" +
                    "server-enable: "+serverenable+"\n" +
                    "server-port: "+serverport+"\n\n" +

                    "client-enable: "+clientenable+"\n" +
                    "client-port: "+clientport+"\n" +
                    "client-host: "+clienthost+"\n\n" +

                    "# If turn on realname, even if the player changes the nickname, it will be set to the previous nickname.\n" +
                    "# If you want colornick features, must enable this.\n" +
                    "realname: "+realname+"\n\n" +

                    "# Color nickname update interval. 1sec = 1000\n" +
                    "colornick update interval: "+cupdatei+"\n\n" +

                    "# If turn on detectreactor, send alert message when the thorium reactor is overheated and explodes.\n" +
                    "detectreactor: "+detectreactor+"\n\n" +

                    "# Experience value setting.\n# Base xp is required experience to level up from 1 to 2\n# exponent is EXP multiplier required for the next level.\n" +
                    "basexp: "+basexp+"\n" +
                    "exponent: "+exponent+"\n\n" +

                    "# Ban sharing server config\n# If you enable this, your ban list will send to another public servers.\n" +
                    "banshare: "+banshare+"\n\n" +

                    "# Server query config\n# If you enable this, You will be able to get server information from the server port.\n" +
                    "query: "+query+"\n\n" +

                    "# Enable Anti-VPN service.\n" +
                    "antivpn: "+antivpn+"\n\n" +

                    "# Enable Anti PvP early time rushing\n" +
                    "enableantirush: "+enableantirush+"\n" +
                    "antirushtime: "+obj.get("antirushtime");
            Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").writeString(text);
            Global.log("config file updated!");
        }
    }
}