package essential.common.database.data.plugin

import kotlinx.serialization.Serializable
import mindustry.Vars
import mindustry.world.Tile

@Serializable
data class WarpCount(
    val mapName: String,
    val pos: Int,
    val ip: String,
    val port: Int,
    var players: Int,
    var numberSize: Int
) {
    val tile: Tile get() = Vars.world.tile(pos)
}
