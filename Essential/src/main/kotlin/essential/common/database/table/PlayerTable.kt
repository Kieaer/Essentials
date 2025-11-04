package essential.common.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object PlayerTable : Table("players") {
    val id = uinteger("id").autoIncrement()
    val name = varchar("name", 50).index()
    val uuid = varchar("uuid", 25).uniqueIndex("uuid")
    val blockPlaceCount = integer("block_place_count").default(0)
    val blockBreakCount = integer("block_break_count").default(0)
    val level = integer("level").default(0)
    val exp = integer("exp").default(0)
    val firstPlayed = datetime("first_played").defaultExpression(CurrentDateTime)
    val lastPlayed = datetime("last_played").defaultExpression(CurrentDateTime)
    val totalPlayed = integer("total_played").default(0)
    val attackClear = integer("attack_clear").default(0)
    val waveClear = integer("wave_clear").default(0)
    val pvpWinCount = short("pvp_win_count").default(0)
    val pvpLoseCount = short("pvp_lose_count").default(0)
    val pvpEliminatedCount = short("pvp_eliminated_count").default(0)
    val pvpMvpCount = short("pvp_mvp_count").default(0)
    val permission = varchar("permission", 50).default("default")
    val accountID = varchar("account_id", 50).nullable().default(null)
    val accountPW = varchar("account_pw", 256).nullable().default(null)
    val discordID = varchar("discord_id", 50).nullable().default(null).uniqueIndex("discord_id")
    val chatMuted = bool("chat_muted").default(false)
    val effectVisibility = bool("effect_visibility").default(false)
    val effectLevel = short("effect_level").nullable().default(null)
    val effectColor = varchar("effect_color", 10).nullable().default(null)
    val hideRanking = bool("hide_ranking").default(false)
    val strictMode = bool("strict_mode").default(false)
    val lastLoginDate = datetime("last_login_date").defaultExpression(CurrentDateTime)
    val lastLogoutDate = datetime("last_logout_date").nullable().default(null)
    val lastPlayedWorldName = varchar("last_played_world_name", 50).nullable().default(null)
    val lastPlayedWorldMode = varchar("last_played_world_mode", 50).nullable().default(null)
    val isConnected = bool("is_connected").default(false)
    val isBanned = bool("is_banned").default(false)
    val banExpireDate = datetime("ban_expire_date").nullable().default(null)
    val attendanceDays = integer("attendance_days").default(0)

    override val primaryKey = PrimaryKey(id)
}
