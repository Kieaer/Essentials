package essential.protect;

import arc.util.CommandHandler;
import arc.util.Log;
import essential.PluginDataKt;
import essential.bundle.Bundle;
import essential.protect.generated.ClientCommandsGeneratedKt;
import essential.protect.generated.EventHandlersGenerated;
import mindustry.mod.Plugin;

/**
 * Main class for the EssentialProtect plugin.
 */
public class Main extends Plugin {
    // Static fields equivalent to companion object
    public static Bundle bundle = new Bundle();
    public static ProtectConfig conf;
    public static PluginData pluginData = new PluginData();

    @Override
    public void init() {
        bundle.setPrefix("[EssentialProtect]");

        Log.debug(bundle.get("event.plugin.starting"));

        // Load plugin configuration
        try {
            // Use Jackson for YAML parsing in Java
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            arc.files.Fi configFile = PluginDataKt.getRootPath().child("config/config_protect.yaml");

            if (!configFile.exists()) {
                // Create default config
                conf = new ProtectConfig();
                configFile.writeString(mapper.writeValueAsString(conf), false);
                Log.info(bundle.get("config.created", "config_protect.yaml"));
            } else {
                conf = mapper.readValue(configFile.file(), ProtectConfig.class);
                if (conf == null) {
                    Log.err(bundle.get("event.plugin.load.failed"));
                    return;
                }
                Log.info(bundle.get("config.loaded", "config_protect.yaml"));
            }
        } catch (Exception e) {
            Log.err(bundle.get("event.plugin.load.failed"), e);
            return;
        }

        // Load VPN list if enabled
        if (conf.getRules().isVpn()) {
            try {
                arc.files.Fi vpnFile = PluginDataKt.getRootPath().child("vpn_list.txt");
                if (vpnFile.exists()) {
                    String[] vpnList = vpnFile.readString("UTF-8").split("\r\n");
                    pluginData.setVpnList(vpnList);
                    Log.info(bundle.get("vpn.list.loaded", vpnList.length));
                } else {
                    // Create empty VPN list file
                    vpnFile.writeString("");
                    Log.info(bundle.get("vpn.list.created"));
                }
            } catch (Exception e) {
                Log.err("Failed to load VPN list", e);
            }
        }

        // Register event handlers
        EventHandlersGenerated.registerGeneratedEventHandlers();

        Log.debug(bundle.get("event.plugin.loaded"));
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        ClientCommandsGeneratedKt.registerGeneratedClientCommands(handler);
    }
}
