package essentials

import mindustry.gen.Playerc

object PluginVars {
    const val buildVersion = 118
    const val buildRevision = 0
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