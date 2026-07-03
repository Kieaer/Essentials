package essential.core.service.web

import arc.util.Log
import essential.core.service.web.WebService.Companion.bundle
import essential.core.service.web.WebService.Companion.conf
import essential.core.service.web.achievement.AchievementController
import essential.core.service.web.achievement.achievementRoutes
import essential.core.service.web.auth.AuthController
import essential.core.service.web.auth.UserSession
import essential.core.service.web.auth.authRoutes
import essential.core.service.web.maps.MapController
import essential.core.service.web.maps.mapRoutes
import essential.core.service.web.statistics.StatisticsController
import essential.core.service.web.statistics.statisticsRoutes
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.json.Json
import java.net.ServerSocket
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration.Companion.minutes

class WebServer {
    lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    var boundPort: Int = 0

    val authController = AuthController()
    val mapController = MapController()
    val achievementController = AchievementController()
    val statisticsController = StatisticsController()

    // Configure the application module
    private fun configureModule(application: Application) {
        with(application) {
            // Initialize controllers
            mapController.init(this)
            statisticsController.init(this)

            // Install necessary plugins
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }

            install(Sessions) {
                cookie<UserSession>("USER_SESSION") {
                    cookie.path = "/"
                    cookie.maxAgeInSeconds = conf.sessionDuration
                    cookie.secure = false

                    // Create encryption key from session secret
                    val encryptionKey = hex(conf.sessionSecret)
                    transform(
                        SessionTransportTransformerEncrypt(
                            SecretKeySpec(encryptionKey, "AES"),
                            SecretKeySpec(encryptionKey, "HmacSHA256")
                        )
                    )
                }
            }

            install(Authentication) {
                session<UserSession>("auth-session") {
                    validate { session ->
                        session
                    }
                    challenge {
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                }
            }

            install(RateLimit) {
                register {
                    rateLimiter(limit = 300, refillPeriod = 1.minutes)
                    requestKey { call ->
                        call.request.local.remoteAddress
                    }
                }
            }

            // Configure routing
            routing {
                staticResources("/", "/web")

                rateLimit {
                    authRoutes(authController)
                    mapRoutes(mapController)
                    achievementRoutes(achievementController)
                    statisticsRoutes(statisticsController)
                }
            }
        }
    }

    private fun hex(key: String): ByteArray {
        val result = ByteArray(16)
        val keyBytes = key.toByteArray()

        for (i in result.indices) {
            result[i] = if (i < keyBytes.size) keyBytes[i] else 0
        }

        return result
    }

    fun start() = synchronized(this@WebServer) {
        val isTest = try {
            Class.forName("org.junit.Test")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

        boundPort = if (isTest) {
            try {
                ServerSocket(0).use { it.localPort }
            } catch (e: Exception) {
                (45000..60000).random()
            }
        } else {
            conf.port
        }

        // Create the server with a regular function
        server = embeddedServer(
            factory = Netty, 
            port = boundPort
        ) { 
            configureModule(this)
        }

        server.start(false)
        Log.info(bundle["web.server.started", boundPort.toString()])
    }

    fun stop() {
        if (::server.isInitialized) {
            server.stop(1000, 2000)
            Log.info(bundle["web.server.stopped"])
        }
    }
}
