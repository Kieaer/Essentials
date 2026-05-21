package essential.core.service.web.rest

import arc.Core
import essential.common.*
import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import mindustry.Vars
import mindustry.gen.Building
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Unit

/** Builds all game state DTOs from the current game data */
object GameStateBuilder {

    fun buildRules() = GameRulesDto(
        attackMode = Vars.state.rules.attackMode,
        pvp = Vars.state.rules.pvp,
        waveSpacing = Vars.state.rules.waveSpacing,
        waveTeam = Vars.state.rules.waveTeam.name,
        defaultTeam = Vars.state.rules.defaultTeam.name,
        dropZoneRadius = Vars.state.rules.dropZoneRadius,
        blockDamageMultiplier = Vars.state.rules.blockDamageMultiplier,
        unitDamageMultiplier = Vars.state.rules.unitDamageMultiplier,
        coreCapture = Vars.state.rules.coreCapture,
        infiniteResources = Vars.state.rules.infiniteResources,
        modeName = Vars.state.rules.modeName
    )

    fun buildMap() = GameMapDto(
        name = Vars.state.map.name(),
        author = Vars.state.map.author(),
        description = Vars.state.map.description(),
        version = Vars.state.map.version,
        width = Vars.state.map.width,
        height = Vars.state.map.height,
        planet = Vars.state.map.tags.get("planet", "serpulo"),
        isCustom = Vars.state.map.custom
    )

    fun buildPlayers(): List<PlayerInfoDto> {
        val list = mutableListOf<PlayerInfoDto>()
        Groups.player.forEach { player ->
            if (player != null) {
                list.add(playerToDto(player))
            }
        }
        return list
    }

    private fun playerToDto(player: Player): PlayerInfoDto = PlayerInfoDto(
        id = player.id(),
        name = player.name(),
        team = player.team().name,
        uuid = player.uuid(),
        x = player.x(),
        y = player.y(),
        isAlive = player.unit() == null || !player.unit()!!.dead(),
        isAdmin = player.admin()
    )

    fun buildTeams(): List<TeamInfoDto> {
        val list = mutableListOf<TeamInfoDto>()
        Vars.state.teams.active.forEach { teamData ->
            val team = teamData.team
            var unitCount = 0
            var buildingCount = 0
            Groups.unit.forEach { u: Unit -> if (u.team == team) unitCount++ }
            Groups.build.forEach { b: Building -> if (b.team == team) buildingCount++ }

            list.add(TeamInfoDto(
                name = team.name,
                color = team.color.toString(),
                side = team.id,
                score = 0,
                alive = teamData.cores.size > 0,
                leader = teamData.players.filterNotNull().randomOrNull()?.name() ?: "none",
                units = unitCount,
                buildings = buildingCount
            ))
        }
        return list
    }

    fun buildPlugin() = PluginInfoDto(
        version = PLUGIN_VERSION,
        uptime = uptime,
        playTime = playTime,
        gameOverCount = gameOverCount,
        playerCount = Groups.player.size(),
        offlinePlayerCount = offlinePlayers.size
    )

    fun buildVoteState() = VoteStateDto(
        isVoting = isVoting,
        isSurrender = isSurrender,
        nextVoteAvailableSeconds = 0L,
        voterCooldownCount = voterCooldown.size
    )

    fun buildState() = GameStateDto(
        isPlaying = Vars.state.isPlaying,
        isPaused = Vars.state.isPaused,
        wave = Vars.state.wave,
        wavetime = Vars.state.wavetime,
        mapName = Vars.state.map.name(),
        fps = Core.graphics.framesPerSecond.toFloat(),
        players = Groups.player.size()
    )

    fun buildOverview() = GameOverviewDto(
        state = buildState(),
        rules = buildRules(),
        map = buildMap(),
        players = buildPlayers(),
        teams = buildTeams(),
        plugin = buildPlugin(),
        vote = buildVoteState()
    )
}

/** Registers game state API routes for REST API mode */
fun Route.gameStateRoutes() {
    val json = Json { prettyPrint = true; isLenient = true }
    route("/api/game") {
        get("/overview") {
            val overview = GameStateBuilder.buildOverview()
            call.respondText(json.encodeToString(GameOverviewDto.serializer(), overview), ContentType.Application.Json)
        }

        get("/state") {
            val state = GameStateBuilder.buildState()
            call.respondText(json.encodeToString(GameStateDto.serializer(), state), ContentType.Application.Json)
        }

        get("/rules") {
            val rules = GameStateBuilder.buildRules()
            call.respondText(json.encodeToString(GameRulesDto.serializer(), rules), ContentType.Application.Json)
        }

        get("/map") {
            val map = GameStateBuilder.buildMap()
            call.respondText(json.encodeToString(GameMapDto.serializer(), map), ContentType.Application.Json)
        }

        get("/players") {
            val playerList = GameStateBuilder.buildPlayers()
            call.respondText(json.encodeToString(ListSerializer(PlayerInfoDto.serializer()), playerList), ContentType.Application.Json)
        }

        get("/teams") {
            val teamList = GameStateBuilder.buildTeams()
            call.respondText(json.encodeToString(ListSerializer(TeamInfoDto.serializer()), teamList), ContentType.Application.Json)
        }

        get("/plugin") {
            val plugin = GameStateBuilder.buildPlugin()
            call.respondText(json.encodeToString(PluginInfoDto.serializer(), plugin), ContentType.Application.Json)
        }

        get("/vote") {
            val vote = GameStateBuilder.buildVoteState()
            call.respondText(json.encodeToString(VoteStateDto.serializer(), vote), ContentType.Application.Json)
        }
    }
}
