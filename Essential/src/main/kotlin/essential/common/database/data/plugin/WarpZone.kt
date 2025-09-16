package essential.common.database.data.plugin

import kotlinx.serialization.Serializable
import mindustry.Vars
import mindustry.world.Tile

@Serializable
data class WarpZone(
    val mapName: String,
    val start: Int,
    val finish: Int,
    val click: Boolean,
    val ip: String,
    val port: Int
) {
    val startTile: Tile get() = Vars.world.tile(start)
    val finishTile: Tile get() = Vars.world.tile(finish)
}