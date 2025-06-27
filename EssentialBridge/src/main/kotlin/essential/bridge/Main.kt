package essential.bridge

import arc.ApplicationListener
import arc.Core
import arc.util.CommandHandler
import arc.util.Log
import essential.bridge.generated.registerGeneratedClientCommands
import essential.bridge.generated.registerGeneratedEventHandlers
import essential.bundle.Bundle
import essential.config.Config
import mindustry.mod.Plugin

class Main : Plugin() {
    var daemon: java.util.concurrent.ExecutorService = java.util.concurrent.Executors.newSingleThreadExecutor()
    override fun init() {
        bundle.prefix = "[EssentialBridge]"

        Log.debug(bundle["event.plugin.starting"])

        // 플러그인 설정
        val config = Config.load("config_bridge.yaml", BridgeConfig.serializer(), BridgeConfig())
        require(config != null) {
            Log.err(bundle["event.plugin.load.failed"])
            return
        }

        conf = config

        // 서버간 연결할 포트 생성
        try {
            java.net.ServerSocket(conf.port).use { serverSocket ->
                isServerMode = true
                network = Server()
            }
        } catch (_: java.io.IOException) {
            isServerMode = false
            network = Client()
        }
        daemon.submit(network)

        Core.app.addListener(object : ApplicationListener {
            override fun dispose() {
                if (isServerMode) {
                    for (socket in (network as Server).clients) {
                        try {
                            socket.close()
                        } catch (_: java.io.IOException) {
                        }
                    }
                    (network as Server).shutdown()
                } else {
                    (network as Client).send("exit")
                }
                daemon.shutdown()
            }
        })

        // 이벤트 실행
        registerGeneratedEventHandlers()

        Log.debug(bundle["event.plugin.loaded"])
    }

    override fun registerClientCommands(handler: CommandHandler) {
        registerGeneratedClientCommands(handler)
    }

    companion object {
        var bundle: Bundle = Bundle()
        var isServerMode: kotlin.Boolean = false
        lateinit var conf: BridgeConfig
        lateinit var network: Runnable
    }
}
