package essential.feature.bridge

import arc.ApplicationListener
import arc.Core
import arc.util.CommandHandler
import arc.util.Log
import essential.bridge.generated.registerGeneratedClientCommands
import essential.bridge.generated.registerGeneratedEventHandlers
import essential.common.bundle.Bundle
import essential.common.config.Config
import mindustry.mod.Plugin
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BridgeService : Plugin() {
    companion object {
        internal var bundle: Bundle = Bundle()
        internal var isServerMode: Boolean = false
        internal lateinit var conf: BridgeConfig
        internal lateinit var network: Runnable
    }

    var daemon: ExecutorService = Executors.newSingleThreadExecutor()
    override fun init() {
        bundle.prefix = "[EssentialBridge]"

        Log.debug(bundle["event.plugin.starting"])

        // 플러그인 설정
        val config = Config.load("config_bridge", BridgeConfig.serializer(), BridgeConfig())
        require(config != null) {
            Log.err(bundle["event.plugin.load.failed"])
            return
        }

        conf = config

        // 서버간 연결할 포트 생성
        try {
            ServerSocket(conf.port).use { serverSocket ->
                isServerMode = true
                network = Server()
            }
        } catch (_: IOException) {
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
                        } catch (_: IOException) {
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
}
