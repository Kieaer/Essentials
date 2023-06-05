package essentials

import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlin.time.Duration.Companion.seconds

class WebServer {
    lateinit var server : NettyApplicationEngine

    fun start() {
        server = embeddedServer(Netty, Config.webServerPort) {
            install(ContentNegotiation) {
                jackson()
            }
            install(RateLimit) {
                register {
                    rateLimiter(limit = Config.restAPIRequestsLimit, refillPeriod = Config.restAPILimitRefillPeriod.seconds)
                }
            }

            routing {
                rateLimit {
                    route("/api/ranking") {
                        get {
                            val set = HashSet<HashMap<String, Any>>()

                            val playerData = Main.database.getAllByExp().take(50)
                            for(i in playerData.indices) {
                                val map = HashMap<String, Any>()
                                map["rank"] = i + 1
                                map["username"] = playerData[i].name
                                map["level"] = playerData[i].level
                                map["exp"] = playerData[i].exp
                                map["playtime"] = playerData[i].totalPlayTime
                                val stat = HashMap<String, Any>()
                                stat["attackclear"] = playerData[i].attackModeClear
                                stat["pvpwin"] = playerData[i].pvpVictoriesCount
                                stat["pvplose"] = playerData[i].pvpDefeatCount
                                map["stat"] = stat

                                set.add(map)
                            }

                            call.respond(mapOf("data" to set))
                        }
                    }
                }

                singlePageApplication {
                    useResources = true
                    filesPath = "www"
                    defaultPage = "index.html"
                }
            }
        }
        server.start(false)
    }

    fun stop() {
        if(::server.isInitialized) server.stop()
    }
}