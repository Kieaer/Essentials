package essentials

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

class WebServer: Runnable {
    lateinit var server : NettyApplicationEngine

    override fun run() {
        server = embeddedServer(Netty, Config.webServerPort) {
            install(ContentNegotiation) {
                jackson { }
            }

            routing {
                route("/api/ranking") {
                    get {
                        val set = HashSet<HashMap<String, Any>>()

                        val playerData = Main.database.getAllByExp().take(30)
                        for (i in playerData.indices) {
                            val map = HashMap<String, Any>()
                            map["rank"] = i + 1
                            map["username"] = playerData[i].name
                            map["level"] = playerData[i].level
                            map["exp"] = playerData[i].exp
                            map["playtime"] = playerData[i].playtime
                            val stat = HashMap<String, Any>()
                            stat["attackclear"] = playerData[i].attackclear
                            stat["pvpwin"] = playerData[i].pvpwincount
                            stat["pvplose"] = playerData[i].pvplosecount
                            map["stat"] = stat

                            set.add(map)
                        }

                        call.respond(mapOf("data" to set))
                    }
                }

                singlePageApplication {
                    useResources = true
                    filesPath = "www"
                    defaultPage = "index.html"
                }
            }
        }
        server.start(true)
    }

    fun stop() {
        if(::server.isInitialized) server.stop()
    }
}