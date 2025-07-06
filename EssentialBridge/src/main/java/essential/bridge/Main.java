package essential.bridge;

import arc.ApplicationListener;
import arc.Core;
import arc.util.CommandHandler;
import arc.util.Log;
import essential.bridge.generated.ClientCommandsGeneratedKt;
import essential.bridge.generated.EventHandlersGenerated;
import essential.bundle.Bundle;
import mindustry.mod.Plugin;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Main class for the EssentialBridge plugin.
 */
public class Main extends Plugin {
    // Static fields equivalent to companion object
    public static Bundle bundle = new Bundle();
    public static boolean isServerMode = false;
    public static BridgeConfig conf;
    public static Runnable network;

    // Instance fields
    private ExecutorService daemon = Executors.newSingleThreadExecutor();

    @Override
    public void init() {
        bundle.setPrefix("[EssentialBridge]");

        Log.debug(bundle.get("event.plugin.starting"));

        // Load plugin configuration
        try {
            // Use Jackson for YAML parsing in Java
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper(new com.fasterxml.jackson.dataformat.yaml.YAMLFactory());
            arc.files.Fi configFile = essential.PluginDataKt.getRootPath().child("config/config_bridge.yaml");

            if (!configFile.exists()) {
                // Create default config
                conf = new BridgeConfig();
                configFile.writeString(mapper.writeValueAsString(conf), false);
                Log.info(bundle.get("config.created", "config_bridge.yaml"));
            } else {
                conf = mapper.readValue(configFile.file(), BridgeConfig.class);
                if (conf == null) {
                    Log.err(bundle.get("event.plugin.load.failed"));
                    return;
                }
                Log.info(bundle.get("config.loaded", "config_bridge.yaml"));
            }
        } catch (IOException e) {
            Log.err(bundle.get("event.plugin.load.failed"), e);
            return;
        }

        // Create server or client connection
        try {
            ServerSocket serverSocket = new ServerSocket(conf.getPort());
            serverSocket.close();
            isServerMode = true;
            network = new Server();
        } catch (IOException e) {
            isServerMode = false;
            network = new Client();
        }
        daemon.submit(network);

        // Add shutdown hook
        Core.app.addListener(new ApplicationListener() {
            @Override
            public void dispose() {
                if (isServerMode) {
                    for (java.net.Socket socket : ((Server) network).getClients()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            // Ignore
                        }
                    }
                    ((Server) network).shutdown();
                } else {
                    ((Client) network).send("exit");
                }
                daemon.shutdown();
            }
        });

        // Register event handlers
        EventHandlersGenerated.registerGeneratedEventHandlers();

        Log.debug(bundle.get("event.plugin.loaded"));
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        ClientCommandsGeneratedKt.registerGeneratedClientCommands(handler);
    }
}
