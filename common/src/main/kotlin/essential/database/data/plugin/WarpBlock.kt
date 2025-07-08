package essential.database.data.plugin

import kotlinx.serialization.Serializable

@Serializable
data class WarpBlock(
    val mapName: String,
    val x: Int,
    val y: Int,
    val tileName: String,
    val size: Int,
    val ip: String,
    val port: Int,
    val description: String
) {
    var online = false
}
