package essential.database.data

import essential.bundle.Bundle
import essential.database.table.PlayerTable
import essential.ksp.GenerateUpdate
import mindustry.gen.Player
import mindustry.gen.Playerc
import org.jetbrains.exposed.dao.UIntEntity
import org.jetbrains.exposed.dao.UIntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@GenerateUpdate
class PlayerData(id: EntityID<UInt>) : UIntEntity(id) {
    companion object : UIntEntityClass<PlayerData>(PlayerTable)

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

    var expMultiplier: Double = 1.0
    var currentExp: Int = 0
    var currentPlayTime: Int = 0

    var afk = false
    var afkTime: UShort = 0u
    var mousePosition: Float = 0F
    var viewHistoryMode = false
    var mouseTracking = false
    var animatedName = false

    var player: Playerc = Player.create()
    val status = mutableMapOf<String, String>()
    val bundle: Bundle get() = Bundle(player.locale())

    fun err(message: String, vararg parameters: Any) {
        val text = "[scarlet]" + bundle[message, parameters]
        player.sendMessage(text)
    }

    fun send(message: String, vararg parameters: Any) {
        send(bundle, message, *parameters)
    }

    fun send(bundle: Bundle, message: String, vararg parameters: Any) {
        val text = bundle[message, parameters]
        player.sendMessage(text)
    }

    /** 플레이어의 Discord ID 값을 변경 합니다. */
    suspend fun updatePlayerDataByDiscord(name: String, discord: String) {
        newSuspendedTransaction {
            findSingleByAndUpdate(PlayerTable.name like name and(PlayerTable.discordID eq discord)) {
                it.discordID = discord
            }
        }
    }
}

/** 플레이어 데이터 생성 */
suspend fun createPlayerData(player: Playerc) : PlayerData {
    return createPlayerData(player.name(), player.uuid())
}

suspend fun createPlayerData(name: String, uuid: String) : PlayerData {
    return newSuspendedTransaction {
        PlayerData.new {
            this.name = name
            this.uuid = uuid
        }
    }
}

/** 플레이어 데이터 읽기 */
suspend fun getPlayerData(player: Playerc): PlayerData? {
    return getPlayerData(player.uuid())
}

suspend fun getPlayerData(uuid: String): PlayerData? {
    return newSuspendedTransaction {
        PlayerData.find { PlayerTable.uuid eq uuid }.firstOrNull()
    }
}