package essentials

import arc.func.Boolf
import arc.struct.Array
import mindustry.entities.type.Player

class PluginVars {
    val buildVersion = 104
    val buildRevision = 6
    val configVersion = 13
    var serverIP: String = "127.0.0.1"
    var pluginVersion: String? = null
    var uptime = 0L
    var playtime = 0L
    val playerData = Array<PlayerData>()
    var players = Array<Player>()
    var isPvPPeace = false

    fun removePlayerData(d: Boolf<PlayerData>) {
        playerData.remove(d)
    }

    fun removePlayerData(d: PlayerData) {
        playerData.remove(d)
    }
}