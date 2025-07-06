package essential.discord;

import arc.util.CommandHandler;
import arc.util.Log;
import essential.PluginDataKt;
import essential.bundle.Bundle;
import essential.discord.generated.ClientCommandsGeneratedKt;
import essential.discord.generated.EventHandlersGenerated;
import mindustry.mod.Plugin;

/**
 * Main class for the EssentialDiscord plugin.
 */
public class Main extends Plugin {
    // Static fields equivalent to companion object
    public static Bundle bundle = new Bundle();
    public static DiscordConfig conf;

    @Override
    public void init() {
        bundle.setPrefix("[EssentialDiscord]");

        Log.debug(bundle.get("event.plugin.starting"));

        // Load plugin configuration
        try {
            // Use Jackson for YAML parsing in Java
            com.fasterxml.jackson.core.JsonFactory factory = new com.fasterxml.jackson.dataformat.yaml.YAMLFactory();
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper(factory);
            arc.files.Fi configFile = PluginDataKt.getRootPath().child("config/config_discord.yaml");

            if (!configFile.exists()) {
                // Create default config
                conf = new DiscordConfig();
                configFile.writeString(mapper.writeValueAsString(conf), false);
                Log.info(bundle.get("config.created", "config_discord.yaml"));
            } else {
                conf = mapper.readValue(configFile.file(), DiscordConfig.class);
                if (conf == null) {
                    Log.err(bundle.get("event.plugin.load.failed"));
                    return;
                }
                Log.info(bundle.get("config.loaded", "config_discord.yaml"));
            }
        } catch (Exception e) {
            Log.err(bundle.get("event.plugin.load.failed"), e);
            return;
        }

        // Validate URL
        if (conf.getUrl().isEmpty() || !conf.getUrl().matches("https://discord\\.gg/[a-zA-Z0-9]{1,16}")) {
            Log.warn(bundle.get("config.invalid.url"));
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
