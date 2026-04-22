package essential.common.database.data

import essential.common.database.table.ServerRoutingTable
import essential.common.systemTimezone
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.insert
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
    val targetPort: Int,
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
    targetPort = this[ServerRoutingTable.targetPort],
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
suspend fun grantRoutingPermission(
    playerUuid: String,
    hubServerName: String,
    targetServerName: String,
    targetPort: Int,
    hubConnectionTime: LocalDateTime,
    validSeconds: Int = 60
): ServerRoutingData? {
    val routingAllowedTime = Clock.System.now().toLocalDateTime(systemTimezone)
    val expiresAt = (Clock.System.now() + validSeconds.seconds).toLocalDateTime(systemTimezone)

    return suspendTransaction {
        ServerRoutingTable.insert {
            it[ServerRoutingTable.playerUuid] = playerUuid
            it[ServerRoutingTable.hubServerName] = hubServerName
            it[ServerRoutingTable.targetServerName] = targetServerName
            it[ServerRoutingTable.targetPort] = targetPort
            it[ServerRoutingTable.hubConnectionTime] = hubConnectionTime
            it[ServerRoutingTable.routingAllowedTime] = routingAllowedTime
            it[ServerRoutingTable.isUsed] = false
            it[ServerRoutingTable.usedTime] = null
            it[ServerRoutingTable.expiresAt] = expiresAt
        }
        ServerRoutingTable.selectAll()
            .where { ServerRoutingTable.playerUuid eq playerUuid }
            .map { row -> row.toServerRoutingData() }
            .singleOrNull()
    }
}

/**
 * Checks whether the player has permission to connect to a specific server.
 */
@OptIn(ExperimentalTime::class)
suspend fun checkRoutingPermission(playerUuid: String, targetPort: Int): Boolean {
    val now = Clock.System.now().toLocalDateTime(systemTimezone)
    
    return suspendTransaction {
        val sort = ServerRoutingTable.selectAll().where {
            (ServerRoutingTable.playerUuid eq playerUuid) and
            (ServerRoutingTable.targetPort eq targetPort) and
            (ServerRoutingTable.isUsed eq false) and
            (ServerRoutingTable.expiresAt greater now)
        }.count()

        sort > 0
    }
}

/**
 * Marks routing permission as used.
 */
@OptIn(ExperimentalTime::class)
suspend fun useRoutingPermission(playerUuid: String, targetPort: Int): Boolean {
    val now = Clock.System.now().toLocalDateTime(systemTimezone)
    
    return suspendTransaction {
        val updatedCount = ServerRoutingTable.update({
            (ServerRoutingTable.playerUuid eq playerUuid) and
            (ServerRoutingTable.targetPort eq targetPort) and
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