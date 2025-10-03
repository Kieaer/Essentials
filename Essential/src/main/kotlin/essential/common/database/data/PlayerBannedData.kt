package essential.common.database.data

import arc.util.Log
import essential.common.database.table.PlayerBannedTable
import ksp.table.GenerateCode
import mindustry.gen.Playerc
import mindustry.net.Administration
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.json.contains
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

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
    return suspendTransaction {
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
    return suspendTransaction {
        PlayerBannedTable.deleteWhere { PlayerBannedTable.uuid eq uuid }
    }
}

/** 플레이어의 IP 차단을 해제 합니다. */
suspend fun removeBanInfoByIP(ip: String) {
    return suspendTransaction {
        PlayerBannedTable.deleteWhere { PlayerBannedTable.ips.contains(ip) }
    }
}

/** 해당 플레이어가 차단 되어 있는지 확인 합니다. */
suspend fun checkPlayerBanned(player: Playerc): Boolean {
    return checkPlayerBanned(player.uuid(), player.ip(), player.name())
}

/** 해당 플레이어가 차단 되어 있는지 확인 합니다. */
suspend fun checkPlayerBanned(uuid: String, ip: String, name: String): Boolean {
    try {
        return suspendTransaction {
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
        Log.err("Failed to check if player is banned", e)
        return false
    }
}
