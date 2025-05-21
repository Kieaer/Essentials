package essential.database.table

import org.jetbrains.exposed.dao.id.UIntIdTable
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * Table for storing player achievement data
 */
object AchievementTable : UIntIdTable("player_achievements") {
    // Reference to the player
    val playerId = reference("player_id", PlayerTable)
    
    // The achievement name
    val achievementName = varchar("achievement_name", 50)
    
    // The date when the achievement was completed
    val completedAt = datetime("completed_at").defaultExpression(CurrentDateTime)
    
    // Create a unique index on player_id and achievement_name to prevent duplicates
    init {
        uniqueIndex(playerId, achievementName)
    }
}