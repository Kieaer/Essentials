package essential.chat

import essential.core.DB

class Commands {
    @ClientCommand(name = "me", parameter = "<text...>", description = "Chat with special prefix")
    fun me(player: Playerc, playerData: PlayerData, arg: Array<String?>) {
        if (playerData.getMute()) return
        if (Main.Companion.conf.blacklist.enabled) {
            val file: Array<String> = root.child("chat_blacklist.txt").readString("UTF-8").split("\r\n")
            for (s in file) {
                if ((Main.Companion.conf.blacklist.regex && arg[0]!!.matches(s.toRegex())) || (!Main.Companion.conf.blacklist.regex && arg[0]!!.contains(
                        s
                    ))
                ) {
                    playerData.err("event.chat.blacklisted")
                    return
                }
            }
        }

        Call.sendMessage("[orange]*[]" + Vars.netServer.chatFormatter.format(player.`as`(), arg[0]))
    }

    @ClientCommand(name = "pm", parameter = "<player> <message...>", description = "Send a private message")
    fun pm(player: Playerc, playerData: PlayerData, arg: Array<String?>) {
        if (playerData.getMute()) return
        val target: Playerc? = findPlayers(arg[0])
        if (target == null) {
            playerData.err(PLAYER_NOT_FOUND)
        } else if (arg.size > 1) {
            player.sendMessage("[green][PM] " + target.plainName() + "[yellow] => [white] " + arg[1])
            target.sendMessage("[blue][PM] [gray][" + playerData.getEntityid() + "][]" + player.plainName() + "[yellow] => [white] " + arg[1])
            database.getPlayers().stream().filter({ p ->
                Permission.INSTANCE.check(p, "pm.other") && !p.getUuid()
                    .equals(player.uuid()) && (target.uuid() != player.uuid())
            }).forEach({ p ->
                p.getPlayer()
                    .sendMessage("[sky]\${player.plainName()}[][yellow] => [pink]\${target.plainName()} [white]: \${arg[1]}")
            }
            )
        } else {
            playerData.err("command.pm.message")
        }
    }
}
