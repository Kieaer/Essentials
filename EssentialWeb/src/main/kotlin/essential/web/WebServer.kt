package essential.web

import essential.web.Main.Companion.conf
import io.ktor.server.engine.*
import io.ktor.server.netty.*

class WebServer {
    lateinit var server : EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    fun start() {
        server = embeddedServer(Netty, conf.port) {

        }
        server.start(false)
    }

    fun stop() {
        if (::server.isInitialized) server.stop()
    }
}