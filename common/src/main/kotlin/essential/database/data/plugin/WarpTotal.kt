package essential.database.data.plugin

import kotlinx.serialization.Serializable
import mindustry.Vars
import mindustry.world.Tile

data class WarpTotal(val mapName: String, val pos: Int, var totalPlayers: UInt, var numberSize: UInt) {
    val tile: Tile get() = Vars.world.tile(pos)
}
