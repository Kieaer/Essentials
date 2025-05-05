package essential.database.table

import org.jetbrains.exposed.dao.id.UIntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDate
import org.jetbrains.exposed.sql.kotlin.datetime.date

object PlayerTable : UIntIdTable("players") {
    val name = varchar("name", 50).index()
    val uuid = varchar("uuid", 24)
    val blockPlaceCount = uinteger("block_place_count").default(0u)
    val blockBreakCount = uinteger("block_break_count").default(0u)
    val level = integer("level").default(0)
    val exp = integer("exp").default(0)
    val firstPlayed = long("first_played").default(0L)
    val lastPlayed = long("last_played").default(0L)
    val totalPlayed = uinteger("total_played").default(0u)
    val attackClear = uinteger("attack_clear").default(0u)
    val waveClear = uinteger("wave_clear").default(0u)
    val pvpWinCount = ushort("pvp_win_count").default(0u)
    val pvpLoseCount = ushort("pvp_lose_count").default(0u)
    val pvpEliminatedCount = ushort("pvp_eliminated_count").default(0u)
    val pvpMvpCount = ushort("pvp_mvp_count").default(0u)
    val permission = varchar("permission", 50).default("default")
    val accountID = varchar("account_id", 50).nullable().default(null)
    val accountPW = varchar("account_pw", 256).nullable().default(null)
    val discordID = varchar("discord_id", 50).nullable().default(null)
    val chatMuted = bool("chat_muted").default(false)
    val effectVisibility = bool("effect_visibility").default(false)
    val effectLevel = ushort("effect_level").nullable().default(null)
    val effectColor = varchar("effect_color", 10).nullable().default(null)
    val hideRanking = bool("hide_ranking").default(false)
    val strictMode = bool("strict_mode").default(false)
    val lastLoginDate = date("last_login_date").defaultExpression(CurrentDate)
    val lastLogoutDate = date("last_logout_date").nullable().default(null)
    val lastPlayedWorldName = varchar("last_played_world_name", 50).nullable().default(null)
    val lastPlayedWorldMode = varchar("last_played_world_mode", 50).nullable().default(null)
    val isConnected = bool("is_connected").default(false)
    val isBanned = bool("is_banned").default(false)
    val banExpireDate = ulong("ban_expire_date").default(0u)
    val attendanceDays = uinteger("attendance_days").default(0u)
}
