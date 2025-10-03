package essential.common.database.table

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime

object MapRatingTable : Table("map_ratings") {
    val id = uinteger("id").autoIncrement().uniqueIndex()
    val mapName = varchar("map_name", 100)
    val mapHash = varchar("map_hash", 100).uniqueIndex()
    val playerUuid = varchar("player_uuid", 25).uniqueIndex()
    val isUpvote = bool("is_upvote")
    val ratedAt = datetime("rated_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}
