package essential.protect;

import arc.Events;
import arc.files.Fi;
import arc.util.Log;
import arc.util.Strings;
import arc.util.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import essential.PluginDataKt;
import essential.bundle.Bundle;
import essential.event.CustomEvents;
import essential.util.UtilsKt;
import ksp.event.Event;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Call;
import mindustry.gen.Player;

import java.io.IOException;
import java.nio.file.StandardWatchEventKinds;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Event handlers for the EssentialProtect plugin.
 */
public class ProtectEvent {

    /**
     * Event handler for configuration file modifications.
     * This method is called when the protect configuration file is modified.
     * 
     * @param event The configuration file modified event
     */
    @Event
    public static void configFileModified(CustomEvents.ConfigFileModified event) {
        if (event.getKind() == StandardWatchEventKinds.ENTRY_MODIFY) {
            if (event.getPaths().equals("config_protect.yaml")) {
                try {
                    // Use Jackson for YAML parsing in Java
                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    Fi configFile = PluginDataKt.getRootPath().child("config/config_protect.yaml");

                    if (!configFile.exists()) {
                        Log.err(new Bundle().get("event.plugin.load.failed"));
                        return;
                    }

                    ProtectConfig config = mapper.readValue(configFile.file(), ProtectConfig.class);

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

    /**
     * Event handler for player join events.
     * This method is called when a player joins the server.
     * 
     * @param event The player join event
     */
    @Event
    public static void playerJoin(EventType.PlayerJoin event) {
        Player player = event.player;
        Bundle bundle = new Bundle(player.locale());

        // Check for VPN if enabled
        if (Main.conf.getRules().isVpn()) {
            String ip = player.ip();

            // Check if the IP is in the VPN list
            if (Arrays.asList(Main.pluginData.getVpnList()).contains(ip)) {
                player.kick(bundle.get("event.vpn.detected"));
                return;
            }
        }

        // Check for minimal name length if enabled
        if (Main.conf.getRules().getMinimalName().isEnabled()) {
            int minLength = Main.conf.getRules().getMinimalName().getLength();
            if (player.name().length() < minLength) {
                player.kick(bundle.get("event.name.too.short", minLength));
                return;
            }
        }

        // Check for Steam-only mode if enabled
        if (Main.conf.getRules().isSteamOnly()) {
            // Check if player is NOT using Steam (simplified check)
            if (player.con() == null || !player.con().address.startsWith("steam")) {
                player.kick(bundle.get("event.steam.only"));
                return;
            }
        }

        // Check for mobile restrictions if enabled
        if (Main.conf.getRules().isMobile()) {
            if (Strings.stripColors(player.name()).toLowerCase().contains("mobile")) {
                player.kick(bundle.get("event.mobile.not.allowed"));
                return;
            }
        }

        // Check for strict mode if enabled
        if (Main.conf.getRules().isStrict()) {
            // Check for special characters in name
            Pattern pattern = Pattern.compile("[^a-zA-Z0-9\\s]");
            if (pattern.matcher(Strings.stripColors(player.name())).find()) {
                player.kick(bundle.get("event.name.special.characters"));
            }
        }
    }

    /**
     * Event handler for player chat events.
     * This method is called when a player sends a chat message.
     * 
     * @param event The player chat event
     */
    @Event
    public static void playerChat(EventType.PlayerChatEvent event) {
        // Check for chat restrictions
        if (event.message.startsWith("/")) {
            return; // Skip command messages
        }

        Player player = event.player;
        essential.database.data.PlayerData data = UtilsKt.findPlayerData(player.uuid());

        if (data != null && data.getChatMuted()) {
            player.sendMessage(new Bundle(player.locale()).get("event.chat.muted"));
            // We can't modify event.message directly, but we can prevent it from being processed further
            Events.fire(new EventType.PlayerChatEvent(player, "")); // Fire empty message event
            return; // Exit early to prevent the original message from being processed
        }
    }

    /**
     * Event handler for block build events.
     * This method is called when a player builds or destroys a block.
     * 
     * @param event The block build end event
     */
    @Event
    public static void blockBuildEnd(EventType.BlockBuildEndEvent event) {
        // Check for unbreakable core protection
        if (Main.conf.getProtect().isUnbreakableCore() &&
            event.breaking && 
            event.tile.block().name.contains("core")) {

            // Cancel the build if it's breaking a core
            event.unit.clearBuilding();

            // Notify the player if it's a player unit
            if (event.unit.isPlayer()) {
                Player player = event.unit.getPlayer();
                player.sendMessage(new Bundle(player.locale()).get("event.core.unbreakable"));
            }
        }
    }

    /**
     * Event handler for game over events.
     * This method is called when a game ends.
     * 
     * @param event The game over event
     */
    @Event
    public static void gameOver(EventType.GameOverEvent event) {
        // Reset PvP peace timer if enabled
        if (Main.conf.getPvp().getPeace().isEnabled()) {
            Vars.state.rules.pvp = false;

            // Schedule PvP to be enabled after the peace time
            int peaceTime = Main.conf.getPvp().getPeace().getTime();
            if (peaceTime > 0) {
                Timer.schedule(() -> {
                    Vars.state.rules.pvp = true;
                    Call.setRules(Vars.state.rules);
                    Call.sendMessage(new Bundle().get("event.pvp.enabled"));
                }, peaceTime);
            }
        }
    }
}
