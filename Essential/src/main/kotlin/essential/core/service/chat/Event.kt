package essential.core.service.chat

import essential.common.bundle.Bundle
import essential.common.database.data.PlayerData
import essential.common.permission.Permission
import essential.common.rootPath
import essential.common.util.findPlayerData
import essential.core.service.chat.ChatFormatResolver.resolve
import essential.core.service.chat.ChatService.Companion.conf
import ksp.event.Event
import mindustry.Vars
import mindustry.core.NetServer
import mindustry.game.EventType
import mindustry.gen.Player
import mindustry.net.Administration
import java.util.regex.Pattern

@Event
fun serverLoaded(event: EventType.ServerLoadEvent) {
    Vars.netServer.admins.addChatFilter(object : Administration.ChatFilter {
        override fun filter(player: Player, message: String): String? {
            val bundle = Bundle(player.locale)

            if (conf.blacklist.enabled) {
                val file: Array<String> =
                    rootPath.child("chat_blacklist.txt").readString("UTF-8").split(Regex("\\R")).filter { it.isNotBlank() }.toTypedArray()
                for (text in file) {
                    if (conf.blacklist.regex) {
                        if (Pattern.compile(text).matcher(message).find()) {
                            player.sendMessage(bundle["event.chat.blacklisted"])
                            return null
                        }
                    } else {
                        if (message.contains(text)) {
                            player.sendMessage(bundle["event.chat.blacklisted"])
                            return null
                        }
                    }
                }
            }

            return message
        }
    })

    Vars.netServer.chatFormatter = NetServer.ChatFormatter { player, message ->
        if (player != null) {
            val data: PlayerData? = findPlayerData(player.uuid())
            if (message != null) {
                val defaultFormat = "[coral][[" + player.coloredName() + "[coral]]:[white] " + message
                if (data != null) {
                    val chatFormat: String = Permission[data].chatFormat
                    if (chatFormat.isEmpty()) {
                        return@ChatFormatter defaultFormat
                    } else {
                        return@ChatFormatter resolve(chatFormat, data, message)
                    }
                } else {
                    return@ChatFormatter defaultFormat
                }
            }
        }
        return@ChatFormatter null
    }
}