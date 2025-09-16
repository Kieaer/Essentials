package essential.common.database.data

import essential.common.database.table.ServerRoutingTable
import essential.common.systemTimezone
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.time.Duration.Companion.seconds

data class ServerRoutingData(
    val id: UInt,
    val playerUuid: String,
    val hubServerName: String,
    val targetServerName: String,
    val hubConnectionTime: LocalDateTime,
    val routingAllowedTime: LocalDateTime,
    val isUsed: Boolean,
    val usedTime: LocalDateTime?,
    val expiresAt: LocalDateTime
)

fun ResultRow.toServerRoutingData() = ServerRoutingData(
    id = this[ServerRoutingTable.id],
    playerUuid = this[ServerRoutingTable.playerUuid],
    hubServerName = this[ServerRoutingTable.hubServerName],
    targetServerName = this[ServerRoutingTable.targetServerName],
    hubConnectionTime = this[ServerRoutingTable.hubConnectionTime],
    routingAllowedTime = this[ServerRoutingTable.routingAllowedTime],
    isUsed = this[ServerRoutingTable.isUsed],
    usedTime = this[ServerRoutingTable.usedTime],
    expiresAt = this[ServerRoutingTable.expiresAt]
)

/**
 * 허브 서버를 통한 라우팅 권한을 부여합니다
 */
fun grantRoutingPermission(playerUuid: String, hubServerName: String, targetServerName: String, validSeconds: Int = 10): ServerRoutingData? {
    val now = Clock.System.now().toLocalDateTime(systemTimezone)
    val expiresAt = (Clock.System.now() + validSeconds.seconds).toLocalDateTime(systemTimezone)
    
    return transaction {
        ServerRoutingTable.insertReturning {
            it[ServerRoutingTable.playerUuid] = playerUuid
            it[ServerRoutingTable.hubServerName] = hubServerName
            it[ServerRoutingTable.targetServerName] = targetServerName
            it[ServerRoutingTable.hubConnectionTime] = now
            it[ServerRoutingTable.routingAllowedTime] = now
            it[ServerRoutingTable.isUsed] = false
            it[ServerRoutingTable.usedTime] = null
            it[ServerRoutingTable.expiresAt] = expiresAt
        }.singleOrNull()?.toServerRoutingData()
    }
}

/**
 * 플레이어가 특정 서버에 접속할 권한이 있는지 확인합니다
 */
fun checkRoutingPermission(playerUuid: String, targetServerName: String): Boolean {
    val now = Clock.System.now().toLocalDateTime(systemTimezone)
    
    return transaction {
        ServerRoutingTable.selectAll().where {
            (ServerRoutingTable.playerUuid eq playerUuid) and
            (ServerRoutingTable.targetServerName eq targetServerName) and
            (ServerRoutingTable.isUsed eq false) and
            (ServerRoutingTable.expiresAt greater now)
        }.count() > 0
    }
}

/**
 * 라우팅 권한을 사용 처리합니다
 */
fun useRoutingPermission(playerUuid: String, targetServerName: String): Boolean {
    val now = Clock.System.now().toLocalDateTime(systemTimezone)
    
    return transaction {
        val updatedCount = ServerRoutingTable.update({
            (ServerRoutingTable.playerUuid eq playerUuid) and
            (ServerRoutingTable.targetServerName eq targetServerName) and
            (ServerRoutingTable.isUsed eq false) and
            (ServerRoutingTable.expiresAt greater now)
        }) {
            it[isUsed] = true
            it[usedTime] = now
        }
        updatedCount > 0
    }
}

/**
 * 만료된 라우팅 권한을 정리합니다
 */
fun cleanupExpiredRoutingPermissions() {
    val now = Clock.System.now().toLocalDateTime(systemTimezone)
    
    transaction {
        ServerRoutingTable.deleteWhere {
            expiresAt less now
        }
    }
}