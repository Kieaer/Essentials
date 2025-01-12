package essentials.database.table

import org.jetbrains.exposed.sql.Table

object PluginData : Table() {
    val key = varchar("key", 256)
    val value = text("value")
}