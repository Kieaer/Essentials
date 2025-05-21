package essential.database.data

import essential.database.table.AchievementTable
import essential.database.table.PlayerTable
import kotlinx.datetime.LocalDateTime
import org.jetbrains.exposed.dao.UIntEntity
import org.jetbrains.exposed.dao.UIntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

/**
 * Data class for player achievements
 */
class AchievementData(id: EntityID<UInt>) : UIntEntity(id) {
    companion object : UIntEntityClass<AchievementData>(AchievementTable)

    var playerId by AchievementTable.playerId
    var achievementName by AchievementTable.achievementName
    var completedAt by AchievementTable.completedAt
}

/**
 * Check if a player has completed an achievement
 */
suspend fun hasAchievement(playerData: PlayerData, achievementName: String): Boolean {
    return newSuspendedTransaction {
        AchievementData.find {
            (AchievementTable.playerId eq playerData.id) and
            (AchievementTable.achievementName eq achievementName)
        }.firstOrNull() != null
    }
}

/**
 * Set an achievement as completed for a player
 */
suspend fun setAchievement(playerData: PlayerData, achievementName: String) {
    newSuspendedTransaction {
        // Check if the achievement is already completed
        val existing = AchievementData.find {
            (AchievementTable.playerId eq playerData.id) and
            (AchievementTable.achievementName eq achievementName)
        }.firstOrNull()

        // If not, create a new record
        if (existing == null) {
            AchievementData.new {
                this.playerId = playerData.id
                this.achievementName = achievementName
            }
        }
    }
}

/**
 * Get all completed achievements for a player
 */
suspend fun getPlayerAchievements(playerData: PlayerData): List<AchievementData> {
    return newSuspendedTransaction {
        AchievementData.find {
            AchievementTable.playerId eq playerData.id
        }.toList()
    }
}