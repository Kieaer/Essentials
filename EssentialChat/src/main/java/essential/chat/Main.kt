package essential.chat

import arc.util.CommandHandler

class Main : Plugin() {
    public override fun init() {
        essential.chat.Main.Companion.bundle.setPrefix("[EssentialChat]")

        Log.debug(essential.chat.Main.Companion.bundle.get("event.plugin.starting"))

        essential.chat.Main.Companion.conf = essential.core.Main.Companion.createAndReadConfig(
            "config_chat.yaml",
            java.util.Objects.requireNonNull<T?>(this.javaClass.getResourceAsStream("/config_chat.yaml")),
            essential.chat.Config::class.java
        )

        // 이벤트 등록
        essential.chat.Event().load()

        // 채팅 금지어 파일 추가
        if (!root.child("chat_blacklist.txt").exists()) {
            root.child("chat_blacklist.txt").writeString("않")
        }

        Log.debug(essential.chat.Main.Companion.bundle.get("event.plugin.loaded"))
    }

    public override fun registerServerCommands(handler: CommandHandler) {
        val commands = essential.chat.Commands()

        val methods = commands.javaClass.getDeclaredMethods()

        for (method in methods) {
            val annotation: ServerCommand? = method.getAnnotation<T?>(ServerCommand::class.java)
            if (annotation != null) {
                handler.register(annotation.name(), annotation.parameter(), annotation.description(), { args ->
                    if (args.length > 0) {
                        try {
                            method.invoke(commands, *kotlin.arrayOf<kotlin.Any>(args))
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        try {
                            method.invoke(commands, *kotlin.arrayOf<kotlin.Any>(kotlin.arrayOf<kotlin.String?>()))
                        } catch (e: java.lang.Exception) {
                            java.lang.System.err.println("arg size - " + args.length)
                            java.lang.System.err.println("command - " + annotation.name())
                            e.printStackTrace()
                        }
                    }
                })
            }
        }
    }

    public override fun registerClientCommands(handler: CommandHandler) {
        val commands = essential.chat.Commands()
        val methods = commands.javaClass.getDeclaredMethods()

        for (method in methods) {
            val annotation: ClientCommand? = method.getAnnotation<T?>(ClientCommand::class.java)
            if (annotation != null) {
                handler.< Player > register < Player ? > (annotation.name(), annotation.parameter(), annotation.description(), { args, player ->
                    var data: PlayerData? = findPlayerByUuid(player.uuid())
                    if (data == null) {
                        data = PlayerData()
                    }
                    if (Permission.INSTANCE.check(data, annotation.name())) {
                        try {
                            if (args.length > 0) {
                                method.invoke(commands, player, data, args)
                            } else {
                                method.invoke(commands, player, data, kotlin.arrayOf<kotlin.String?>())
                            }
                        } catch (e: java.lang.Exception) {
                            e.printStackTrace()
                        }
                    } else {
                        data.send("command.permission.false")
                    }
                })
            }
        }
    }

    fun findPlayerByUuid(uuid: kotlin.String?): PlayerData {
        return database.getPlayers().stream().filter({ e -> e.getUuid().equals(uuid) }).findFirst().orElse(null)
    }

    companion object {
        var bundle: Bundle = Bundle()
        var conf: essential.chat.Config? = null
    }
}
