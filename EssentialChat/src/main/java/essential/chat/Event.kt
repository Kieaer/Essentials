package essential.chat

import arc.Events

class Event {
    var detector: LanguageDetector? = null

    fun loadDetector() {
        val configs =
            essential.chat.Main.Companion.conf.strict.language.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                .toTypedArray()
        val languages = java.util.ArrayList<com.github.pemistahl.lingua.api.Language?>()
        for (a in configs) {
            languages.add(getByIsoCode639_1.getByIsoCode639_1(IsoCode639_1.valueOf(a.uppercase(java.util.Locale.getDefault()))))
        }

        detector =
            LanguageDetectorBuilder.fromLanguages(*languages.toTypedArray<com.github.pemistahl.lingua.api.Language?>())
                .build()
    }

    fun load() {
        if (essential.chat.Main.Companion.conf.strict.enabled) {
            loadDetector()
        }

        Vars.netServer.admins.addChatFilter(object : ChatFilter() {
            private val specificTextRegex: java.util.regex.Pattern =
                java.util.regex.Pattern.compile("[!@#$%&*()_+=|<>?{}\\[\\]~-]")

            public override fun filter(player: Player, message: kotlin.String): kotlin.String? {
                val bundle: Bundle = Bundle(player.locale)
                if (essential.chat.Main.Companion.conf.strict.enabled) {
                    val e = detector.detectLanguageOf(message)

                    if (e == com.github.pemistahl.lingua.api.Language.UNKNOWN && !specificTextRegex.matcher(
                            message.substring(
                                0,
                                1
                            )
                        ).matches() && !(pluginData.getVoting() && message.equals("y", ignoreCase = true))
                    ) {
                        player.sendMessage(bundle.get("event.chat.language.not.allow"))
                        return null
                    }
                }

                if (essential.chat.Main.Companion.conf.blacklist.enabled) {
                    val file: kotlin.Array<kotlin.String> =
                        root.child("chat_blacklist.txt").readString("UTF-8").split("\r\n")
                    for (text in file) {
                        if (essential.chat.Main.Companion.conf.blacklist.regex) {
                            if (java.util.regex.Pattern.compile(text).matcher(message).find()) {
                                player.sendMessage(bundle.get("event.chat.blacklisted"))
                                return null
                            }
                        } else {
                            if (message.contains(text)) {
                                player.sendMessage(bundle.get("event.chat.blacklisted"))
                                return null
                            }
                        }
                    }
                }

                return message
            }
        })

        Vars.netServer.chatFormatter = label@{ player, message ->
            if (player != null) {
                val data: PlayerData? = findPlayerData(player.uuid())
                if (message != null) {
                    val defaultFormat = "[coral][[" + player.coloredName() + "[coral]]:[white] " + message
                    if (data != null) {
                        val chatFormat: kotlin.String = Permission.INSTANCE.get(data).getChatFormat()
                        if (chatFormat.isEmpty()) {
                            return@label defaultFormat
                        } else {
                            return@label chatFormat
                                .replace("%1", player.coloredName())
                                .replace("%2", message)
                        }
                    } else {
                        return@label defaultFormat
                    }
                }
            }
            null
        }

        Events.on(ConfigFileModified::class.java, { e ->
            if (e.getKind() === java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY) {
                if (e.getPaths().equals("config_chat.yaml")) {
                    essential.chat.Main.Companion.conf = essential.core.Main.Companion.createAndReadConfig(
                        "config_chat.yaml",
                        java.util.Objects.requireNonNull<T?>(this.javaClass.getResourceAsStream("/config_chat.yaml")),
                        essential.chat.Config::class.java
                    )
                    Log.info(Bundle().get("config.reloaded"))
                }
            }
        })
    }

    fun findPlayerData(uuid: kotlin.String?): PlayerData? {
        for (data in database.getPlayers()) {
            if ((data.getOldUUID() != null && data.getOldUUID().equals(uuid)) || data.getUuid().equals(uuid)) {
                return data
            }
        }
        return null
    }
}
