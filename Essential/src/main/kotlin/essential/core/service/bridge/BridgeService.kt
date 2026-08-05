package essential.core.service.bridge

import arc.ApplicationListener
import arc.Core
import arc.util.CommandHandler
import arc.util.Log
import essential.common.bundle.Bundle
import essential.common.config.Config
import essential.core.service.bridge.generated.registerGeneratedClientCommands
import kotlinx.coroutines.runBlocking
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
        var isRunning: Boolean = false
        var conf: BridgeConfig = reloadConf()

        fun reloadConf() : BridgeConfig {
            return runBlocking {
                val config = Config.load("config_bridge", BridgeConfig.serializer(), BridgeConfig())
                require(config != null) {
                    Log.err(bundle["event.plugin.load.failed"])
                }
                config
            }
        }

        var network: Runnable? = null
    }

    var daemon: ExecutorService = Executors.newSingleThreadExecutor()
    override fun init() {
        bundle.prefix = "[EssentialBridge]"

        if (!hasValidBridgeSecret(conf.sharedSecret)) {
            Log.warn("Bridge is disabled: configure a sharedSecret of at least 32 UTF-8 bytes on every bridge server")
            return
        }

        val server = Server.bind(conf.port)
        network = if (server != null) {
            isServerMode = true
            server
        } else {
            isServerMode = false
            Client()
        }
        isRunning = true
        daemon.submit(requireNotNull(network))

        Core.app.addListener(object : ApplicationListener {
            override fun dispose() {
                val activeNetwork = network
                if (isServerMode && activeNetwork is Server) {
                    for (socket in activeNetwork.clients) {
                        try {
                            socket.close()
                        } catch (_: IOException) {
                        }
                    }
                    activeNetwork.shutdown()
                } else if (activeNetwork is Client) {
                    activeNetwork.send("exit")
                    activeNetwork.cancel()
                }
                isRunning = false
                daemon.shutdownNow()
            }
        })
    }

    override fun registerClientCommands(handler: CommandHandler) {
        registerGeneratedClientCommands(handler)
    }
}
