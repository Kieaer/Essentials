package essential.chat

import arc.util.Log
import com.github.pemistahl.lingua.api.IsoCode639_1
import com.github.pemistahl.lingua.api.Language
import com.github.pemistahl.lingua.api.Language.Companion.getByIsoCode639_1
import com.github.pemistahl.lingua.api.LanguageDetector
import com.github.pemistahl.lingua.api.LanguageDetectorBuilder
import essential.bundle.Bundle
import essential.chat.Main.Companion.conf
import essential.config.Config
import essential.database.data.PlayerDataEntity
import essential.event.CustomEvents
import essential.isVoting
import essential.permission.Permission
import essential.rootPath
import essential.util.findPlayerData
import ksp.event.Event
import mindustry.Vars
import mindustry.core.NetServer
import mindustry.game.EventType
import mindustry.gen.Player
import mindustry.net.Administration
import java.util.regex.Pattern

var detector: LanguageDetector? = null

fun loadDetector() {
    val configs =
        conf.strict.language.split(",".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
    val languages = ArrayList<Language>()
    for (a in configs) {
        languages.add(getByIsoCode639_1(IsoCode639_1.valueOf(a.uppercase(java.util.Locale.getDefault()))))
    }

    detector = LanguageDetectorBuilder.fromLanguages(*languages.toTypedArray()).build()
}

fun load() {
    if (conf.strict.enabled) {
        loadDetector()
    }

    Vars.netServer.admins.addChatFilter(object : Administration.ChatFilter {
        private val specificTextRegex: Pattern =
            Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]")

        override fun filter(player: Player, message: String): String? {
            val bundle: Bundle = Bundle(player.locale)
            if (conf.strict.enabled) {
                val e = detector.detectLanguageOf(message)

                if (e == Language.UNKNOWN && !specificTextRegex.matcher(
                        message.substring(
                            0,
                            1
                        )
                    ).matches() && !(isVoting && message.equals("y", ignoreCase = true))
                ) {
                    player.sendMessage(bundle["event.chat.language.not.allow"])
                    return null
                }
            }

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
            val data: PlayerDataEntity? = findPlayerData(player.uuid())
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
    if (it.kind == java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY) {
        if (it.paths == "config_chat.yaml") {
            val config = Config.load("config_discord.yaml", ChatConfig.serializer(), true, ChatConfig())
            require(config != null) {
                Log.err(Bundle()["event.plugin.load.failed"])
                return
            }
            conf = config
            Log.info(Bundle()["config.reloaded"])
        }
    }
}