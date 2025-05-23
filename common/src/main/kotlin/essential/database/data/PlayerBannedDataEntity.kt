package essential.database.data

import essential.database.table.PlayerBannedTable
import mindustry.gen.Playerc
import mindustry.net.Administration
import org.jetbrains.exposed.dao.UIntEntity
import org.jetbrains.exposed.dao.UIntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.json.contains
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class PlayerBannedDataEntity(id: EntityID<UInt>) : UIntEntity(id) {
    companion object : UIntEntityClass<PlayerBannedDataEntity>(PlayerBannedTable)

    var names by PlayerBannedTable.names
    var ips by PlayerBannedTable.ips
    var uuid by PlayerBannedTable.uuid
    var reason by PlayerBannedTable.reason
    var date by PlayerBannedTable.date
}

/** 차단된 플레이어의 정보를 DB 에 추가 합니다. */
suspend fun createBanInfo(data: Administration.PlayerInfo, reason: String?) {
    return newSuspendedTransaction {
        PlayerBannedDataEntity.new {
            names = data.names.toList()
            ips = data.ips.toList()
            uuid = data.id
            this.reason = reason ?: "Banned by an administrator."
            date = System.currentTimeMillis()
        }
    }
}

/** 플레이어의 UUID를 차단 해제 합니다. */
suspend fun removeBanInfoByUUID(uuid: String) {
    return newSuspendedTransaction {
        PlayerBannedDataEntity.find {
            PlayerBannedTable.uuid eq uuid
        }.firstOrNull()?.delete()
    }
}

/** 플레이어의 IP 차단을 해제 합니다. */
suspend fun removeBanInfoByIP(ip: String) {
    return newSuspendedTransaction {
        PlayerBannedDataEntity.find {
            PlayerBannedTable.ips.contains(ip)
        }
    }
}

/** 해당 플레이어가 차단 되어 있는지 확인 합니다. */
suspend fun checkPlayerBanned(player: Playerc): Boolean {
    return newSuspendedTransaction {
        PlayerBannedDataEntity.find {
            (PlayerBannedTable.uuid eq player.uuid()) or
                    (PlayerBannedTable.names.contains(player.name())) or
                    (PlayerBannedTable.ips.contains(player.ip()))
        }.firstOrNull() != null
    }
}