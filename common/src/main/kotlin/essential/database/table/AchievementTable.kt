package essential.database.table

import essential.systemTimezone
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.dao.id.UIntIdTable
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

/**
 * Table for storing player achievement data
 */
object AchievementTable : Table("player_achievements") {
    val id = uinteger("id").autoIncrement().uniqueIndex()
    val playerId = uinteger("player_id").references(PlayerTable.id)
    val achievementName = varchar("achievement_name", 50)
    val completedAt = datetime("completed_at").clientDefault { Clock.System.now().toLocalDateTime(systemTimezone) }
    
    init {
        uniqueIndex(playerId, achievementName)
    }

    override val primaryKey = PrimaryKey(PlayerTable.id)
}