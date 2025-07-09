package essential.web

import arc.Core
import arc.Events
import arc.files.Fi
import arc.util.Log
import essential.database.data.getPlayerDataByName
import essential.web.Main.Companion.bundle
import essential.web.Main.Companion.conf
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.server.websocket.*
import io.ktor.utils.io.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.io.SaveIO
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.mindrot.jbcrypt.BCrypt
import java.io.File
import java.nio.file.Files
import java.util.*
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration.Companion.seconds

class WebServer {
    lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    private val chatHistory = Collections.synchronizedList(mutableListOf<ChatMessage>())

    @Serializable
    data class UserSession(val id: String, val username: String)

    @Serializable
    data class LoginRequest(val username: String, val password: String)

    @Serializable
    data class MapInfo(
        val name: String,
        val author: String,
        val description: String,
        val planet: String,
        val preview: String? = null
    )

    @Serializable
    data class ServerStatus(
        val map: String,
        val players: List<String>,
        val tps: Float,
        val wave: Int,
        val gameTime: Float
    )

    @Serializable
    data class ChatMessage(
        val player: String,
        val message: String,
        val time: Long = System.currentTimeMillis()
    )

    fun start(jdbcUrl: String, user: String, pass: String) = synchronized(this@WebServer) {
        // Create upload directory if it doesn't exist
        val uploadDir = File(conf.uploadPath)
        if (!uploadDir.exists()) {
            uploadDir.mkdirs()
        }

        // Use the common database configuration function
        essential.database.setDatabaseConfig(jdbcUrl, user, pass)

        // Ensure database connection is established in this module's classloader
        essential.database.connectToDatabase()

        server = embeddedServer(Netty, conf.port) {
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

            install(WebSockets) {
                pingPeriod = 15.seconds
                timeout = 30.seconds
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }

            // Configure routing
            routing {
                staticResources("/", "/web")

                // Authentication routes
                route("/api/auth") {
                    post("/login") {
                        val loginRequest = call.receive<LoginRequest>()
                        handleLogin(call, loginRequest)
                    }

                    get("/logout") {
                        call.sessions.clear<UserSession>()
                        call.respond(HttpStatusCode.OK)
                    }

                    get("/status") {
                        val session = call.sessions.get<UserSession>()
                        if (session != null) {
                            call.respond(mapOf("username" to session.username))
                        } else {
                            call.respond(HttpStatusCode.Unauthorized)
                        }
                    }
                }

                // Map management routes
                route("/api/maps") {
                    authenticate("auth-session") {
                        get {
                            val maps = getMaps()
                            call.respond(maps)
                        }

                        post("/upload") {
                            handleMapUpload(call)
                        }

                        get("/download/{name}") {
                            val mapName = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                            handleMapDownload(call, mapName)
                        }
                    }
                }

                // Server status routes
                route("/api/server") {
                    authenticate("auth-session") {
                        get("/status") {
                            val status = getServerStatus()
                            call.respond(status)
                        }

                        get("/chat") {
                            call.respond(chatHistory)
                        }

                        post("/chat") {
                            val session = call.sessions.get<UserSession>()
                                ?: return@post call.respond(HttpStatusCode.Unauthorized)
                            val message = call.receiveText()

                            // Validate chat message
                            if (message.isBlank() || message.length > 100) {
                                return@post call.respond(HttpStatusCode.BadRequest, "Invalid message")
                            }

                            // Sanitize message to prevent code injection
                            val sanitizedMessage = sanitizeMessage(message)

                            // Send message to server
                            Call.sendMessage("[cyan]<WEB>[white] ${session.username}: $sanitizedMessage")

                            // Add to chat history
                            val chatMessage = ChatMessage(session.username, sanitizedMessage)
                            chatHistory.add(chatMessage)
                            if (chatHistory.size > 100) {
                                chatHistory.removeAt(0)
                            }

                            call.respond(HttpStatusCode.OK)
                        }
                    }
                }

                // WebSocket for live monitoring
                authenticate("auth-session") {
                    webSocket("/api/server/live") {
                        val session = call.sessions.get<UserSession>()
                            ?: return@webSocket close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "Unauthorized"))

                        try {
                            // Send initial status
                            val initialStatus = getServerStatus()
                            val initialJson = Json.encodeToString(ServerStatus.serializer(), initialStatus)
                            outgoing.send(Frame.Text(initialJson))

                            // Start periodic updates
                            launch {
                                while (true) {
                                    delay(1000) // Update every second
                                    val status = getServerStatus()
                                    val json = Json.encodeToString(ServerStatus.serializer(), status)
                                    outgoing.send(Frame.Text(json))
                                }
                            }

                            // Handle incoming messages
                            for (frame in incoming) {
                                when (frame) {
                                    is Frame.Text -> {
                                        val text = frame.readText()
                                        // Process commands if needed
                                    }

                                    else -> {}
                                }
                            }
                        } catch (e: ClosedReceiveChannelException) {
                            Log.debug("WebSocket closed: ${e.message}")
                        } catch (e: Throwable) {
                            Log.err("WebSocket error", e)
                        }
                    }
                }
            }
        }

        Events.on(EventType.PlayerChatEvent::class.java) { event ->
            val player = event.player
            val message = event.message

            // Add the chat message to the chat history
            val chatMessage = ChatMessage(player.name(), message)
            chatHistory.add(chatMessage)
            if (chatHistory.size > 100) {
                chatHistory.removeAt(0)
            }

            Log.debug("Chat message added to history: ${player.name()}: $message")
        }

        server.start(false)
        Log.info(bundle["web.server.started", conf.port])
    }

    fun stop() {
        if (::server.isInitialized) {
            server.stop(1000, 2000)
            Log.info(bundle["web.server.stopped"])
        }
    }

    private suspend fun handleLogin(call: ApplicationCall, request: LoginRequest) {
        try {
            val playerData = getPlayerDataByName(request.username)

            if (playerData == null) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid username or password")
                return
            }

            // Check if account ID and password are set
            if (playerData.accountID == null || playerData.accountPW == null) {
                call.respond(HttpStatusCode.Unauthorized, "Account not set up")
                return
            }

            // Check if account ID and password are the same
            if (playerData.accountID == request.password) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    "Your username and password are the same. Please change your password."
                )
                return
            }

            // Check if Discord ID is set
            if (playerData.discordID == null) {
                call.respond(
                    HttpStatusCode.Forbidden,
                    mapOf("message" to "Please link your Discord account first", "discordUrl" to conf.discordUrl)
                )
                return
            }

            // Verify password using BCrypt
            val passwordMatches = newSuspendedTransaction {
                val storedHash = playerData.accountPW
                if (storedHash != null) {
                    BCrypt.checkpw(request.password, storedHash)
                } else {
                    false
                }
            }

            if (!passwordMatches) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid username or password")
                return
            }

            // Create session
            val session = UserSession(playerData.id.toString(), playerData.name)
            call.sessions.set(session)

            call.respond(HttpStatusCode.OK, mapOf("username" to playerData.name))
        } catch (e: Exception) {
            Log.err("Login error", e)
            call.respond(HttpStatusCode.InternalServerError, "An error occurred during login")
        }
    }

    private suspend fun handleMapUpload(call: ApplicationCall) {
        val multipart = call.receiveMultipart()
        var fileName = ""
        var fileBytes: ByteArray? = null

        multipart.forEachPart { part ->
            when (part) {
                is PartData.FileItem -> {
                    fileName = part.originalFileName ?: "unknown.msav"
                    fileBytes = part.provider().readRemaining().readByteArray()
                }

                else -> {}
            }
            part.dispose()
        }

        if (fileBytes == null) {
            call.respond(HttpStatusCode.BadRequest, "No file uploaded")
            return
        }

        // Check file size
        if (fileBytes.size > conf.maxFileSize) {
            call.respond(HttpStatusCode.BadRequest, "File too large")
            return
        }

        // Validate file extension
        if (!fileName.endsWith(".msav")) {
            call.respond(HttpStatusCode.BadRequest, "Invalid file type. Only .msav files are allowed")
            return
        }

        // Create temporary file for validation
        val tempFile = File.createTempFile("map_", ".msav")
        tempFile.writeBytes(fileBytes)

        // Validate map file without affecting the current game state
        try {
            // Try to load the map to validate it
            SaveIO.load(Fi(tempFile.absolutePath))

            // Save the file
            val targetFile = File(conf.uploadPath, fileName)
            Files.copy(tempFile.toPath(), targetFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            tempFile.delete()

            // Reload maps
            Core.app.post {
                Vars.maps.reload()
                Log.info("Maps reloaded after upload: $fileName")
            }

            call.respond(HttpStatusCode.OK, "Map uploaded successfully")
        } catch (e: Exception) {
            tempFile.delete()
            Log.err("Map validation error", e)
            call.respond(HttpStatusCode.BadRequest, "Invalid map file: ${e.message}")
        }
    }

    private suspend fun handleMapDownload(call: ApplicationCall, mapName: String) {
        val map = Vars.maps.all().find { it.name() == mapName }
        if (map == null) {
            call.respond(HttpStatusCode.NotFound, "Map not found")
            return
        }

        val file = File(map.file.absolutePath())
        if (!file.exists()) {
            call.respond(HttpStatusCode.NotFound, "Map file not found")
            return
        }

        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "${map.name()}.msav")
                .toString()
        )
        call.respondFile(file)
    }

    private fun getMaps(): List<MapInfo> {
        val mapsList = mutableListOf<MapInfo>()
        Vars.maps.all().forEach { map ->
            mapsList.add(
                MapInfo(
                    name = map.name(),
                    author = map.author(),
                    description = map.description(),
                    planet = map.tags.get("planet", "serpulo"),
                    preview = null // We'll implement preview image generation later
                )
            )
        }
        return mapsList
    }

    private fun getServerStatus(): ServerStatus {
        val playerNames = mutableListOf<String>()
        Groups.player.each { playerNames.add(it.name()) }

        return ServerStatus(
            map = Vars.state.map.name(),
            players = playerNames,
            tps = Core.graphics.framesPerSecond.toFloat(), // Use FPS as TPS
            wave = Vars.state.wave,
            gameTime = Vars.state.wavetime / 60f // Use wave time as game time (in minutes)
        )
    }

    private fun sanitizeMessage(message: String): String {
        // Remove potentially dangerous characters and HTML tags
        return message
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("&", "&amp;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
    }

    private fun hex(key: String): ByteArray {
        val result = ByteArray(16) // Use 16 bytes (256 bits) for AES-256
        val keyBytes = key.toByteArray()

        // Copy key bytes or pad with zeros
        for (i in result.indices) {
            result[i] = if (i < keyBytes.size) keyBytes[i] else 0
        }

        return result
    }
}
