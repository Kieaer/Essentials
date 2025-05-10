package essential.core

import kotlinx.serialization.Serializable

@Serializable
data class CoreConfig(
    val plugin: Plugin = Plugin(),
    val feature: Feature = Feature(),
    val command: Command = Command(),
)

/** 플러그인 설정 */
@Serializable
data class Plugin(
    val lang: String = "en",
    val autoUpdate: Boolean = true,
    val database: DatabaseConfig = DatabaseConfig(),
)

/** Database 설정 */
@Serializable
data class DatabaseConfig(
    var url: String = "jdbc:sqlite:config/essential/database.db",
    val username: String = "sa",
    val password: String = "",
)

/** 플러그인 기능 설정 */
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

/** 기능 - 잠수 설정 */
@Serializable
data class Afk(
    val enabled: Boolean = false,
    val time: Int = 300,
    val server: String? = "",
)

/** 기능 - 투표 설정 */
@Serializable
data class Vote(
    val enabled: Boolean = true,
    val enableVotekick: Boolean = false,
)

/** 기능 - 유닛 제한 설정 */
@Serializable
data class UnitFeature(
    val enabled: Boolean = false,
    val limit: Int = 3000,
)

/** 기능 - 오늘의 메세지 설정 */
@Serializable
data class MessageOfTheDay(
    val enabled: Boolean = false,
    val time: Int = 600,
)

/** 기능 - PvP 설정 */
@Serializable
data class PvP(
    val autoTeam: Boolean = false,
    val spector: Boolean = false,
)

/** 기능 - 플레이어 레벨 기능 설정 */
@Serializable
data class LevelConfig(
    val effect: Effects = Effects(),
    val levelNotify: Boolean = false,
    val display: Boolean = false,
)

/** 기능 - 레벨별 이동 효과 설정 */
@Serializable
data class Effects(
    val enabled: Boolean = false,
    val moving: Boolean = false,
)

/** 기능 - 명령어 설정 */
@Serializable
data class Command(
    val skip: Skip = Skip(),
    val rollback: Rollback = Rollback(),
)

/** 명령어 - wave skip 설정 */
@Serializable
data class Skip(
    val enabled: Boolean = true,
    val limit: Int = 10,
)

/** 기능 - 게임 설정 */
@Serializable
data class Game(
    val wave: Wave = Wave(),
)

/** 게임 설정 - wave 설정 */
@Serializable
data class Wave(
    val autoSkip: Int = 1,
)

/** 기능 - 닉네임 필터링 설정 */
@Serializable
data class Blacklist(
    val enabled: Boolean = true,
    val regex: Boolean = false,
)

/** 명령어 - 롤백 설정 */
@Serializable
data class Rollback(
    val enabled: Boolean = true,
    val time: Int = 300,
)
