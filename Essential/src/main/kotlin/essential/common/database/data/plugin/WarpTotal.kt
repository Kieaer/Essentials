package essential.common.database.data.plugin

import kotlinx.serialization.Serializable
import mindustry.Vars
import mindustry.world.Tile

@Serializable
data class WarpTotal(val mapName: String, val pos: Int, var totalPlayers: Int, var numberSize: Int) {
    val tile: Tile get() = Vars.world.tile(pos)
}
