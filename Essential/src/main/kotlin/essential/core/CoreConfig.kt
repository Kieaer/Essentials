package essential.core

import kotlinx.serialization.Serializable

@Serializable
data class CoreConfig(
    val plugin: Plugin = Plugin(),
    val feature: Feature = Feature(),
    val command: Command = Command(),
)

/** Plugin configuration */
@Serializable
data class Plugin(
    val lang: String = "en",
    val autoUpdate: Boolean = true,
    val database: DatabaseConfig = DatabaseConfig(),
    val enableBridge: Boolean = true,
    val enableProtect: Boolean = true,
    val enableChat: Boolean = true,
    val enableAchievements: Boolean = true,
    val enableDiscord: Boolean = true,
)

/** Database configuration */
@Serializable
data class DatabaseConfig(
    var url: String = "h2:./config/mods/Essentials/data/database",
    val username: String = "sa",
    val password: String = "",
)

/** Plugin feature configuration */
@Serializable
data class Feature(
    val afk: Afk = Afk(),
    val vote: Vote = Vote(),
    val unit: UnitFeature = UnitFeature(),
    val motd: MessageOfTheDay = MessageOfTheDay(),
    val pvp: PvP = PvP(),
    val level: LevelConfig = LevelConfig(),
    val game: Game = Game(),
    val blacklist: Blacklist = Blacklist(),
    val count: Boolean = false,
)

/** Feature - AFK settings */
@Serializable
data class Afk(
    val enabled: Boolean = false,
    val time: Int = 300,
    val server: String? = "",
)

/** Feature - Vote settings */
@Serializable
data class Vote(
    val enabled: Boolean = true,
    val enableVotekick: Boolean = false,
)

/** Feature - Unit limit settings */
@Serializable
data class UnitFeature(
    val enabled: Boolean = false,
    val limit: Int = 3000,
)

/** Feature - Message of the Day settings */
@Serializable
data class MessageOfTheDay(
    val enabled: Boolean = false,
    val time: Int = 600,
)

/** Feature - PvP settings */
@Serializable
data class PvP(
    val autoTeam: Boolean = false,
    val spector: Boolean = false,
)

/** Feature - Player level feature settings */
@Serializable
data class LevelConfig(
    val effect: Effects = Effects(),
    val levelNotify: Boolean = false,
    val display: Boolean = false,
)

/** Feature - Movement effects per level */
@Serializable
data class Effects(
    val enabled: Boolean = false,
    val moving: Boolean = false,
)

/** Feature - Command settings */
@Serializable
data class Command(
    val skip: Skip = Skip(),
    val rollback: Rollback = Rollback(),
)

/** Command - Wave skip settings */
@Serializable
data class Skip(
    val enabled: Boolean = true,
    val limit: Int = 10,
)

/** Feature - Game settings */
@Serializable
data class Game(
    val wave: Wave = Wave(),
)

/** Game settings - Wave settings */
@Serializable
data class Wave(
    val autoSkip: Int = 1,
)

/** Feature - Nickname filtering settings */
@Serializable
data class Blacklist(
    val enabled: Boolean = true,
    val regex: Boolean = false,
)

/** Command - Rollback settings */
@Serializable
data class Rollback(
    val enabled: Boolean = true,
    val time: Int = 300,
)
