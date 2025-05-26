package essential.database.data

import essential.bundle.Bundle
import essential.database.data.entity.IPlayerData
import essential.database.data.entity.PlayerDataAdapter
import essential.database.data.entity.PlayerDataEntity
import essential.database.data.entity.update
import essential.playerNumber
import mindustry.gen.Player
import mindustry.gen.Playerc

class PlayerData(
    val entity: PlayerDataEntity
) : IPlayerData by PlayerDataAdapter(entity) {
    // Exp
    var expMultiplier: Double = 1.0
    var currentExp: Int = 0
    var currentPlayTime: Int = 0

    // AFK
    var afk = false
    var afkTime: UShort = 0u
    var mousePosition: Float = 0F

    // Logging
    var viewHistoryMode = false
    var mouseTracking = false

    // Used by voting
    val entityId = playerNumber

    // Statistics
    var currentUnitDestroyedCount = 0
    var currentBuildDestroyedCount = 0
    var currentBuildAttackCount = 0

    // APM (Actions Per Minute)
    var apm = 0
    var apmTimestamps = mutableListOf<Long>()

    var animatedName = false

    var player: Playerc = Player.create()
    val status = mutableMapOf<String, String>()
    val bundle: Bundle get() = Bundle(player.locale())

    fun send(bundle: Bundle, key: String, vararg args: Any) = send(
        bundle.get(key, *args)
    )

    fun send(key: String, vararg args: Any) {
        player.sendMessage(bundle.get(key, *args))
    }

    fun err(key: String, vararg args: Any) {
        player.sendMessage("[scarlet]" + bundle.get(key, *args))
    }
}

suspend fun PlayerData.update() {
    entity.update()
}
