package essential.chat

import arc.util.Log
import essential.chat.Main.Companion.conf
import essential.common.bundle.Bundle
import essential.common.config.Config
import essential.common.database.data.PlayerData
import essential.common.event.CustomEvents
import essential.common.permission.Permission
import essential.common.rootPath
import essential.common.util.findPlayerData
import ksp.event.Event
import mindustry.Vars
import mindustry.core.NetServer
import mindustry.game.EventType
import mindustry.gen.Player
import mindustry.net.Administration
import java.nio.file.StandardWatchEventKinds
import java.util.regex.Pattern

fun load() {
    Vars.netServer.admins.addChatFilter(object : Administration.ChatFilter {
        private val specificTextRegex: Pattern =
            Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]")

        override fun filter(player: Player, message: String): String? {
            val bundle = Bundle(player.locale)

            if (conf.blacklist.enabled) {
                val file: Array<String> =
                    rootPath.child("chat_blacklist.txt").readString("UTF-8").split("\r\n").toTypedArray()
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


}

@Event
fun serverLoaded(event: EventType.ServerLoadEvent) {
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
                        return@ChatFormatter chatFormat
                            .replace("%1", player.coloredName())
                            .replace("%2", message)
                    }
                } else {
                    return@ChatFormatter defaultFormat
                }
            }
        }
        return@ChatFormatter null
    }
}

@Event
fun configFileModified(it: CustomEvents.ConfigFileModified) {
    if (it.kind == StandardWatchEventKinds.ENTRY_MODIFY) {
        if (it.paths == "config_chat.yaml") {
            val config = Config.load("config_chat", ChatConfig.serializer(), ChatConfig())
            require(config != null) {
                Log.err(Bundle()["event.plugin.load.failed"])
                return
            }
            conf = config
            Log.info(Bundle()["config.reloaded"])
        }
    }
}