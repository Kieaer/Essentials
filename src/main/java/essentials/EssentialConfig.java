package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

public class EssentialConfig {
    static String clienthost;
    static int clientport;
    static int serverport;
    static boolean realname;
    static boolean detectreactor;
    static boolean serverenable;
    static boolean clientenable;
    static double basexp;
    static double exponent;
    public static int cupdatei;
    static boolean banshare;
    static boolean antivpn;
    static boolean webhookenable;
    static String discordurl;

    static void main() {
        Map<String, Object> obj;
        if (!Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").exists()) {
            String text = "# server to server chat config\n" +
                    "server-enable: false\n" +
                    "server-port: 25000\n\n" +
                    "client-port: 20000\n" +
                    "client-enable: false\n" +
                    "client-host: localhost\n\n" +

                    "# If turn on realname, even if the player changes the nickname, it will be set to the previous nickname.\n" +
                    "# If you want colornick features, must enable this.\n" +
                    "realname: true\n\n" +

                    "# Color nickname update interval. 1sec = 1000\n" +
                    "colornick update interval: 1000\n\n" +

                    "# If turn on detectreactor, send alert message when the thorium reactor is overheated and explodes.\n" +
                    "detectreactor: true\n\n" +

                    "# Experience value setting.\n# Base xp is required experience to level up from 1 to 2\n# exponent is EXP multiplier required for the next level.\n" +
                    "basexp: 1000\n" +
                    "exponent: 1.12f\n\n" +

                    "# [NOT WORKING NOW] Ban sharing server config\n# If you enable this, your ban list will send to another public servers.\n" +
                    "banshare: false\n\n" +

                    "# Enable Anti-VPN service.\n" +
                    "antivpn: true\n\n" +

                    "# Use Discord webbook to send server conversations to Discord.\n" +
                    "webhookenable: false\n" +
                    "discordurl: none";

            Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").writeString(text);
            Log.info("[Essentials] config file created!");
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
            Log.info("[Essentials] Exp config file created!");
        }

        if(!Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").exists()){
            JSONObject ban = new JSONObject();
            String json = ban.toString();
            Core.settings.getDataDirectory().child("plugins/Essentials/banned.json").writeString(json);
            Log.info("[Essentials] banned file created!");
        }

        if(!Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").exists()){
            String msg = "To edit this message, modify the [green]motd.txt[] file in the [green]config/plugins/Essentials/[] folder.";
            Core.settings.getDataDirectory().child("plugins/Essentials/motd.txt").writeString(msg);
            Log.info("[Essentials] motd file created.");
        }

        if(!Core.settings.getDataDirectory().child("plugins/Essentials/blacklist.json").exists()){
            JSONArray blacklist = new JSONArray();
            String json = blacklist.toString();
            Core.settings.getDataDirectory().child("plugins/Essentials/blacklist.json").writeString(json);
            Log.info("[Essentials] blacklist file created!");
        }

        if (Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").exists()){
            Yaml yaml = new Yaml();
            obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").readString()));
            clienthost = (String) obj.get("client-host");
            clientport = Integer.parseInt(String.valueOf(obj.get("client-port")));
            serverport = Integer.parseInt(String.valueOf(obj.get("server-port")));
            realname = Boolean.parseBoolean(String.valueOf(obj.get("realname")));
            detectreactor = Boolean.parseBoolean(String.valueOf(obj.get("detectreactor")));
            serverenable = Boolean.parseBoolean(String.valueOf(obj.get("server-enable")));
            clientenable = Boolean.parseBoolean(String.valueOf(obj.get("client-enable")));
            basexp = Double.parseDouble(String.valueOf(obj.get("basexp")));
            exponent = Double.parseDouble(String.valueOf(obj.get("exponent")));
            cupdatei = Integer.parseInt(String.valueOf(obj.get("colornick update interval")));
            banshare = Boolean.parseBoolean(String.valueOf(obj.get("banshare")));
            antivpn = Boolean.parseBoolean(String.valueOf(obj.get("antivpn")));
            webhookenable = Boolean.parseBoolean(String.valueOf(obj.get("webhookenable")));
            discordurl = (String) obj.get("discordurl");
            Log.info("[Essentials] config file loaded!");
        }
    }
}