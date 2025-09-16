package essential.common.database.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.CurrentDateTime
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import kotlin.time.ExperimentalTime

/**
 * Table for storing player achievement data
 */
object AchievementTable : Table("player_achievements") {
    val id = uinteger("id").autoIncrement().uniqueIndex()
    val playerId = uinteger("player_id").references(PlayerTable.id)
    val achievementName = varchar("achievement_name", 50)
    @OptIn(ExperimentalTime::class)
    val completedAt = datetime("completed_at").defaultExpression(CurrentDateTime)
    
    init {
        uniqueIndex(playerId, achievementName)
    }

    override val primaryKey = PrimaryKey(id)
}