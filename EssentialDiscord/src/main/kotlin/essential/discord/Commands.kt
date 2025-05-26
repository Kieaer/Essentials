package essential.discord

import ksp.command.ClientCommand
import essential.event.CustomEvents.DiscordURLOpen
import arc.Events
import essential.database.data.PlayerData
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
