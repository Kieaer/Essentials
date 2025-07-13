package essential.database.table

import essential.systemTimezone
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object WorldHistoryTable : Table("world_history") {
    val id = uinteger("id").autoIncrement().uniqueIndex()
    val time = long("time")
    val player = varchar("player", 100)
    val action = varchar("action", 50)
    val x = short("x")
    val y = short("y")
    val tile = varchar("tile", 100)
    val rotate = integer("rotate")
    val team = varchar("team", 50)
    val value = text("value").nullable()
    val createdAt = datetime("created_at").clientDefault { Clock.System.now().toLocalDateTime(systemTimezone) }

    override val primaryKey = PrimaryKey(id)
}