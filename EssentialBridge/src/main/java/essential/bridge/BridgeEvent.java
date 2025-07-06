package essential.bridge;

import arc.files.Fi;
import arc.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import essential.PluginDataKt;
import essential.bundle.Bundle;
import essential.event.CustomEvents;
import ksp.event.Event;

import java.io.IOException;
import java.nio.file.StandardWatchEventKinds;

/**
 * Event handler for bridge configuration file modifications.
 */
public class BridgeEvent {

    /**
     * Event handler for configuration file modifications.
     * This method is called when the bridge configuration file is modified.
     * 
     * @param event The configuration file modified event
     */
    @Event
    public static void configFileModified(CustomEvents.ConfigFileModified event) {
        if (event.getKind() == StandardWatchEventKinds.ENTRY_MODIFY) {
            if (event.getPaths().equals("config_bridge.yaml")) {
                try {
                    // Use Jackson for YAML parsing in Java
                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    Fi configFile = PluginDataKt.getRootPath().child("config/config_bridge.yaml");

                    if (!configFile.exists()) {
                        Log.err(new Bundle().get("event.plugin.load.failed"));
                        return;
                    }

                    BridgeConfig config = mapper.readValue(configFile.file(), BridgeConfig.class);

                    if (config == null) {
                        Log.err(new Bundle().get("event.plugin.load.failed"));
                        return;
                    }

                    Main.conf = config;

                    Log.info(new Bundle().get("config.reloaded"));
                } catch (IOException e) {
                    Log.err("Failed to load config file", e);
                }
            }
        }
    }
}
