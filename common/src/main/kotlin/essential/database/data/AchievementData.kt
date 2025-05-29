package essential.database.data

import essential.database.table.AchievementTable
import kotlinx.datetime.LocalDateTime
import ksp.table.GenerateCode
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Data class for player achievements
 */
@GenerateCode
data class AchievementData(
    val id: UInt,
    val playerId: UInt,
    val achievementName: String,
    val completedAt: LocalDateTime
)

/**
 * Check if a player has completed an achievement
 */
suspend fun hasAchievement(playerData: PlayerData, achievementName: String): Boolean {
    return newSuspendedTransaction {
        val query = AchievementTable.select(AchievementTable.id)
            .where { 
                (AchievementTable.playerId eq playerData.id) and
                (AchievementTable.achievementName eq achievementName)
            }

        query.firstOrNull() != null
    }
}

/**
 * Set an achievement as completed for a player
 */
suspend fun setAchievement(playerData: PlayerData, achievementName: String) {
    newSuspendedTransaction {
        // Check if the achievement is already completed
        val query = AchievementTable.select(AchievementTable.id)
            .where { 
                (AchievementTable.playerId eq playerData.id) and
                (AchievementTable.achievementName eq achievementName)
            }

        val existing = query.firstOrNull()

        // If not, create a new record
        if (existing == null) {
            AchievementTable.insert {
                it[AchievementTable.playerId] = playerData.id
                it[AchievementTable.achievementName] = achievementName
                // completedAt will be set automatically by the default value
            }
        }
    }
}

/**
 * Get all completed achievements for a player
 */
suspend fun getPlayerAchievements(playerData: PlayerData): List<AchievementData> {
    return newSuspendedTransaction {
        AchievementTable.select(AchievementTable.columns)
            .where { AchievementTable.playerId eq playerData.id }
            .map { row ->
                AchievementData(
                    id = row[AchievementTable.id],
                    playerId = row[AchievementTable.playerId],
                    achievementName = row[AchievementTable.achievementName],
                    completedAt = row[AchievementTable.completedAt]
                )
            }
    }
}
