package essential.common.database.table

import org.jetbrains.exposed.v1.core.Table

object MapRatingTable : Table("map_ratings") {
    val id = uinteger("id").autoIncrement().uniqueIndex()
    val mapName = varchar("map_name", 100)
    val mapHash = varchar("map_hash", 100)
    val playerUuid = varchar("player_uuid", 25).uniqueIndex()
    val difficulty = integer("difficulty").default(3)
    val rating = integer("rating").default(3)

    override val primaryKey = PrimaryKey(id)
}
