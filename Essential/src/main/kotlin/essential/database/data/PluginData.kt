package essential.database.data

import essential.DATABASE_VERSION
import essential.database.data.plugin.WarpBlock
import essential.database.data.plugin.WarpCount
import essential.database.data.plugin.WarpTotal
import essential.database.data.plugin.WarpZone
import essential.database.table.PluginTable
import kotlinx.serialization.Serializable
import ksp.table.GenerateCode
import mindustry.Vars
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

@GenerateCode
data class PluginData(
    val id: UInt,
    var pluginVersion: String,
    var databaseVersion: UByte,
    var hubMapName: String?,
    var data: DisplayData
)

@Serializable
data class DisplayData(
    val warpZone: ArrayList<WarpZone> = arrayListOf(),
    val warpCount: ArrayList<WarpCount> = arrayListOf(),
    val warpTotal: ArrayList<WarpTotal> = arrayListOf(),
    val warpBlock: ArrayList<WarpBlock> = arrayListOf(),
    val blacklistedNames: ArrayList<String> = arrayListOf(),
    val mapRatings: HashMap<String, HashMap<String, Boolean>> = hashMapOf()
)

/** 플러그인 데이터 읽기 */
suspend fun getPluginData(): PluginData? {
    return newSuspendedTransaction {
        PluginTable.selectAll()
            .mapToPluginDataList()
            .firstOrNull()
    }
}

/** 플러그인 데이터 생성 */
suspend fun createPluginData(hubMapName: String?): PluginData {
    val displayData = DisplayData()
    return newSuspendedTransaction {
        PluginTable.insertReturning {
            it[PluginTable.pluginVersion] = Vars.mods.getMod("Essential").meta.version
            it[PluginTable.databaseVersion] = DATABASE_VERSION
            it[PluginTable.hubMapName] = hubMapName
            it[PluginTable.data] = displayData
        }.single().toPluginData()
    }
}
