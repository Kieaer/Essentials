package essential.achievements

import arc.util.CommandHandler

class Main : Plugin() {
    public override fun init() {
        essential.achievements.Main.Companion.bundle.setPrefix("[EssentialAchievements]")
        Log.debug(essential.achievements.Main.Companion.bundle.get("event.plugin.starting"))

        /*conf = essential.core.Main.Companion.createAndReadConfig(
                "config_achievements.yaml",
                Objects.requireNonNull(this.getClass().getResourceAsStream("/config_achievements.yaml")),
                Config.class
        );
*/
        // 이벤트 실행
        val event = essential.achievements.Event()
        val methods = event.javaClass.getDeclaredMethods()
        for (method in methods) {
            val annotation: essential.core.annotation.Event? =
                method.getAnnotation<T?>(essential.core.annotation.Event::class.java)
            if (annotation != null) {
                try {
                    method.invoke(event)
                } catch (e: java.lang.IllegalAccessException) {
                    throw java.lang.RuntimeException(e)
                } catch (e: java.lang.reflect.InvocationTargetException) {
                    throw java.lang.RuntimeException(e)
                }
            }
        }

        Log.debug(essential.achievements.Main.Companion.bundle.get("event.plugin.loaded"))
    }

    public override fun registerServerCommands(handler: CommandHandler) {
        val commands = essential.achievements.Commands()

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
        val commands = essential.achievements.Commands()
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
        var conf: essential.achievements.Config? = null
    }
}
