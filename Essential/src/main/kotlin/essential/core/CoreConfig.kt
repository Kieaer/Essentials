package essential.core

import com.charleskorn.kaml.YamlComment
import kotlinx.serialization.Serializable

@Serializable
data class CoreConfig(
    @YamlComment("Plugin configuration")
    val plugin: Plugin = Plugin(),
    @YamlComment("Feature settings")
    val feature: Feature = Feature(),
    @YamlComment("Module enable/disable settings")
    val module: Module = Module(),
    @YamlComment("Command settings")
    val command: Command = Command(),
    val ban: Ban = Ban()
)

/** Plugin configuration */
@Serializable
data class Plugin(
    @YamlComment("Default language code (en: English, ko: Korean, etc.)")
    val lang: String = "en",
    @YamlComment("Automatically update the plugin when new version is available")
    val autoUpdate: Boolean = true,
    @YamlComment("Database configuration")
    val database: DatabaseConfig = DatabaseConfig(),
)

/** Database configuration */
@Serializable
data class DatabaseConfig(
    @YamlComment("Database connection URL (h2: embedded database, mysql: MySQL, etc.)")
    var url: String = "h2:./config/mods/Essentials/data/database",
    @YamlComment("Database username")
    val username: String = "sa",
    @YamlComment("Database password")
    val password: String = "",
)

/** Plugin feature configuration */
@Serializable
data class Feature(
    @YamlComment("AFK (Away From Keyboard) detection")
    val afk: Afk = Afk(),
    @YamlComment("Vote system")
    val vote: Vote = Vote(),
    @YamlComment("Unit (block) usage limits")
    val unit: UnitFeature = UnitFeature(),
    @YamlComment("Message of the Day (MOTD)")
    val motd: MessageOfTheDay = MessageOfTheDay(),
    @YamlComment("PvP-related features")
    val pvp: PvP = PvP(),
    @YamlComment("Leveling system")
    val level: LevelConfig = LevelConfig(),
    @YamlComment("Game mode settings (wave survival, etc.)")
    val game: Game = Game(),
    @YamlComment("Player name blacklist settings")
    val blacklist: Blacklist = Blacklist(),
    @YamlComment("Show total player count on server list")
    val count: Boolean = false,
    @YamlComment("Show map vote menu on GameOver")
    val mapVote: Boolean = false,
)

/** Feature - AFK settings */
@Serializable
data class Afk(
    val enabled: Boolean = false,
    @YamlComment("Time in seconds before player is marked as AFK")
    val time: Int = 300,
    @YamlComment("Server to teleport AFK players to (empty = disable teleport)")
    val server: String? = "",
)

/** Feature - Vote settings */
@Serializable
data class Vote(
    val enabled: Boolean = true,
    @YamlComment("Enable vote kick feature")
    val enableVotekick: Boolean = false,
)

/** Feature - Unit limit settings */
@Serializable
data class UnitFeature(
    val enabled: Boolean = false,
    @YamlComment("Maximum number of blocks a player can place")
    val limit: Int = 3000,
)

/** Feature - Message of the Day settings */
@Serializable
data class MessageOfTheDay(
    val enabled: Boolean = false,
    @YamlComment("Update interval in seconds")
    val time: Int = 600,
)

/** Feature - PvP settings */
@Serializable
data class PvP(
    @YamlComment("Automatically assign teams in PvP")
    val autoTeam: Boolean = false,
    @YamlComment("Spectator mode for defeated players")
    val spector: Boolean = false,
)

/** Feature - Player level feature settings */
@Serializable
data class LevelConfig(
    @YamlComment("Visual effects for leveling up")
    val effect: Effects = Effects(),
    @YamlComment("Show level-up notification")
    val levelNotify: Boolean = false,
    @YamlComment("Display level information")
    val display: Boolean = false,
)

/** Feature - Movement effects per level */
@Serializable
data class Effects(
    val enabled: Boolean = false,
    @YamlComment("Trigger effect only when moving")
    val moving: Boolean = false,
)

@Serializable
data class Module(
    val achievement: Boolean = false,
    val bridge: Boolean = false,
    val chat: Boolean = true,
    val contribution: Boolean = true,
    val discord: Boolean = false,
    val protect: Boolean = false,
    val web: Boolean = false,
)

/** Feature - Command settings */
@Serializable
data class Command(
    @YamlComment("Vote skip command configuration")
    val skip: Skip = Skip(),
    @YamlComment("Rollback command configuration")
    val rollback: Rollback = Rollback(),
)

/** Command - Wave skip settings */
@Serializable
data class Skip(
    val enabled: Boolean = true,
    @YamlComment("Maximum number of skips allowed")
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
    @YamlComment("Process 1 + n waves (default is 1)")
    val autoSkip: Int = 1,
)

/** Feature - Nickname filtering settings */
@Serializable
data class Blacklist(
    val enabled: Boolean = true,
    @YamlComment("Use regex patterns for blacklist matching")
    val regex: Boolean = false,
)

/** Command - Rollback settings */
@Serializable
data class Rollback(
    val enabled: Boolean = true,
    @YamlComment("Maximum rollback time in seconds")
    val time: Int = 300,
    val limit: Int = 10,
)

@Serializable
data class Ban(
    @YamlComment(
        "If true, the database value is used in the banned list.",
        "If false, use mindustry server built-in banned list.",
        "If true and multiple servers are connected to the same DB, the banned list is shared.",
    )
    val useDatabase: Boolean = false,
)
