package essential.discord

import essential.core.Bundle

class Main : mindustry.mod.Plugin() {
    override fun init() {
        essential.discord.Main.Companion.bundle.setPrefix("[EssentialDiscord]")

        arc.util.Log.debug(essential.discord.Main.Companion.bundle.get("event.plugin.starting"))

        // 플러그인 설정
        essential.discord.Main.Companion.conf = essential.core.Main.Companion.createAndReadConfig(
            "config_discord.yaml",
            java.util.Objects.requireNonNull<T?>(this.javaClass.getResourceAsStream("/config_discord.yaml")),
            essential.discord.Config::class.java
        )

        if (!essential.discord.Main.Companion.conf!!.getUrl()
                .isEmpty() && !essential.discord.Main.Companion.conf!!.getUrl()
                .matches("https://discord\\.gg/[a-zA-Z0-9]{1,16}".toRegex())
        ) {
            arc.util.Log.warn(essential.discord.Main.Companion.bundle.get("config.invalid.url"))
        }

        arc.Events.on<ConfigFileModified?>(ConfigFileModified::class.java, arc.func.Cons { e: ConfigFileModified? ->
            if (e.getKind() === java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY) {
                if (e.getPaths().equals("config_discord.yaml")) {
                    essential.discord.Main.Companion.conf = essential.core.Main.Companion.createAndReadConfig(
                        "config_discord.yaml",
                        java.util.Objects.requireNonNull<T?>(this.javaClass.getResourceAsStream("/config_discord.yaml")),
                        essential.discord.Config::class.java
                    )
                    arc.util.Log.info(Bundle().get("config.reloaded"))
                }
            }
        })

        arc.util.Log.debug(essential.discord.Main.Companion.bundle.get("event.plugin.loaded"))
    }

    override fun registerServerCommands(handler: CommandHandler) {
        val commands = essential.discord.Commands()

        val methods = commands.javaClass.getDeclaredMethods()

        for (method in methods) {
            val annotation: ServerCommand? = method.getAnnotation<T?>(ServerCommand::class.java)
            if (annotation != null) {
                handler.register(
                    annotation.name(),
                    annotation.parameter(),
                    annotation.description(),
                    arc.func.Cons { args: kotlin.Array<kotlin.String?>? ->
                        if (args!!.size > 0) {
                            try {
                                method.invoke(commands, *kotlin.arrayOf<kotlin.Any>(args))
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }
                        } else {
                            try {
                                method.invoke(commands, *kotlin.arrayOf<kotlin.Any>(kotlin.arrayOf<kotlin.String?>()))
                            } catch (e: java.lang.Exception) {
                                java.lang.System.err.println("arg size - " + args.size)
                                java.lang.System.err.println("command - " + annotation.name())
                                e.printStackTrace()
                            }
                        }
                    })
            }
        }
    }

    override fun registerClientCommands(handler: CommandHandler) {
        val commands = essential.discord.Commands()
        val methods = commands.javaClass.getDeclaredMethods()

        for (method in methods) {
            val annotation: ClientCommand? = method.getAnnotation<T?>(ClientCommand::class.java)
            if (annotation != null) {
                handler.register<Player?>(
                    annotation.name(),
                    annotation.parameter(),
                    annotation.description(),
                    CommandRunner { args: kotlin.Array<kotlin.String?>?, player: Player? ->
                        var data: PlayerData? = findPlayerByUuid(player.uuid())
                        if (data == null) {
                            data = PlayerData()
                        }
                        if (Permission.INSTANCE.check(data, annotation.name())) {
                            try {
                                if (args!!.size > 0) {
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
        var conf: essential.discord.Config? = null
    }
}
