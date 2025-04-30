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

class PlayerBannedData(id: EntityID<UInt>) : UIntEntity(id) {
    companion object : UIntEntityClass<PlayerBannedData>(PlayerBannedTable)

    var names by PlayerBannedTable.names
    var ips by PlayerBannedTable.ips
    var uuid by PlayerBannedTable.uuid
    var reason by PlayerBannedTable.reason
    var date by PlayerBannedTable.date
}

/** 차단된 플레이어의 정보를 DB 에 추가 합니다. */
suspend fun createBanInfo(data: Administration.PlayerInfo, reason: String) {
    return newSuspendedTransaction {
        PlayerBannedData.new {
            names = data.names.toList()
            ips = data.ips.toList()
            uuid = data.id
            this.reason = reason
            date = System.currentTimeMillis()
        }
    }
}

/** 해당 플레이어가 차단 되어 있는지 확인 합니다. */
suspend fun checkPlayerBanned(player: Playerc): Boolean {
    return newSuspendedTransaction {
        PlayerBannedData.find {
            (PlayerBannedTable.uuid eq player.uuid()) or
                    (PlayerBannedTable.names.contains(player.name())) or
                    (PlayerBannedTable.ips.contains(player.ip()))
        }.firstOrNull() != null
    }
}