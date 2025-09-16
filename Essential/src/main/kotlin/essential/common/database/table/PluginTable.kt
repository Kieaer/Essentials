package essential.common.database.table

import essential.common.DATABASE_VERSION
import org.jetbrains.exposed.sql.Table

object PluginTable : Table("plugin_data") {
    val id = uinteger("id").autoIncrement().uniqueIndex()
    var databaseVersion = ubyte("database_version").default(DATABASE_VERSION)
    var hubMapName = text("hub_map_name").nullable().default(null)
    var data = text("data")

    override val primaryKey = PrimaryKey(id)
}