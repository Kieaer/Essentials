package essential.common.database.data

import essential.common.database.WorldHistoryBuffer
import essential.common.database.table.WorldHistoryTable
import essential.common.database.worldHistoryDatabase
import ksp.table.GenerateCode
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@GenerateCode(db = "worldHistoryDatabase")
data class WorldHistoryData @OptIn(ExperimentalTime::class) constructor(
    val id: UInt,
    val time: Long,
    val player: String,
    val action: String,
    val x: Short,
    val y: Short,
    val tile: String,
    val rotate: Int,
    val team: String,
    val value: String?,
    val createdAt: Instant
)

/**
 * Get all world history entries
 */
suspend fun getAllWorldHistory(): List<WorldHistoryData> {
    return suspendTransaction(db = worldHistoryDatabase) {
        WorldHistoryTable.selectAll()
            .mapToWorldHistoryDataList()
    }
}

/**
 * Get world history entries by coordinates
 */
suspend fun getWorldHistoryByCoordinates(x: Short, y: Short): List<WorldHistoryData> {
    return suspendTransaction(db = worldHistoryDatabase) {
        WorldHistoryTable.selectAll()
            .where { (WorldHistoryTable.x eq x) and (WorldHistoryTable.y eq y) }
            .mapToWorldHistoryDataList()
    }
}

/**
 * Clear all world history entries
 */
suspend fun clearWorldHistory() {
    suspendTransaction(db = worldHistoryDatabase) {
        exec("TRUNCATE TABLE world_history")
    }
    WorldHistoryBuffer.clear()
}
