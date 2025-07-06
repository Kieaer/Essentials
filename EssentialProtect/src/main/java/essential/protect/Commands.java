package essential.protect;

import arc.Events;
import arc.util.Log;
import essential.bundle.Bundle;
import essential.event.CustomEvents;
import ksp.command.ClientCommand;
import mindustry.Vars;
import mindustry.gen.Player;
import mindustry.net.Administration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Commands implementation for the EssentialProtect plugin.
 */
public class Commands {
    private static final String PLAYER_NOT_FOUND = "player.not.found";
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Login command for player authentication.
     * This is a simplified version that doesn't interact with the database.
     *
     * @param playerData The player data
     * @param args The command arguments
     */
    @ClientCommand(name = "login", parameter = "<id> <password>", description = "Log-in to account.")
    public void login(essential.database.data.PlayerData playerData, String[] args) {
        if (args.length < 2) {
            playerData.err("command.login.usage");
            return;
        }

        Bundle bundle = playerData.getBundle();
        Player player = (Player) playerData.getPlayer();

        if (args[0].equals(args[1])) {
            player.sendMessage(bundle.get("command.login.same.password"));
            return;
        }

        // Simplified implementation that just shows a message
        Log.info("Login attempt: " + args[0]);
        player.sendMessage(bundle.get("command.login.not.found"));
    }

    /**
     * Register command for player account creation.
     * This is a simplified version that doesn't interact with the database.
     *
     * @param playerData The player data
     * @param args The command arguments
     */
    @ClientCommand(name = "reg", parameter = "<id> <password> <password_repeat>", description = "Register account")
    public void register(essential.database.data.PlayerData playerData, String[] args) {
        if (args.length < 3) {
            playerData.err("command.reg.usage");
            return;
        }

        Bundle bundle = playerData.getBundle();
        Player player = (Player) playerData.getPlayer();

        if (Main.conf.getAccount().getAuthType() != ProtectConfig.AuthType.None) {
            if (!args[1].equals(args[2])) {
                player.sendMessage(bundle.get("command.reg.incorrect"));
                return;
            }

            // Simplified implementation that just shows a message
            Log.info("Registration attempt: " + args[0]);
            player.sendMessage(bundle.get("command.reg.success"));
        } else if (Main.conf.getAccount().getAuthType() == ProtectConfig.AuthType.Discord) {
            if (Vars.mods.getMod("essential-discord") != null) {
                Events.fire(new CustomEvents.PlayerDiscordRequested(player.uuid()));
            } else {
                player.sendMessage(bundle.get("command.reg.unavailable-server"));
            }
        } else {
            player.sendMessage(bundle.get("command.reg.unavailable"));
        }
    }

    /**
     * Report command for reporting players.
     *
     * @param playerData The player data
     * @param args The command arguments
     */
    @ClientCommand(name = "report", parameter = "<player> <reason...>", description = "Report a player")
    public void report(essential.database.data.PlayerData playerData, String[] args) {
        if (args.length < 2) {
            playerData.err("command.report.usage");
            return;
        }

        Player player = (Player) playerData.getPlayer();
        Administration.PlayerInfo target = Vars.netServer.admins.findByName(args[0]).first();

        if (target != null) {
            String reason = args[1];
            String date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Format the report text
            String text = new Bundle().get("command.report.texts", 
                target.lastName, player.plainName(), reason, 
                target.lastName, String.join(",", target.names), 
                target.id, target.lastIP, String.join(",", target.ips));

            // Log the report (simplified)
            Log.info("REPORT: " + date + " " + text);

            Log.info(new Bundle().get("command.report.received", 
                player.plainName(), target.lastName, reason));

            playerData.send("command.report.done", target.lastName);

            // Fire report event
            Events.fire(new CustomEvents.PlayerReported(
                player.plainName(), target.lastName, reason));
        } else {
            playerData.err(PLAYER_NOT_FOUND);
        }
    }

    /**
     * Clean up resources when the plugin is unloaded.
     */
    public void dispose() {
        executor.shutdown();
    }
}
