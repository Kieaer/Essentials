package essential.common.database.data

import arc.util.Log
import essential.common.bundle.Bundle
import essential.common.database.table.PlayerTable
import essential.common.playerNumber
import kotlinx.datetime.LocalDateTime
import ksp.table.GenerateCode
import mindustry.gen.Player
import mindustry.gen.Playerc
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.mindrot.jbcrypt.BCrypt

@GenerateCode
data class PlayerData(
    val id: UInt,
    var name: String,
    var uuid: String,
    var blockPlaceCount: Int = 0,
    var blockBreakCount: Int = 0,
    var level: Int = 0,
    var exp: Int = 0,
    var firstPlayed: LocalDateTime,
    var lastPlayed: LocalDateTime,
    var totalPlayed: Int = 0,
    var attackClear: Int = 0,
    var waveClear: Int = 0,
    var pvpWinCount: Short = 0,
    var pvpLoseCount: Short = 0,
    var pvpEliminatedCount: Short = 0,
    var pvpMvpCount: Short = 0,
    var permission: String = "default",
    var accountID: String? = null,
    var accountPW: String? = null,
    var discordID: String? = null,
    var chatMuted: Boolean = false,
    var effectVisibility: Boolean = false,
    var effectLevel: Short? = null,
    var effectColor: String? = null,
    var hideRanking: Boolean = false,
    var strictMode: Boolean = false,
    var lastLoginDate: LocalDateTime,
    var lastLogoutDate: LocalDateTime? = null,
    var lastPlayedWorldName: String? = null,
    var lastPlayedWorldMode: String? = null,
    var isConnected: Boolean = false,
    var isBanned: Boolean = false,
    var banExpireDate: LocalDateTime? = null,
    var attendanceDays: Int = 0
) {
    // Exp
    var expMultiplier: Double = 1.0
    var currentExp: Int = 0
    var currentPlayTime: Int = 0

    // AFK
    var afk = false
    var afkTime: UShort = 0u
    var mousePosition: Float = 0F

    // Logging
    var viewHistoryMode = false
    var mouseTracking = false

    // Used by voting
    val entityId = playerNumber

    // Statistics
    var currentUnitDestroyedCount = 0
    var currentBuildDestroyedCount = 0
    var currentBuildAttackCount = 0

    // APM (Actions Per Minute)
    var apm = 0
    var apmTimestamps = mutableListOf<Long>()

    // achievements status
    var achievementStatus = mutableListOf<String>()

    var animatedName = false

    var player: Playerc = Player.create()
    val status = mutableMapOf<String, String>()
    val bundle: Bundle get() = Bundle(player.locale())

    fun send(bundle: Bundle, key: String, vararg args: Any) = send(
        bundle.get(key, *args)
    )

    fun send(key: String, vararg args: Any) {
        val message = bundle.get(key, *args)
        player.sendMessage(message)
        lastReceivedMessage = message
    }

    fun err(key: String, vararg args: Any) {
        val message = "[scarlet]" + bundle.get(key, *args)
        player.sendMessage(message)
        lastReceivedMessage = message
    }

    var lastReceivedMessage: String = ""
        set(value) {
            Log.debug("Plugin send message to ${player.name()}: $value")
            field = value
        }

    /**
     * Send a direct message to the player without looking up a bundle resource.
     * This is useful for sending messages that are not localized.
     * @param message The message to send
     */
    fun sendDirect(message: String) {
        player.sendMessage(message)
        lastReceivedMessage = message
    }
}

/** 플레이어 데이터 생성 */
suspend fun createPlayerData(player: Playerc): PlayerData {
    return createPlayerData(player.name(), player.uuid())
}

suspend fun createPlayerData(name: String, uuid: String): PlayerData {
    val id = newSuspendedTransaction {
        PlayerTable.insertReturning {
            it[PlayerTable.name] = name
            it[PlayerTable.uuid] = uuid
        }.single()[PlayerTable.id]
    }

    val entity = newSuspendedTransaction {
        val query = PlayerTable.select(PlayerTable.columns)
            .where { PlayerTable.id eq id }

        query.map { row ->
            row.toPlayerData()
        }.first()
    }

    return entity
}

suspend fun createPlayerData(name: String, uuid: String, accountID: String, accountPW: String): PlayerData {
    val id = newSuspendedTransaction {
        PlayerTable.insertReturning {
            it[PlayerTable.name] = name
            it[PlayerTable.uuid] = uuid
            it[PlayerTable.accountID] = accountID
            it[PlayerTable.accountPW] = BCrypt.hashpw(accountPW, BCrypt.gensalt())
        }.single()[PlayerTable.id]
    }

    val data = newSuspendedTransaction {
        val query = PlayerTable.select(PlayerTable.columns)
            .where { PlayerTable.id eq id }

        query.mapToPlayerDataList().first()
    }

    return data
}

/** 플레이어 데이터 읽기 */
suspend fun getPlayerData(uuid: String): PlayerData? {
    return newSuspendedTransaction {
        PlayerTable.selectAll()
            .where { PlayerTable.uuid eq uuid }
            .mapToPlayerDataList()
    }.firstOrNull()
}

fun getPlayerDataByName(name: String): PlayerData? {
    return transaction {
        PlayerTable.selectAll()
            .where { PlayerTable.name eq name }
            .mapToPlayerDataList()
    }.firstOrNull()
}

/** 동기식으로 플레이어 데이터 읽기 (클래스로더 브리지용) */
fun getPlayerDataSync(uuid: String): PlayerData? {
    return transaction {
        PlayerTable.selectAll()
            .where { PlayerTable.uuid eq uuid }
            .mapToPlayerDataList()
    }.firstOrNull()
}

// 외부 플러그인에서 사용
suspend fun getAllPlayerData(): List<PlayerData> {
    return newSuspendedTransaction {
        PlayerTable.selectAll().map { row -> row.toPlayerData() }
    }
}

// 외부 플러그인에서 사용
suspend fun getPlayerDataByDiscord(discordID: String): PlayerData? {
    return newSuspendedTransaction {
        PlayerTable.selectAll()
            .where { PlayerTable.discordID eq discordID }
            .mapToPlayerDataList()
    }.firstOrNull()
}