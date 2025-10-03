package essential.common.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object ServerRoutingTable : Table("server_routing") {
    val id = uinteger("id").autoIncrement().uniqueIndex()
    val playerUuid = varchar("player_uuid", 25).index()
    val hubServerName = varchar("hub_server_name", 100)
    val targetServerName = varchar("target_server_name", 100)
    val hubConnectionTime = datetime("hub_connection_time").defaultExpression(CurrentDateTime)
    val routingAllowedTime = datetime("routing_allowed_time").defaultExpression(CurrentDateTime)
    val isUsed = bool("is_used").default(false)
    val usedTime = datetime("used_time").nullable().default(null)
    val expiresAt = datetime("expires_at")

    override val primaryKey = PrimaryKey(id)
}