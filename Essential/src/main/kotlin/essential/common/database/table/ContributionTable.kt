package essential.common.database.table

import essential.common.database.table.ContributionTable.score
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.datetime.CurrentDateTime
import org.jetbrains.exposed.v1.datetime.datetime
import kotlin.time.ExperimentalTime

/**
 * Table for storing per-game contribution scores.
 *
 * One row is inserted per finished game (GameOverEvent) per player, holding that
 * game's final contribution score. The average-per-game contribution is computed
 * by averaging [score] across a player's rows.
 */
object ContributionTable : Table("player_contributions") {
    val id = uinteger("id").autoIncrement()
    val playerId = uinteger("player_id").references(PlayerTable.id)
    val gameMode = varchar("game_mode", 20) // "pvp" | "attack" | "survival"
    val mapName = varchar("map_name", 64).nullable().default(null)
    val score = double("score")
    @OptIn(ExperimentalTime::class)
    val recordedAt = datetime("recorded_at").defaultExpression(CurrentDateTime)

    override val primaryKey = PrimaryKey(id)
}
