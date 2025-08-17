package essential.database.data

import essential.database.table.PlayerTable
import essential.systemTimezone
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

/**
 * Merge player data from a source UUID into a target UUID according to the defined rules.
 * - Non-merged fields (kept from target): id, name, uuid, level, permission, account_id, account_pw, discord_id
 * - Summed: block_place_count, block_break_count, exp, total_played, attack_clear, wave_clear,
 *           pvp_win_count, pvp_lose_count, pvp_eliminated_count, pvp_mvp_count, attendance_days
 * - first_played: oldest of the two
 * - last_played: newest of the two
 * - last_login_date, last_logout_date: newest of the two (null-safe for logout)
 * - last_played_world_name/mode: use source (new) values
 * - ban_expire_date: latest of the two; isBanned adjusted to whether ban not expired
 */
@Suppress("RedundantSuspendModifier")
suspend fun mergePlayerAccounts(fromUuid: String, toUuid: String): String = newSuspendedTransaction {
    if (fromUuid.equals(toUuid, ignoreCase = true)) {
        return@newSuspendedTransaction "Source and target UUID must be different."
    }

    val fromRow = PlayerTable.selectAll().where { PlayerTable.uuid eq fromUuid }.singleOrNull()
    val toRow = PlayerTable.selectAll().where { PlayerTable.uuid eq toUuid }.singleOrNull()

    if (fromRow == null) return@newSuspendedTransaction "Source player not found: $fromUuid"
    if (toRow == null) return@newSuspendedTransaction "Target player not found: $toUuid"

    val from = fromRow.toPlayerData()
    val to = toRow.toPlayerData()

    // Calculate merged values
    fun sumShort(a: Short, b: Short): Short = (a.toInt() + b.toInt()).coerceIn(0, Short.MAX_VALUE.toInt()).toShort()

    val mergedFirstPlayed: LocalDateTime = if (from.firstPlayed < to.firstPlayed) from.firstPlayed else to.firstPlayed
    val mergedLastPlayed: LocalDateTime = if (from.lastPlayed > to.lastPlayed) from.lastPlayed else to.lastPlayed

    val mergedLastLogin: LocalDateTime = if (from.lastLoginDate > to.lastLoginDate) from.lastLoginDate else to.lastLoginDate

    val mergedLastLogout: LocalDateTime? = when {
        from.lastLogoutDate == null && to.lastLogoutDate == null -> null
        from.lastLogoutDate == null -> to.lastLogoutDate
        to.lastLogoutDate == null -> from.lastLogoutDate
        else -> if (from.lastLogoutDate!! > to.lastLogoutDate!!) from.lastLogoutDate else to.lastLogoutDate
    }

    val mergedBanExpire: LocalDateTime? = when {
        from.banExpireDate == null && to.banExpireDate == null -> null
        from.banExpireDate == null -> to.banExpireDate
        to.banExpireDate == null -> from.banExpireDate
        else -> if (from.banExpireDate!! > to.banExpireDate!!) from.banExpireDate else to.banExpireDate
    }

    val nowLocal = Clock.System.now().toLocalDateTime(systemTimezone)
    // isBanned flag aligned to merged banExpireDate; if null -> false
    val mergedIsBanned: Boolean = mergedBanExpire?.let { it > nowLocal } ?: false

    // Apply update to target (keep non-merged fields from target as-is)
    PlayerTable.update({ PlayerTable.id eq to.id }) {
        it[blockPlaceCount] = to.blockPlaceCount + from.blockPlaceCount
        it[blockBreakCount] = to.blockBreakCount + from.blockBreakCount
        it[level] = to.level // unchanged per rule
        it[exp] = to.exp + from.exp
        it[firstPlayed] = mergedFirstPlayed
        it[lastPlayed] = mergedLastPlayed
        it[totalPlayed] = to.totalPlayed + from.totalPlayed
        it[attackClear] = to.attackClear + from.attackClear
        it[waveClear] = to.waveClear + from.waveClear
        it[pvpWinCount] = sumShort(to.pvpWinCount, from.pvpWinCount)
        it[pvpLoseCount] = sumShort(to.pvpLoseCount, from.pvpLoseCount)
        it[pvpEliminatedCount] = sumShort(to.pvpEliminatedCount, from.pvpEliminatedCount)
        it[pvpMvpCount] = sumShort(to.pvpMvpCount, from.pvpMvpCount)
        it[permission] = to.permission // unchanged per rule
        it[accountID] = to.accountID // unchanged per rule
        it[accountPW] = to.accountPW // unchanged per rule
        it[discordID] = to.discordID // unchanged per rule
        it[lastLoginDate] = mergedLastLogin
        it[lastLogoutDate] = mergedLastLogout
        it[lastPlayedWorldName] = from.lastPlayedWorldName // from source
        it[lastPlayedWorldMode] = from.lastPlayedWorldMode // from source
        it[isBanned] = mergedIsBanned
        it[banExpireDate] = mergedBanExpire
        it[attendanceDays] = to.attendanceDays + from.attendanceDays
        // Keep other fields (chatMuted, effect*, hideRanking, strictMode, isConnected, name, uuid) unchanged intentionally
    }

    // Delete source row
    PlayerTable.deleteWhere { PlayerTable.id eq from.id }

    return@newSuspendedTransaction "Merged ${from.name} ($fromUuid) into ${to.name} ($toUuid)."
}
