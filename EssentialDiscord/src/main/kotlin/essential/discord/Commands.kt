package essential.discord

import ksp.command.ClientCommand
import essential.database.data.PlayerData
import essential.event.CustomEvents.DiscordURLOpen
import mindustry.gen.Playerc
import arc.Events

class Commands {
    @ClientCommand(name = "discord", description = "Open server discord url")
    fun discord(player: Playerc, playerData: PlayerData?, arg: Array<String?>?) {
        val url = Main.Companion.conf?.url
        if (url != null && url.isNotEmpty()) {
            mindustry.gen.Call.openURI(player.con(), url)
            playerData?.let { Events.fire(DiscordURLOpen(it)) }
        }
    }
}
