package essential.common.database.data

import essential.common.database.table.ContributionTable
import essential.common.database.table.PlayerTable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

/**
 * One finished game's contribution score for a player.
 */
data class ContributionData(
    val id: UInt,
    val playerId: UInt,
    val gameMode: String,
    val mapName: String?,
    val score: Double,
    val recordedAt: LocalDateTime
)

/**
 * Record a finished game's contribution score for a player.
 */
suspend fun insertContribution(
    playerData: PlayerData,
    gameMode: String,
    mapName: String?,
    score: Double
) {
    // Temporary (id == 0u) players are not persisted; skip.
    if (playerData.id == 0u) return
    suspendTransaction {
        ContributionTable.insert {
            it[ContributionTable.playerId] = playerData.id
            it[ContributionTable.gameMode] = gameMode
            it[ContributionTable.mapName] = mapName
            it[ContributionTable.score] = score
            // recordedAt is set by the default value
        }
    }
}

/**
 * Number of recorded games for a player.
 */
suspend fun getContributionCount(playerData: PlayerData): Int {
    if (playerData.id == 0u) return 0
    return suspendTransaction {
        ContributionTable.select(ContributionTable.id)
            .where { ContributionTable.playerId eq playerData.id }
            .toList()
            .size
    }
}

/**
 * Arithmetic mean of a player's contribution scores across all recorded games.
 * Returns 0.0 when the player has no recorded games.
 */
suspend fun getAverageContribution(playerData: PlayerData): Double {
    if (playerData.id == 0u) return 0.0
    val scores = suspendTransaction {
        ContributionTable.select(ContributionTable.score)
            .where { ContributionTable.playerId eq playerData.id }
            .map { it[ContributionTable.score] }
            .toList()
    }
    return if (scores.isEmpty()) 0.0 else scores.sum() / scores.size
}

/**
 * One player's aggregated contribution ranking entry.
 */
data class ContributionRanking(
    val playerId: UInt,
    val name: String,
    val average: Double,
    val total: Double,
    val games: Int
)

/**
 * Average-contribution ranking across all players with at least one recorded game.
 * Sorted by average descending, limited to [limit] entries.
 */
suspend fun getContributionRanking(limit: Int = 10): List<ContributionRanking> {
    // Aggregate per player in Kotlin to stay dialect-agnostic (H2/Postgres/MySQL).
    val rows = suspendTransaction {
        ContributionTable.select(ContributionTable.playerId, ContributionTable.score)
            .map { it[ContributionTable.playerId] to it[ContributionTable.score] }
            .toList()
    }
    if (rows.isEmpty()) return emptyList()

    val byPlayer = rows.groupBy({ it.first }, { it.second })

    // Resolve player names.
    val ids = byPlayer.keys
    val names = suspendTransaction {
        PlayerTable.select(PlayerTable.id, PlayerTable.name)
            .map { it[PlayerTable.id] to it[PlayerTable.name] }
            .toList()
            .toMap()
    }

    return byPlayer.map { (id, scores) ->
        ContributionRanking(
            playerId = id,
            name = names[id] ?: id.toString(),
            average = scores.sum() / scores.size,
            total = scores.sum(),
            games = scores.size
        )
    }.sortedByDescending { it.average }.take(limit)
}

/**
 * All recorded contribution rows for a player (most recent first).
 */
suspend fun getPlayerContributions(playerData: PlayerData): List<ContributionData> {
    if (playerData.id == 0u) return emptyList()
    return suspendTransaction {
        ContributionTable.selectAll()
            .where { ContributionTable.playerId eq playerData.id }
            .map { row ->
                ContributionData(
                    id = row[ContributionTable.id],
                    playerId = row[ContributionTable.playerId],
                    gameMode = row[ContributionTable.gameMode],
                    mapName = row[ContributionTable.mapName],
                    score = row[ContributionTable.score],
                    recordedAt = row[ContributionTable.recordedAt]
                )
            }.toList()
    }
}
