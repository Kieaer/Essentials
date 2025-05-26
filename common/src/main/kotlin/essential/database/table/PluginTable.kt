package essential.database.table

import essential.DATABASE_VERSION
import essential.PLUGIN_VERSION
import essential.database.data.entity.DisplayData
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.UIntIdTable
import org.jetbrains.exposed.sql.json.json

object PluginTable : UIntIdTable("plugin_data") {
    var pluginVersion = varchar("plugin_version", 3).default(PLUGIN_VERSION)
    var databaseVersion = ubyte("database_version").default(DATABASE_VERSION)
    var hubMapName = text("hub_map_name").nullable().default(null)
    var data = json<DisplayData>("data", Json { encodeDefaults = true })
}