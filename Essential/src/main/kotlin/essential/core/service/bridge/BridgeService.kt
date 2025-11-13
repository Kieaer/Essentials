package essential.core.service.bridge

import arc.ApplicationListener
import arc.Core
import arc.util.CommandHandler
import arc.util.Log
import essential.common.bundle.Bundle
import essential.common.config.Config
import essential.core.service.bridge.generated.registerGeneratedClientCommands
import mindustry.mod.Plugin
import java.io.IOException
import java.net.ServerSocket
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class BridgeService : Plugin() {
    companion object {
        // Note: bridge 전용 번들이 없으므로 공통 번들을 사용합니다.
        var bundle: Bundle = Bundle(ResourceBundle.getBundle("bundles/common/bundle"))
        var isServerMode: Boolean = false
        lateinit var conf: BridgeConfig
        lateinit var network: Runnable
    }

    var daemon: ExecutorService = Executors.newSingleThreadExecutor()
    override fun init() {
        bundle.prefix = "[EssentialBridge]"

        // 플러그인 설정
        val config = Config.load("config_bridge", BridgeConfig.serializer(), BridgeConfig())
        require(config != null) {
            Log.err(bundle["event.plugin.load.failed"])
            return
        }

        conf = config

        // 서버간 연결할 포트 생성
        try {
            ServerSocket(conf.port).use {
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
    }

    override fun registerClientCommands(handler: CommandHandler) {
        registerGeneratedClientCommands(handler)
    }
}
