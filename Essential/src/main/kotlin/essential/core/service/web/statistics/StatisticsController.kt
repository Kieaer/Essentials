package essential.core.service.web.statistics

import arc.Core
import arc.Events
import arc.util.Log
import essential.common.database.data.getAverageContribution
import essential.common.database.data.getContributionCount
import essential.common.playTime
import essential.common.players
import essential.common.systemTimezone
import essential.common.util.size
import essential.common.util.toHString
import essential.core.service.web.auth.UserSession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.toInstant
import kotlinx.serialization.Serializable
import mindustry.Vars
import mindustry.game.EventType
import mindustry.gen.Call
import mindustry.gen.Groups
import java.util.*
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
data class ContributionEntry(
    val name: String,
    val current: Double,
    val average: Double,
    val games: Int,
    val team: String? = null,
    val teamColor: String? = null
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

class StatisticsController {
    val chatHistory = Collections.synchronizedList(mutableListOf<ChatMessage>())
    val statusHistory = Collections.synchronizedList(mutableListOf<StatusDataPoint>())

    fun init(scope: CoroutineScope) {
        scope.launch {
            while (true) {
                delay(60000.milliseconds)
                try {
                    recordStatusPoint()
                } catch (e: Exception) {
                    Log.err("Error recording status point", e)
                }
            }
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

    suspend fun handleGetContribution(call: ApplicationCall) {
        if (!essential.core.Main.conf.module.contribution) {
            return call.respond(HttpStatusCode.Forbidden, "Contribution module is disabled")
        }
        // Live: current online players, each with this game's contribution and their overall average.
        // In PvP, include team so the client can group players by team.
        val isPvp = Vars.state != null && !Vars.state.isMenu && Vars.state.rules.pvp
        val snapshot = players.toList()
        val entries = snapshot.map { data ->
            val team = if (isPvp) data.player.team() else null
            ContributionEntry(
                name = data.name,
                current = data.currentContribution,
                average = getAverageContribution(data),
                games = getContributionCount(data),
                team = team?.name,
                teamColor = team?.color?.toString()?.let { "#$it" }
            )
        }.sortedByDescending { it.current }
        call.respond(entries)
    }

    suspend fun handleGetChat(call: ApplicationCall) {
        val messages = chatHistory.filter { !it.message.startsWith("/") }.sortedBy { it.time }
        call.respond(messages)
    }

    suspend fun handlePostChat(call: ApplicationCall) {
        val session = call.sessions.get<UserSession>()
            ?: return call.respond(HttpStatusCode.Unauthorized)
        val message = call.receiveText()

        // Validate chat message
        if (message.isBlank() || message.length > 100) {
            return call.respond(HttpStatusCode.BadRequest, "Invalid message")
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

    suspend fun handleGetHistory(call: ApplicationCall) {
        val history = synchronized(statusHistory) { statusHistory.toList() }
        call.respond(history)
    }

    suspend fun handleGetServerStatus(call: ApplicationCall) {
        val status = getServerStatus()
        call.respond(status)
    }
}

fun Route.statisticsRoutes(controller: StatisticsController) {
    route("/api/server") {
        get("/status") {
            controller.handleGetServerStatus(call)
        }

        get("/contribution") {
            controller.handleGetContribution(call)
        }

        authenticate("auth-session") {
            get("/chat") {
                controller.handleGetChat(call)
            }

            post("/chat") {
                controller.handlePostChat(call)
            }

            get("/history") {
                controller.handleGetHistory(call)
            }
        }
    }
}
