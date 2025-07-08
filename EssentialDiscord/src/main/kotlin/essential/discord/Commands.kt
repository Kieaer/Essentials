package essential.discord

import arc.Events
import essential.database.data.PlayerData
import essential.event.CustomEvents.DiscordURLOpen
import ksp.command.ClientCommand
import mindustry.gen.Call

class Commands {
    @ClientCommand(name = "discord", description = "Open server discord url")
    fun discord(playerData: PlayerData, args: Array<out String>) {
        val url = Main.Companion.conf.url
        if (url.isNotEmpty()) {
            Call.openURI(playerData.player.con(), url)
            playerData.let { Events.fire(DiscordURLOpen(it)) }
        }
    }
}
