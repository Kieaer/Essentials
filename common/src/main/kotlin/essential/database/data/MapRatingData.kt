package essential.database.data

import essential.database.table.MapRatingTable
import kotlinx.datetime.LocalDateTime
import ksp.table.GenerateCode
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.insertReturning
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update

@GenerateCode
data class MapRatingData(
    val id: UInt,
    val mapName: String,
    val mapHash: String,
    val playerUuid: String,
    val isUpvote: Boolean,
    val ratedAt: LocalDateTime
)

/**
 * Create a new map rating
 */
suspend fun createMapRating(
    mapName: String,
    mapHash: String,
    playerUuid: String,
    isUpvote: Boolean
): MapRatingData {
    return newSuspendedTransaction {
        MapRatingTable.insertReturning {
            it[MapRatingTable.mapName] = mapName
            it[MapRatingTable.mapHash] = mapHash
            it[MapRatingTable.playerUuid] = playerUuid
            it[MapRatingTable.isUpvote] = isUpvote
        }.single().toMapRatingData()
    }
}

/**
 * Get a map rating by player UUID and map name
 */
suspend fun getMapRating(playerUuid: String, mapName: String): MapRatingData? {
    return newSuspendedTransaction {
        MapRatingTable.selectAll()
            .where { (MapRatingTable.playerUuid eq playerUuid) and (MapRatingTable.mapName eq mapName) }
            .mapToMapRatingDataList()
            .firstOrNull()
    }
}

/**
 * Get all map ratings for a specific map
 */
suspend fun getMapRatings(mapName: String): List<MapRatingData> {
    return newSuspendedTransaction {
        MapRatingTable.selectAll()
            .where { MapRatingTable.mapName eq mapName }
            .mapToMapRatingDataList()
    }
}

/**
 * Get all map ratings by a specific player
 */
suspend fun getPlayerMapRatings(playerUuid: String): List<MapRatingData> {
    return newSuspendedTransaction {
        MapRatingTable.selectAll()
            .where { MapRatingTable.playerUuid eq playerUuid }
            .mapToMapRatingDataList()
    }
}

/**
 * Update an existing map rating or create a new one if it doesn't exist
 */
suspend fun updateOrCreateMapRating(
    mapName: String,
    mapHash: String,
    playerUuid: String,
    isUpvote: Boolean
): MapRatingData {
    val existing = getMapRating(playerUuid, mapName)

    return if (existing != null) {
        // If the rating exists but the vote is different, update it
        if (existing.isUpvote != isUpvote) {
            newSuspendedTransaction {
                MapRatingTable.update({ (MapRatingTable.playerUuid eq playerUuid) and (MapRatingTable.mapName eq mapName) }) {
                    it[MapRatingTable.isUpvote] = isUpvote
                }
            }
            getMapRating(playerUuid, mapName)!!
        } else {
            existing
        }
    } else {
        // Create a new rating
        createMapRating(mapName, mapHash, playerUuid, isUpvote)
    }
}

/**
 * Migrate map ratings from PluginData to the new MapRating table
 */
suspend fun migrateMapRatingsFromPluginData(pluginData: PluginData) {
    newSuspendedTransaction {
        // Iterate through all map ratings in PluginData
        for ((mapName, ratings) in pluginData.data.mapRatings) {
            // For each map, iterate through all player ratings
            for ((playerUuid, isUpvote) in ratings) {
                // Create a new MapRating entry
                // We don't have the map hash, so we'll use an empty string for now
                try {
                    MapRatingTable.insertReturning {
                        it[MapRatingTable.mapName] = mapName
                        it[MapRatingTable.mapHash] = "" // Empty hash as we don't have it
                        it[MapRatingTable.playerUuid] = playerUuid
                        it[MapRatingTable.isUpvote] = isUpvote
                    }
                } catch (e: Exception) {
                    // If there's an error (like duplicate entry), just log it and continue
                    println("Error migrating map rating for map $mapName and player $playerUuid: ${e.message}")
                }
            }
        }
    }
}
