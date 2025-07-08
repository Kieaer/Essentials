package essential.chat

import arc.Core
import essential.database.data.PlayerData
import essential.util.findPlayers
import ksp.command.ClientCommand
import mindustry.Vars
import mindustry.gen.Call
import mindustry.gen.Playerc

class Commands {
    companion object {
        private const val PLAYER_NOT_FOUND = "player.not.found"
    }
    @ClientCommand(name = "me", parameter = "<text...>", description = "Chat with special prefix")
    fun me(playerData: PlayerData, arg: Array<out String>) {
        if (playerData.chatMuted) return

        val conf = Main.Companion.conf
        if (conf.blacklist.enabled) {
            val file: Array<String> = Core.settings.dataDirectory.child("chat_blacklist.txt").readString("UTF-8").split("\r\n").toTypedArray()
            for (s in file) {
                val message = arg[0]
                if ((conf.blacklist.regex && message.matches(s.toRegex())) || (!conf.blacklist.regex && message.contains(s))) {
                    playerData.err("event.chat.blacklisted")
                    return
                }
            }
        }

        val message = arg[0]
        Call.sendMessage("[orange]*[]" + Vars.netServer.chatFormatter.format(playerData.player.`as`(), message))
    }

    @ClientCommand(name = "pm", parameter = "<player> <message...>", description = "Send a private message")
    fun pm(playerData: PlayerData, arg: Array<out String>) {
        if (playerData.chatMuted) return

        val targetName = arg[0]
        val target: Playerc? = findPlayers(targetName)

        if (target == null) {
            playerData.err(PLAYER_NOT_FOUND)
        } else if (arg.size > 1) {
            val message = arg[1]
            playerData.player.sendMessage("[green][PM] " + target.plainName() + "[yellow] => [white] " + message)
            target.sendMessage("[blue][PM] [gray][" + playerData.entityId + "][]" + playerData.player.plainName() + "[yellow] => [white] " + message)

            // This part is commented out as it requires access to database and Permission which we don't have proper references for
            /*
            database.getPlayers().stream().filter { p ->
                Permission.INSTANCE.check(p, "pm.other") && p.uuid != player.uuid() && target.uuid() != player.uuid()
            }.forEach { p ->
                p.player.sendMessage("[sky]${player.plainName()}[][yellow] => [pink]${target.plainName()} [white]: ${message}")
            }
            */
        } else {
            playerData.err("command.pm.message")
        }
    }
}
