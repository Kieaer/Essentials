package essential.core.service.web

import arc.util.Log
import essential.common.database.data.getPlayerDataByName
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
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
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
        require(conf.sessionSecret.length >= 32) {
            "Web sessions require a configured sessionSecret of at least 32 characters"
        }
        val (encryptionKey, signingKey) = sessionKeys(conf.sessionSecret)

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
                    cookie.secure = conf.secureCookie
                    cookie.httpOnly = true

                    transform(
                        SessionTransportTransformerEncrypt(
                            encryptionKey,
                            signingKey
                        )
                    )
                }
            }

            install(Authentication) {
                session<UserSession>("auth-session") {
                    validate { session ->
                        getPlayerDataByName(session.username)
                            ?.takeIf { it.id.toString() == session.id }
                            ?.let { session }
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

    private fun sessionKeys(secret: String): Pair<SecretKeySpec, SecretKeySpec> {
        val digest = MessageDigest.getInstance("SHA-512")
            .digest(secret.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(digest.copyOfRange(0, 32), "AES") to
            SecretKeySpec(digest.copyOfRange(32, 64), "HmacSHA256")
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
