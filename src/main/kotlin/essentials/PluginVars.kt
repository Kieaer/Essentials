package essentials

import mindustry.gen.Playerc

class PluginVars {
    val buildVersion = 104
    val buildRevision = 6
    val configVersion = 13
    var serverIP: String = "127.0.0.1"
    var pluginVersion: String? = null
    var uptime = 0L
    var playtime = 0L
    val playerData = ArrayList<PlayerData>()
    var players = ArrayList<Playerc>()
    var isPvPPeace = false

    fun removePlayerData(d: PlayerData) {
        playerData.remove(d)
    }
}