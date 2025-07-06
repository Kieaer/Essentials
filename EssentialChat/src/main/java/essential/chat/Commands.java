package essential.chat;

import arc.Core;
import essential.database.data.PlayerData;
import essential.util.UtilsKt;
import ksp.command.ClientCommand;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Playerc;

/**
 * Commands implementation for the EssentialChat plugin.
 */
public class Commands {
    private static final String PLAYER_NOT_FOUND = "player.not.found";

    /**
     * Chat with a special prefix.
     *
     * @param playerData The player data
     * @param args The command arguments
     */
    @ClientCommand(name = "me", parameter = "<text...>", description = "Chat with special prefix")
    public void me(PlayerData playerData, String[] args) {
        if (playerData.getChatMuted()) {
            return;
        }

        ChatConfig conf = Main.conf;
        if (conf.getBlacklist().isEnabled()) {
            String[] blacklist = Core.settings.getDataDirectory().child("chat_blacklist.txt").readString("UTF-8").split("\r\n");
            String message = args[0];
            
            for (String s : blacklist) {
                if ((conf.getBlacklist().isRegex() && message.matches(s)) ||
                    (!conf.getBlacklist().isRegex() && message.contains(s))) {
                    playerData.err("event.chat.blacklisted");
                    return;
                }
            }
        }

        String message = args[0];
        Call.sendMessage("[orange]*[]" + Vars.netServer.chatFormatter.format(playerData.getPlayer().as(), message));
    }

    /**
     * Send a private message to another player.
     *
     * @param playerData The player data
     * @param args The command arguments
     */
    @ClientCommand(name = "pm", parameter = "<player> <message...>", description = "Send a private message")
    public void pm(PlayerData playerData, String[] args) {
        if (playerData.getChatMuted()) {
            return;
        }

        String targetName = args[0];
        Playerc target = UtilsKt.findPlayers(targetName);

        if (target == null) {
            playerData.err(PLAYER_NOT_FOUND);
        } else if (args.length > 1) {
            String message = args[1];
            playerData.getPlayer().sendMessage("[green][PM] " + target.plainName() + "[yellow] => [white] " + message);
            target.sendMessage("[blue][PM] [gray][" + playerData.getEntityId() + "][]" + playerData.getPlayer().plainName() + "[yellow] => [white] " + message);

            // This part is commented out as it requires access to database and Permission which we don't have proper references for
            /*
            database.getPlayers().stream().filter { p ->
                Permission.INSTANCE.check(p, "pm.other") && p.uuid != player.uuid() && target.uuid() != player.uuid()
            }.forEach { p ->
                p.player.sendMessage("[sky]${player.plainName()}[][yellow] => [pink]${target.plainName()} [white]: ${message}")
            }
            */
        } else {
            playerData.err("command.pm.message");
        }
    }
}