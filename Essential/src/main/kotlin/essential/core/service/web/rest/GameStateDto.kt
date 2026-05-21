package essential.core.service.web.rest

import kotlinx.serialization.Serializable

/** REST API DTOs for game state responses */

@Serializable
data class GameRulesDto(
    val attackMode: Boolean,
    val pvp: Boolean,
    val waveSpacing: Float,
    val waveTeam: String,
    val defaultTeam: String,
    val dropZoneRadius: Float,
    val blockDamageMultiplier: Float,
    val unitDamageMultiplier: Float,
    val coreCapture: Boolean,
    val infiniteResources: Boolean,
    val modeName: String
)

@Serializable
data class GameMapDto(
    val name: String,
    val author: String,
    val description: String,
    val version: Int,
    val width: Int,
    val height: Int,
    val planet: String,
    val isCustom: Boolean
)

@Serializable
data class PlayerInfoDto(
    val id: Int,
    val name: String,
    val team: String,
    val uuid: String,
    val x: Float,
    val y: Float,
    val isAlive: Boolean,
    val isAdmin: Boolean
)

@Serializable
data class TeamInfoDto(
    val name: String,
    val color: String,
    val side: Int,
    val score: Int,
    val alive: Boolean,
    val leader: String,
    val units: Int,
    val buildings: Int
)

@Serializable
data class PluginInfoDto(
    val version: String,
    val uptime: String,
    val playTime: String,
    val gameOverCount: Int,
    val playerCount: Int,
    val offlinePlayerCount: Int
)

@Serializable
data class VoteStateDto(
    val isVoting: Boolean,
    val isSurrender: Boolean,
    val nextVoteAvailableSeconds: Long,
    val voterCooldownCount: Int
)

@Serializable
data class GameStateDto(
    val isPlaying: Boolean,
    val isPaused: Boolean,
    val wave: Int,
    val wavetime: Float,
    val mapName: String,
    val fps: Float,
    val players: Int
)

@Serializable
data class GameOverviewDto(
    val state: GameStateDto,
    val rules: GameRulesDto,
    val map: GameMapDto,
    val players: List<PlayerInfoDto>,
    val teams: List<TeamInfoDto>,
    val plugin: PluginInfoDto,
    val vote: VoteStateDto
)
