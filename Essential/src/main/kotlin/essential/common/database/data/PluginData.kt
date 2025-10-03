package essential.common.database.data

import essential.common.DATABASE_VERSION
import essential.common.database.data.plugin.WarpBlock
import essential.common.database.data.plugin.WarpCount
import essential.common.database.data.plugin.WarpTotal
import essential.common.database.data.plugin.WarpZone
import essential.common.database.table.PluginTable
import kotlinx.coroutines.flow.single
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ksp.table.GenerateCode
import org.jetbrains.exposed.v1.r2dbc.insertReturning
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

@GenerateCode
@Serializable
data class PluginData(
    val id: UInt,
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
    return suspendTransaction {
        PluginTable.selectAll()
            .mapToPluginDataList()
            .firstOrNull()
    }
}

/** 플러그인 데이터 생성 */
suspend fun createPluginData(): PluginData {
    val displayData = DisplayData()
    return suspendTransaction {
        PluginTable.insertReturning {
            it[PluginTable.databaseVersion] = DATABASE_VERSION
            it[PluginTable.hubMapName] = null
            it[PluginTable.data] = Json.encodeToString(displayData)
        }.single().toPluginData()
    }
}
