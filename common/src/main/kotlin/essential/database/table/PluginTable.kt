package essential.database.table

import essential.database.data.DisplayData
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.dao.id.UIntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.json.json

object PluginTable : UIntIdTable("plugin_data") {
    var pluginVersion = ubyte("plugin_version")
    var databaseVersion = ubyte("database_version")
    var data = json<DisplayData>("data", Json { encodeDefaults = true })
}