package essential.chat;

import arc.util.CommandHandler;
import arc.util.Log;
import essential.PluginDataKt;
import essential.bundle.Bundle;
import essential.chat.generated.ClientCommandsGeneratedKt;
import essential.chat.generated.EventHandlersGenerated;
import mindustry.mod.Plugin;

/**
 * Main class for the EssentialChat plugin.
 */
public class Main extends Plugin {
    // Static field equivalent to companion object
    public static Bundle bundle = new Bundle();
    public static ChatConfig conf;

    @Override
    public void init() {
        bundle.setPrefix("[EssentialChat]");

        Log.debug(bundle.get("event.plugin.starting"));

        // Load configuration
        try {
            // Use Jackson for YAML parsing in Java
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            arc.files.Fi configFile = PluginDataKt.getRootPath().child("config/config_chat.yaml");

            if (!configFile.exists()) {
                // Create default config
                conf = new ChatConfig();
                configFile.writeString(mapper.writeValueAsString(conf), false);
                Log.info(bundle.get("config.created", "config_chat.yaml"));
            } else {
                conf = mapper.readValue(configFile.file(), ChatConfig.class);
                if (conf == null) {
                    Log.err(bundle.get("event.plugin.load.failed"));
                    return;
                }
                Log.info(bundle.get("config.loaded", "config_chat.yaml"));
            }
        } catch (Exception e) {
            Log.err(bundle.get("event.plugin.load.failed"), e);
            return;
        }

        // Register event handlers
        EventHandlersGenerated.registerGeneratedEventHandlers();

        // Initialize chat filter and language detector
        ChatEvents.load();

        // Create chat blacklist file if it doesn't exist
        if (!PluginDataKt.getRootPath().child("chat_blacklist.txt").exists()) {
            PluginDataKt.getRootPath().child("chat_blacklist.txt").writeString("않");
        }

        Log.debug(bundle.get("event.plugin.loaded"));
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        ClientCommandsGeneratedKt.registerGeneratedClientCommands(handler);
    }
}
