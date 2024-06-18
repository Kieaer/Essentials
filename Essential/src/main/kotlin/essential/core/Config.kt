package essential.core

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val plugin: Plugin,
    val feature: Feature,
    val command: Command
)

@Serializable
data class Plugin(
    val lang: String = "en",
    val autoUpdate: Boolean = true,
    val database: DatabaseConf
)

@Serializable
data class DatabaseConf(
    val url: String,
    val username: String,
    val password: String
)

@Serializable
data class Feature(
    val afk: Afk,
    val vote: Vote,
    val unit: UnitFeature,
    val motd: Motd,
    val pvp: Pvp,
    val level: LevelConf,
    val game: Game,
    val blacklist: Blacklist
)

@Serializable
data class Afk(
    val enabled: Boolean = false,
    val time: Int = 300,
    val server: String = ""
)

@Serializable
data class Vote(
    val enabled: Boolean = true,
    val enableVotekick: Boolean = false
)

@Serializable
data class UnitFeature(
    val enabled: Boolean = false,
    val limit: Int = 3000
)

@Serializable
data class Motd(
    val enabled: Boolean = false,
    val time: Int = 600
)

@Serializable
data class Pvp(
    val autoTeam: Boolean = false,
    val spector: Boolean = false
)

@Serializable
data class LevelConf(
    val effect: Effects,
    val levelNotify: Boolean = false,
    val display: Boolean = false
)

@Serializable
data class Effects(
    val enabled: Boolean = false,
    val moving: Boolean = false
)

@Serializable
data class Command(
    val skip: Skip,
    val rollback: Rollback
)

@Serializable
data class Skip(
    val enabled: Boolean = true,
    val limit: Int = 10
)

@Serializable
data class Game(
    val wave: Wave
)

@Serializable
data class Wave(
    val autoSkip: Int = 1
)

@Serializable
data class Blacklist(
    val enabled: Boolean,
    val regex: Boolean
)

@Serializable
data class Rollback(
    val enabled: Boolean,
    val time: Int
)