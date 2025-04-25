package essential.bridge

import arc.ApplicationListener

class Main : Plugin() {
    var daemon: java.util.concurrent.ExecutorService = java.util.concurrent.Executors.newSingleThreadExecutor()
    public override fun init() {
        essential.bridge.Main.Companion.bundle.setPrefix("[EssentialBridge]")

        Log.debug(essential.bridge.Main.Companion.bundle.get("event.plugin.starting"))

        // 플러그인 설정
        essential.bridge.Main.Companion.conf = essential.core.Main.Companion.createAndReadConfig(
            "config_bridge.yaml",
            java.util.Objects.requireNonNull<T?>(this.javaClass.getResourceAsStream("/config_bridge.yaml")),
            essential.bridge.Config::class.java
        )

        // 서버간 연결할 포트 생성
        try {
            java.net.ServerSocket(essential.bridge.Main.Companion.conf!!.port).use { serverSocket ->
                essential.bridge.Main.Companion.isServerMode = true
                essential.bridge.Main.Companion.network = essential.bridge.Server()
            }
        } catch (e: java.io.IOException) {
            essential.bridge.Main.Companion.isServerMode = false
            essential.bridge.Main.Companion.network = essential.bridge.Client()
        }
        daemon.submit(essential.bridge.Main.Companion.network)

        Core.app.addListener(object : ApplicationListener() {
            public override fun dispose() {
                if (essential.bridge.Main.Companion.isServerMode) {
                    for (socket in (essential.bridge.Main.Companion.network as essential.bridge.Server).clients) {
                        try {
                            socket.close()
                        } catch (ignored: java.io.IOException) {
                        }
                    }
                    (essential.bridge.Main.Companion.network as essential.bridge.Server).shutdown()
                } else {
                    (essential.bridge.Main.Companion.network as essential.bridge.Client).send("exit")
                }
                daemon.shutdown()
            }
        })

        // 이벤트 실행
        val event = essential.bridge.Event()
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

        Log.debug(essential.bridge.Main.Companion.bundle.get("event.plugin.loaded"))
    }

    public override fun registerServerCommands(handler: CommandHandler) {
        val commands = essential.bridge.Commands()

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
        val commands = essential.bridge.Commands()
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
        var isServerMode: kotlin.Boolean = false
        var conf: essential.bridge.Config? = null
        var network: java.lang.Runnable? = null
    }
}
