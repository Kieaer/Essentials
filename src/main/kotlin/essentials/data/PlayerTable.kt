package essentials.data

import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.jsonb
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object PlayerTable : Table("PlayerData") {
    val name = varchar("name", 255)
    val uuid = varchar("uuid", 24)
    val uuidList = jsonb<List<String>>("uuidList", Json.Default)
    val id = varchar("id", 255)
    val password = text("password")
    val blockPlaceCount = uinteger("blockPlaceCount")
    val blockBreakCount = uinteger("blockBreakCount")
    val totalJoinCount = uinteger("totalJoinCount")
    val totalKickCount = uinteger("totalKickCount")
    val level = uinteger("level")
    val exp = uinteger("exp")
    val firstPlayDate = datetime("firstPlayDate")
    val lastLoginDate = datetime("lastLoginDate")
    val lastLeaveDate = datetime("lastLeaveDate")
    val totalPlayTime = ulong("totalPlayTime")
    val attackModeClear = uinteger("attackModeClear")
    val pvpVictories = uinteger("pvpVictories")
    val pvpDefeats = uinteger("pvpDefeats")
    val pvpEliminationTeamCount = uinteger("pvpEliminationTeamCount")
    val hideRanking = bool("hideRanking")
    val joinStacks = uinteger("joinStacks")
    val strict = bool("strict")
    val isConnected = bool("isConnected")

    val animatedName = bool("animatedName")
    val permission = varchar("permission", 255)
    val mute = bool("mute")
    val discord = varchar("discord", 255)
    val effectLevel = uinteger("effectLevel")
    val effectColor = varchar("effectColor", 50)
    val freeze = bool("freeze")
    val hud = varchar("hud", 255)
    val tpp = bool("tpp")
    val log = bool("log")
    val banTime = datetime("banTime")
    val tracking = bool("tracking")
    val showLevelEffects = bool("showLevelEffects")
    val lastPlayedWorldName = jsonb<List<String>>("lastPlayedWorldName", Json.Default)
    val lastPlayedWorldMode = jsonb<List<String>>("lastPlayedWorldMode", Json.Default)
    val mvpTime = uinteger("mvpTime")

    override val primaryKey: PrimaryKey = PrimaryKey(uuid)
}