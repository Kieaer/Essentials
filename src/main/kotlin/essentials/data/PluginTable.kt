package essentials.data

import org.jetbrains.exposed.sql.Table

object PluginTable : Table() {
    val key = varchar("key", 256)
    val value = text("value")
}