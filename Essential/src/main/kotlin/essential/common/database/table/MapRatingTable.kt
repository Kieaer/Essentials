package essential.common.database.table

import org.jetbrains.exposed.v1.core.Table

object MapRatingTable : Table("map_ratings") {
    val id = uinteger("id").autoIncrement().uniqueIndex()
    val mapName = varchar("map_name", 100)
    val mapHash = varchar("map_hash", 100).uniqueIndex()
    val playerUuid = varchar("player_uuid", 25).uniqueIndex()
    val isUpvote = bool("is_upvote")

    override val primaryKey = PrimaryKey(id)
}
