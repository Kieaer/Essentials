package essential.common.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentTimestamp
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.time.ExperimentalTime

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
    @OptIn(ExperimentalTime::class)
    val createdAt = timestamp("created_at").defaultExpression(CurrentTimestamp)

    override val primaryKey = PrimaryKey(id)
}