package www.frontend

import arc.util.Log
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*

object WebServer {
    fun start() {
        Log.info("[Essentials][WebServer] Loading")
        embeddedServer(Netty, port = 80) {
            routing {
                singlePageApplication {
                    useResources = true
                    filesPath = "www"
                    defaultPage = "index.html"
                }
            }
        }.start(wait = false);
        Log.info("[Essentials][WebServer] Loaded")
    }
}