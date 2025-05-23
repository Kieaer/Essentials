package essential.discord

import ksp.command.ClientCommand
import essential.database.data.PlayerDataEntity
import essential.event.CustomEvents.DiscordURLOpen
import arc.Events
import mindustry.gen.Call

class Commands {
    @ClientCommand(name = "discord", description = "Open server discord url")
    fun discord(playerData: PlayerDataEntity, args: Array<out String>) {
        val url = Main.Companion.conf.url
        if (url.isNotEmpty()) {
            Call.openURI(playerData.player.con(), url)
            playerData.let { Events.fire(DiscordURLOpen(it)) }
        }
    }
}
