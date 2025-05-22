package essential.database.data

import essential.DATABASE_VERSION
import essential.database.data.plugin.WarpBlock
import essential.database.data.plugin.WarpCount
import essential.database.data.plugin.WarpTotal
import essential.database.data.plugin.WarpZone
import essential.database.table.PluginTable
import mindustry.Vars
import org.jetbrains.exposed.dao.UIntEntity
import org.jetbrains.exposed.dao.UIntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

class PluginData(id: EntityID<UInt>) : UIntEntity(id) {
    companion object : UIntEntityClass<PluginData>(PluginTable)

    var pluginVersion by PluginTable.pluginVersion
    var databaseVersion by PluginTable.databaseVersion
    var hubMapName by PluginTable.hubMapName
    var data by PluginTable.data
}

data class DisplayData(
    val warpZone: ArrayList<WarpZone> = arrayListOf(),
    val warpCount: ArrayList<WarpCount> = arrayListOf(),
    val warpTotal: ArrayList<WarpTotal> = arrayListOf(),
    val warpBlock: ArrayList<WarpBlock> = arrayListOf(),
    val blacklistedNames: ArrayList<String> = arrayListOf()
)

/** 플러그인 데이터 업데이트 */
suspend fun PluginData.update() {
    val displayData = this.data
    val hubPort = this.hubMapName

    newSuspendedTransaction {
        PluginTable.update {
            it[PluginTable.pluginVersion] = Vars.mods.getMod("Essential")?.meta?.version ?: "1.0.0"
            it[PluginTable.databaseVersion] = DATABASE_VERSION
            it[PluginTable.hubMapName] = hubPort
            it[PluginTable.data] = displayData
        }
    }
}

/** 플러그인 데이터 읽기 */
suspend fun getPluginData(): PluginData? {
    return newSuspendedTransaction {
        PluginData.all().firstOrNull()
    }
}
