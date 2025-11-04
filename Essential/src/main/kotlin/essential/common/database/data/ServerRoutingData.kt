package essential.common.database.data

import essential.common.database.table.ServerRoutingTable
import essential.common.systemTimezone
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SqlExpressionBuilder.less
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insertReturning
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

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
 * Grants routing permission via the hub server.
 */
@OptIn(ExperimentalTime::class)
suspend fun grantRoutingPermission(playerUuid: String, hubServerName: String, targetServerName: String, validSeconds: Int = 10): ServerRoutingData? {
    val now = Clock.System.now().toLocalDateTime(systemTimezone)
    val expiresAt = (Clock.System.now() + validSeconds.seconds).toLocalDateTime(systemTimezone)
    
    return suspendTransaction {
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
 * Checks whether the player has permission to connect to a specific server.
 */
@OptIn(ExperimentalTime::class)
suspend fun checkRoutingPermission(playerUuid: String, targetServerName: String): Boolean {
    val now = Clock.System.now().toLocalDateTime(systemTimezone)
    
    return suspendTransaction {
        ServerRoutingTable.selectAll().where {
            (ServerRoutingTable.playerUuid eq playerUuid) and
            (ServerRoutingTable.targetServerName eq targetServerName) and
            (ServerRoutingTable.isUsed eq false) and
            (ServerRoutingTable.expiresAt greater now)
        }.count() > 0
    }
}

/**
 * Marks routing permission as used.
 */
@OptIn(ExperimentalTime::class)
suspend fun useRoutingPermission(playerUuid: String, targetServerName: String): Boolean {
    val now = Clock.System.now().toLocalDateTime(systemTimezone)
    
    return suspendTransaction {
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
 * Clean up expired routing permissions.
 */
@OptIn(ExperimentalTime::class)
suspend fun cleanupExpiredRoutingPermissions() {
    val now = Clock.System.now().toLocalDateTime(systemTimezone)
    
    suspendTransaction {
        ServerRoutingTable.deleteWhere {
            expiresAt less now
        }
    }
}