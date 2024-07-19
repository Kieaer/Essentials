package essential.chat;

import essential.core.DB;
import essential.core.Permission;
import essential.core.annotation.ClientCommand;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Playerc;

import java.util.Objects;

import static essential.chat.Main.conf;
import static essential.core.Commands.PLAYER_NOT_FOUND;
import static essential.core.Event.findPlayers;
import static essential.core.Main.database;
import static essential.core.Main.root;

public class Commands {
    @ClientCommand(name = "me", parameter = "<text...>", description = "Chat with special prefix")
    void me(Playerc player, DB.PlayerData playerData, String[] arg) {
        if (playerData.getMute()) return;
        if (conf.getBlacklist().getEnabled()) {
            String[] file = root.child("chat_blacklist.txt").readString("UTF-8").split("\r\n");
            if (file.length != 0) {
                for (String s : file) {
                    if ((conf.getBlacklist().getRegex() && arg[0].matches(s)) || (!conf.getBlacklist().getRegex() && arg[0].contains(s))) {
                        playerData.err("event.chat.blacklisted");
                        return;
                    }
                }
                Call.sendMessage("[orange]*[]" + Vars.netServer.chatFormatter.format(player.as(), arg[0]));
            }
        }
    }

    @ClientCommand(name = "pm", parameter = "<player> <message...>", description = "Send a private message")
    void pm(Playerc player, DB.PlayerData playerData, String[] arg) {
        if (playerData.getMute()) return;
        Playerc target = findPlayers(arg[0]);
        if (target == null) {
            playerData.err(PLAYER_NOT_FOUND);
        } else if (arg.length > 1) {
            player.sendMessage("[green][PM] " + target.plainName() + "[yellow] => [white] " + arg[1]);
            target.sendMessage("[blue][PM] [gray][" + playerData.getEntityid() + "][]" + player.plainName() + "[yellow] => [white] " + arg[1]);
            database.getPlayers().stream().filter( p -> Permission.INSTANCE.check(p, "pm.other") && !p.getUuid().equals(player.uuid()) && !Objects.equals(target.uuid(), player.uuid())).forEach(p ->
                    p.getPlayer().sendMessage("[sky]${player.plainName()}[][yellow] => [pink]${target.plainName()} [white]: ${arg[1]}")
            );
        } else {
            playerData.err("command.pm.message");
        }
    }
}
