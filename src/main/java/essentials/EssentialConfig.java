package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.util.Log;
import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class EssentialConfig {
    static String clienthost;
    static int clientport;
    static int serverport;
    static boolean realname;
    static boolean detectreactor;
    static boolean serverenable;
    static boolean clientenable;
    static double basexp;
    static double exponent;

    static void main() {
        Map<String, Object> obj;
        if (!Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").exists()) {
            // Make file
            Core.settings.getDataDirectory().child("plugins/Essentials/config.txt").writeString("");

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml yaml = new Yaml(options);

            Map<String, Object> data = new HashMap<>();
            data.put("server-enable", false);
            data.put("server-port", 25000);
            data.put("client-port", 20000);
            data.put("realname", true);
            data.put("client-enable", false);
            data.put("client-host", "localhost");
            data.put("detectreactor", true);
            data.put("basexp", 1000);
            data.put("exponent", 1.12f);

            FileWriter writer = null;
            try {
                writer = new FileWriter(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/config.txt")));
            } catch (IOException e) {
                e.printStackTrace();
            }
            yaml.dump(data, writer);

            Log.info("[Essentials] config file created!");
        }

        if (!Core.settings.getDataDirectory().child("plugins/Essentials/Exp.txt").exists()) {
            // Make file
            Core.settings.getDataDirectory().child("plugins/Essentials/Exp.txt").writeString("");

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml yaml = new Yaml(options);

            Map<String, Object> data = new HashMap<>();

            // crafting
            data.put("graphite-press", 5);
            data.put("multi-press", 15);
            data.put("silicon-smelter", 5);
            data.put("kiln", 8);
            data.put("plastanium-compressor", 20);
            data.put("phase-weaver", 35);
            data.put("cryofluidmixer", 15);
            data.put("blast-mixer", 8);
            data.put("pyratite-mixer", 12);
            data.put("melter", 3);
            data.put("separator", 6);
            data.put("spore-press", 5);
            data.put("pulverizer", 2);
            data.put("coal-centrifuge", 3);
            data.put("incinerator", 2);

            // defense
            data.put("copper-wall", 1);
            data.put("copper-wall-large", 4);
            data.put("titanium-wall", 2);
            data.put("titanium-wall-large", 8);
            data.put("thorium-wall", 3);
            data.put("thorium-wall-large", 12);
            data.put("phase-wall", 4);
            data.put("phase-wall-large", 16);
            data.put("surge-wall", 5);
            data.put("surge-wall-large", 20);
            data.put("door", 2);
            data.put("door-large", 6);
            data.put("mender", 2);
            data.put("mend-projector", 10);
            data.put("overdrive-projector", 12);
            data.put("force-projector", 35);
            data.put("shock-mine", 5);

            // Distribution
            data.put("conveyor", 1);
            data.put("titanium-conveyor", 2);
            data.put("junction", 1);
            data.put("bridge-conveyor", 1);
            data.put("phase-conveyor", 5);
            data.put("sorter", 2);
            data.put("router", 1);
            data.put("distributor", 1);
            data.put("overflow-gate", 2);
            data.put("mass-driver", 15);

            // Liquid
            data.put("mechanical-pump", 2);
            data.put("rotary-pump", 8);
            data.put("thermal-pump", 15);
            data.put("conduit", 2);
            data.put("pulse-conduit", 4);
            data.put("liquid-router", 3);
            data.put("liquid-tank", 10);
            data.put("liquid-junction", 2);
            data.put("bridge-conduit", 2);
            data.put("phase-conduit", 5);

            // Power
            data.put("power-node", 1);
            data.put("power-node-large", 3);
            data.put("surge-tower", 10);
            data.put("battery", 3);
            data.put("battery-large", 21);
            data.put("combustion-generator", 5);
            data.put("thermal-generator", 8);
            data.put("turbine-generator", 12);
            data.put("rtg-generator", 15);
            data.put("solar-panel", 6);
            data.put("solar-panel-large", 18);
            data.put("thorium-reactor", 25);
            data.put("impact-reactor", 200);

            // Production
            data.put("mechanical-drill", 2);
            data.put("pneumatic-drill", 5);
            data.put("laser-drill", 10);
            data.put("blast-drill", 15);
            data.put("water-extractor", 3);
            data.put("cultivator", 7);
            data.put("oil-extractor", 15);

            // Storage
            data.put("vault", 20);
            data.put("container", 7);
            data.put("unloader", 3);

            // Turrets
            data.put("duo", 3);
            data.put("scatter", 5);
            data.put("scorch", 8);
            data.put("hail", 10);
            data.put("wave", 12);
            data.put("lancer", 15);
            data.put("arc", 18);
            data.put("swarmer", 30);
            data.put("salvo", 24);
            data.put("fuse", 40);
            data.put("ripple", 50);
            data.put("cyclone", 60);
            data.put("spectre", 100);
            data.put("meltdown", 250);

            // Units
            data.put("draug-factory", 8);
            data.put("spirit-factory", 12);
            data.put("phantom-factory", 18);
            data.put("command-center", 30);
            data.put("wraith-factory", 15);
            data.put("ghoul-factory", 25);
            data.put("revenant-factory", 40);
            data.put("dagger-factory", 6);
            data.put("crawler-factory", 8);
            data.put("titan-factory", 12);
            data.put("fortress-factory", 18);
            data.put("repair-point", 2);

            // Upgrade
            data.put("dart-mech-pad", 15);
            data.put("delta-mech-pad", 30);
            data.put("tau-mech-pad", 40);
            data.put("omega-mech-pad", 150);
            data.put("javelin-ship-pad", 35);
            data.put("trident-ship-pad", 45);
            data.put("glaive-ship-pad", 150);

            FileWriter writer = null;
            try {
                writer = new FileWriter(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Exp.txt")));
            } catch (IOException e) {
                e.printStackTrace();
            }
            yaml.dump(data, writer);

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
            Log.info("[Essentials] config file loaded!");
        }
    }
}