package essential.database.table

import essential.systemTimezone
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.datetime

object MapRatingTable : Table("map_ratings") {
    val id = uinteger("id").autoIncrement().uniqueIndex()
    val mapName = varchar("map_name", 100)
    val mapHash = varchar("map_hash", 100).uniqueIndex()
    val playerUuid = varchar("player_uuid", 25).uniqueIndex()
    val isUpvote = bool("is_upvote")
    val ratedAt = datetime("rated_at").clientDefault { Clock.System.now().toLocalDateTime(systemTimezone) }

    override val primaryKey = PrimaryKey(id)
}
