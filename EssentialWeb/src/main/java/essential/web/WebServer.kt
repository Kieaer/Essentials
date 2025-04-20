package essential.web

import essential.core.Main.Companion.database
import essential.web.Main.conf
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
    lateinit var server : EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>

    fun start() {
        server = embeddedServer(Netty, conf.port) {
            extracted()
        }
        server.start(false)
    }

    private fun Application.extracted() {
        install(ContentNegotiation) {
            jackson()
        }
        install(RateLimit) {
            register {
                rateLimiter(limit = 3, refillPeriod = 60.seconds)
            }
        }

        routing {
            rateLimit {
                route("/api/ranking") {
                    get {
                        val set = HashSet<HashMap<String, Any>>()

                        val playerData = database.getAllByExp().take(50)
                        playerData.indices.forEach {
                            val map = HashMap<String, Any>()
                            map["rank"] = it + 1
                            map["username"] = playerData[it].name
                            map["level"] = playerData[it].level
                            map["exp"] = playerData[it].exp
                            map["playtime"] = playerData[it].totalPlayTime
                            val stat = HashMap<String, Any>()
                            stat["attackclear"] = playerData[it].attackModeClear
                            stat["pvpwin"] = playerData[it].pvpVictoriesCount
                            stat["pvplose"] = playerData[it].pvpDefeatCount
                            map["stat"] = stat

                            set.add(map)
                        }

                        call.respond(mapOf("data" to set))
                    }
                }
            }

            singlePageApplication {
                useResources = true
                filesPath = "webTest"
                defaultPage = "index.html"
            }
        }
    }

    fun stop() {
        if (::server.isInitialized) server.stop()
    }
}