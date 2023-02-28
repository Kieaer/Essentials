package essentials

import arc.util.Log
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.serialization.jackson.*
import io.ktor.server.routing.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

object WebServer {
    fun start() {
        Log.info("[Essentials][WebServer] Loading")
        embeddedServer(Netty, port = 80) {
            install(ContentNegotiation) {
                jackson { }
            }

            routing {
                route("/api/ranking") {
                    get {
                        val set = HashSet<HashMap<String, Any>>()

                        val playerData = Main.database.getAllByExp().take(30)
                        for (i in 0..(playerData.size - 1)) {
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
        }.start(wait = false);
        Log.info("[Essentials][WebServer] Loaded")
    }
}