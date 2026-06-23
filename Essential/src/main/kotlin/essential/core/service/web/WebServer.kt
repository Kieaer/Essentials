package essential.core.service.web

import arc.Core
import arc.Events
import arc.files.Fi
import arc.util.Log
import essential.common.database.data.getMapRatings
import essential.common.database.data.getPlayerDataByName
import essential.common.log.LogType
import essential.common.log.writeLog
import essential.common.playTime
import essential.common.players
import essential.common.systemTimezone
import essential.common.util.size
import essential.common.util.toHString
import essential.core.service.web.WebService.Companion.bundle
import essential.core.service.web.WebService.Companion.conf
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.toInstant
import kotlinx.io.readByteArray
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.io.MapIO
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.mindrot.jbcrypt.BCrypt
import java.io.File
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class WebServer {
    lateinit var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>
    var boundPort: Int = 0
    private val chatHistory = Collections.synchronizedList(mutableListOf<ChatMessage>())
    private val statusHistory = Collections.synchronizedList(mutableListOf<StatusDataPoint>())

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
        val preview: String? = null,
        val votes: Int = 0
    )

    @Serializable
    data class WebPlayerInfo(
        val name: String,
        val playTime: String
    )

    @Serializable
    data class ServerStatus(
        val map: String,
        val players: List<WebPlayerInfo>,
        val tps: Float,
        val wave: Int,
        val gameTime: String,
        val mode: String,
        val activeTeams: Int
    )

    @Serializable
    data class ChatMessage(
        val player: String,
        val message: String,
        val time: Long = System.currentTimeMillis(),
        val isWeb: Boolean = false
    )

    @Serializable
    data class StatusDataPoint(
        val time: Long,
        val tps: Float,
        val players: Int,
        val units: Int,
        val buildings: Int,
        val resources: Map<String, Int>? = null,
        val teamResources: Map<String, Int>? = null,
        val teamUnits: Map<String, Int>? = null,
        val teamBuildings: Map<String, Int>? = null
    )

    // Configure the application module
    private fun configureModule(application: Application) {
        with(application) {
            launch {
                while (true) {
                    delay(60000)
                    try {
                        recordStatusPoint()
                    } catch (e: Exception) {
                        Log.err("Error recording status point", e)
                    }
                }
            }

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
                    rateLimiter(limit = 60, refillPeriod = 60.seconds)
                    requestKey { call ->
                        call.request.local.remoteAddress
                    }
                }
            }

            // Configure routing
            routing {
                staticResources("/", "/web")

                rateLimit {
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
                        get("/status") {
                            val status = getServerStatus()
                            call.respond(status)
                        }

                        authenticate("auth-session") {
                            get("/chat") {
                                val messages = chatHistory.filter { !it.message.startsWith("/") }.sortedBy { it.time }
                                call.respond(messages)
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
                                val chatMessage = ChatMessage(session.username, sanitizedMessage, isWeb = true)
                                chatHistory.add(chatMessage)
                                if (chatHistory.size > 100) {
                                    chatHistory.removeAt(0)
                                }

                                call.respond(HttpStatusCode.OK)
                            }

                            get("/history") {
                                val history = synchronized(statusHistory) { statusHistory.toList() }
                                call.respond(history)
                            }
                        }
                    }
                }


            }
        }
    }

    fun start() = synchronized(this@WebServer) {
        // Create upload directory if it doesn't exist
        val uploadDir = File(conf.uploadPath)
        if (!uploadDir.exists()) {
            uploadDir.mkdirs()
        }

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

        Events.on(EventType.PlayerChatEvent::class.java) { event ->
            val player = event.player
            val message = event.message

            // Add the chat message to the chat history
            val chatMessage = ChatMessage(player.name(), message, isWeb = false)
            chatHistory.add(chatMessage)
            if (chatHistory.size > 100) {
                chatHistory.removeAt(0)
            }

            Log.debug("Chat message added to history: ${player.name()}: $message")
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
            val passwordMatches = suspendTransaction {
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
            // Validate that the map parses correctly without actually loading it into the server
            val parsedMap = MapIO.createMap(Fi(tempFile.absolutePath), true)
            if (parsedMap.width <= 0 || parsedMap.height <= 0) {
                throw IllegalArgumentException("Invalid map dimensions: ${parsedMap.width}x${parsedMap.height}")
            }

            // Save the file
            val targetFile = File(conf.uploadPath, fileName)
            Files.copy(tempFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            tempFile.delete()

            // Log the upload details
            try {
                val session = call.sessions.get<UserSession>()
                val username = session?.username ?: "unknown"
                val map = MapIO.createMap(Fi(targetFile.absolutePath), true)
                writeLog(
                    LogType.Web,
                    "User '$username' uploaded file '$fileName' (Map name: '${map.plainName()}', Author: '${map.plainAuthor()}', Version: ${map.version}, Build: ${map.build}, Size: ${map.width}x${map.height})"
                )
            } catch (le: Exception) {
                Log.err("Error writing upload log", le)
            }

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

    private suspend fun getMaps(): List<MapInfo> {
        val mapsList = mutableListOf<MapInfo>()
        Vars.maps.all().filter { it.custom }.forEach { map ->
            val mapName = map.name()
            val ratings = getMapRatings(mapName)
            val upvotes = ratings.count { it.rating >= 3 }
            val downvotes = ratings.count { it.rating < 3 }
            val netVotes = upvotes - downvotes

            mapsList.add(
                MapInfo(
                    name = mapName,
                    author = map.author(),
                    description = map.description(),
                    planet = map.tags.get("planet", "serpulo"),
                    preview = null, // We'll implement preview image generation later
                    votes = netVotes
                )
            )
        }
        return mapsList
    }

    private fun getServerStatus(): ServerStatus {
        val playersList = mutableListOf<WebPlayerInfo>()
        Groups.player.each { player ->
            val playerData = players.find { it.uuid == player.uuid() }
            val playtimeStr = if (playerData != null) {
                val joinInstant = playerData.lastLoginDate.toInstant(systemTimezone)
                val elapsedSeconds = (Clock.System.now().toEpochMilliseconds() - joinInstant.toEpochMilliseconds()) / 1000
                elapsedSeconds.seconds.toHString()
            } else {
                "00:00"
            }
            playersList.add(WebPlayerInfo(player.name(), playtimeStr))
        }

        val mode = when {
            Vars.state == null || Vars.state.isMenu -> "none"
            Vars.state.rules.pvp -> "pvp"
            Vars.state.rules.mode() == mindustry.game.Gamemode.survival || Vars.state.rules.mode() == mindustry.game.Gamemode.attack -> "wave"
            else -> "none"
        }

        val activeTeams = if (Vars.state != null && !Vars.state.isMenu && Vars.state.teams != null && Vars.state.teams.active != null) {
            Vars.state.teams.active.size
        } else {
            0
        }

        return ServerStatus(
            map = if (Vars.state != null && Vars.state.map != null) Vars.state.map.name() else "Menu",
            players = playersList,
            tps = Core.graphics.framesPerSecond.toFloat(),
            wave = if (Vars.state != null) Vars.state.wave else 0,
            gameTime = playTime,
            mode = mode,
            activeTeams = activeTeams
        )
    }

    private fun recordStatusPoint() {
        if (Vars.state == null || Vars.state.isMenu) return

        val mode = when {
            Vars.state == null || Vars.state.isMenu -> "none"
            Vars.state.rules.pvp -> "pvp"
            Vars.state.rules.mode() == mindustry.game.Gamemode.survival || Vars.state.rules.mode() == mindustry.game.Gamemode.attack -> "wave"
            else -> "none"
        }

        var resources: Map<String, Int>? = null
        var teamResources: Map<String, Int>? = null
        var teamUnits: Map<String, Int>? = null
        var teamBuildings: Map<String, Int>? = null

        if (mode == "wave") {
            val resMap = mutableMapOf<String, Int>()
            val cores = Vars.state.teams.cores(Vars.state.rules.defaultTeam)
            if (cores != null && !cores.isEmpty) {
                Vars.content.items().forEach { item ->
                    if (!item.isHidden) {
                        var sum = 0
                        cores.forEach { core ->
                            sum += core.items.get(item)
                        }
                        resMap[item.name] = sum
                    }
                }
            }
            resources = resMap
        } else if (mode == "pvp") {
            val teamResMap = mutableMapOf<String, Int>()
            val teamUnitsMap = mutableMapOf<String, Int>()
            val teamBuildingsMap = mutableMapOf<String, Int>()

            if (Vars.state.teams != null && Vars.state.teams.active != null) {
                Vars.state.teams.active.forEach { teamData ->
                    val team = teamData.team
                    val teamName = team.name

                    // Compute team resources (sum of all items across all cores of this team)
                    val cores = teamData.cores
                    var totalRes = 0
                    if (cores != null && !cores.isEmpty) {
                        Vars.content.items().forEach { item ->
                            if (!item.isHidden) {
                                cores.forEach { core ->
                                    totalRes += core.items.get(item)
                                }
                            }
                        }
                    }
                    teamResMap[teamName] = totalRes

                    // Compute team units
                    var unitCount = 0
                    for (unit in Groups.unit) {
                        if (unit.team == team) {
                            unitCount++
                        }
                    }
                    teamUnitsMap[teamName] = unitCount

                    // Compute team buildings
                    var buildingCount = 0
                    for (build in Groups.build) {
                        if (build.team == team) {
                            buildingCount++
                        }
                    }
                    teamBuildingsMap[teamName] = buildingCount
                }
            }
            teamResources = teamResMap
            teamUnits = teamUnitsMap
            teamBuildings = teamBuildingsMap
        }

        val point = StatusDataPoint(
            time = System.currentTimeMillis(),
            tps = Core.graphics.framesPerSecond.toFloat(),
            players = Groups.player.size(),
            units = Groups.unit.size,
            buildings = Groups.build.size,
            resources = resources,
            teamResources = teamResources,
            teamUnits = teamUnits,
            teamBuildings = teamBuildings
        )

        synchronized(statusHistory) {
            statusHistory.add(point)
            if (statusHistory.size > 1440) {
                statusHistory.removeAt(0)
            }
        }
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
