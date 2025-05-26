package essential.database.data.entity

import essential.database.data.PlayerData
import essential.database.table.PlayerTable
import ksp.table.GenerateTable
import mindustry.gen.Playerc
import org.jetbrains.exposed.dao.UIntEntity
import org.jetbrains.exposed.dao.UIntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.mindrot.jbcrypt.BCrypt

@GenerateTable
class PlayerDataEntity(id: EntityID<UInt>) : UIntEntity(id) {
    companion object : UIntEntityClass<PlayerDataEntity>(PlayerTable)

    var name by PlayerTable.name
    var uuid by PlayerTable.uuid
    var blockPlaceCount by PlayerTable.blockPlaceCount
    var blockBreakCount by PlayerTable.blockBreakCount
    var level by PlayerTable.level
    var exp by PlayerTable.exp
    var firstPlayed by PlayerTable.firstPlayed
    var lastPlayed by PlayerTable.lastPlayed
    var totalPlayed by PlayerTable.totalPlayed
    var attackClear by PlayerTable.attackClear
    var waveClear by PlayerTable.waveClear
    var pvpWinCount by PlayerTable.pvpWinCount
    var pvpLoseCount by PlayerTable.pvpLoseCount
    var pvpEliminatedCount by PlayerTable.pvpEliminatedCount
    var pvpMvpCount by PlayerTable.pvpMvpCount
    var permission by PlayerTable.permission
    var accountID by PlayerTable.accountID
    var accountPW by PlayerTable.accountPW
    var discordID by PlayerTable.discordID
    var chatMuted by PlayerTable.chatMuted
    var effectVisibility by PlayerTable.effectVisibility
    var effectLevel by PlayerTable.effectLevel
    var effectColor by PlayerTable.effectColor
    var hideRanking by PlayerTable.hideRanking
    var strictMode by PlayerTable.strictMode
    var lastLoginDate by PlayerTable.lastLoginDate
    var lastLogoutDate by PlayerTable.lastLogoutDate
    var lastPlayedWorldName by PlayerTable.lastPlayedWorldName
    var lastPlayedWorldMode by PlayerTable.lastPlayedWorldMode
    var isConnected by PlayerTable.isConnected
    var isBanned by PlayerTable.isBanned
    var banExpireDate by PlayerTable.banExpireDate
    var attendanceDays by PlayerTable.attendanceDays
}

/** 플레이어 데이터 생성 */
fun createPlayerData(player: Playerc): PlayerData {
    return createPlayerData(player.name(), player.uuid())
}

fun createPlayerData(name: String, uuid: String): PlayerData {
    val entity = PlayerDataEntity.new {
        this.name = name
        this.uuid = uuid
    }
    return PlayerData(entity)
}

fun createPlayerData(name: String, uuid: String, accountID: String, accountPW: String): PlayerData {
    val entity = PlayerDataEntity.new {
        this.name = name
        this.uuid = uuid
        this.accountID = accountID
        this.accountPW = BCrypt.hashpw(accountPW, BCrypt.gensalt())
    }
    return PlayerData(entity)
}

/** 플레이어 데이터 읽기 */
suspend fun getPlayerData(player: Playerc): PlayerData? {
    return getPlayerData(player.uuid())
}

suspend fun getPlayerData(uuid: String): PlayerData? {
    return newSuspendedTransaction {
        val entity = PlayerDataEntity.find { PlayerTable.uuid eq uuid }.firstOrNull()
        entity?.let { PlayerData(it) }
    }
}
