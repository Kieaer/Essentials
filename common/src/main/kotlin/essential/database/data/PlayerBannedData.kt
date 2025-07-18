package essential.database.data

import essential.database.table.PlayerBannedTable
import ksp.table.GenerateCode
import mindustry.gen.Playerc
import mindustry.net.Administration
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.json.contains
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction

@GenerateCode
data class PlayerBannedData(
    val id: UInt,
    var names: List<String>,
    var ips: List<String>,
    var uuid: String,
    var reason: String,
    var date: Long
)

/** 차단된 플레이어의 정보를 DB 에 추가 합니다. */
suspend fun createBanInfo(data: Administration.PlayerInfo, reason: String?) {
    return newSuspendedTransaction {
        PlayerBannedTable.insert {
            it[names] = data.names.toList()
            it[ips] = data.ips.toList()
            it[uuid] = data.id
            it[PlayerBannedTable.reason] = reason ?: "Banned by an administrator."
            it[date] = System.currentTimeMillis()
        }
    }
}

/** 플레이어의 UUID를 차단 해제 합니다. */
suspend fun removeBanInfoByUUID(uuid: String) {
    return newSuspendedTransaction {
        PlayerBannedTable.deleteWhere { PlayerBannedTable.uuid eq uuid }
    }
}

/** 플레이어의 IP 차단을 해제 합니다. */
suspend fun removeBanInfoByIP(ip: String) {
    return newSuspendedTransaction {
        PlayerBannedTable.deleteWhere { PlayerBannedTable.ips.contains(ip) }
    }
}

/** 해당 플레이어가 차단 되어 있는지 확인 합니다. */
fun checkPlayerBanned(player: Playerc): Boolean {
    return checkPlayerBanned(player.uuid(), player.ip(), player.name())
}

/** 해당 플레이어가 차단 되어 있는지 확인 합니다. */
fun checkPlayerBanned(uuid: String, ip: String, name: String): Boolean {
    try {
        return transaction {
            val nameCond =
                PlayerBannedTable.names
                    .castTo<String>(TextColumnType())
                    .like("%\"$name\"%")

            val ipCond =
                PlayerBannedTable.ips
                    .castTo<String>(TextColumnType())
                    .like("%\"$ip\"%")

            PlayerBannedTable
                .selectAll()
                .where { (PlayerBannedTable.uuid eq uuid) or nameCond or ipCond }
                .limit(1)
                .count() > 0
        }
    } catch (e: Exception) {
        arc.util.Log.err("Failed to check if player is banned", e)
        return false
    }
}
