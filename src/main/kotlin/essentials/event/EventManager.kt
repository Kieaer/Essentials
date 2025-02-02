package essentials.event

import arc.Events
import essentials.Manager
import mindustry.game.EventType.PlayerJoin

class EventManager : Manager {
    override fun initialize() {
        playerJoin()
    }

    override fun terminate() {
        TODO("Not yet implemented")
    }

    private fun playerJoin() {
        Events.on(PlayerJoin::class.java) {

        }
    }
}