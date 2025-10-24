package essential.discord

import arc.Events
import essential.common.database.data.PlayerData
import essential.common.event.CustomEvents.DiscordURLOpen
import ksp.command.ClientCommand
import mindustry.gen.Call

class Commands {
    @ClientCommand(name = "discord", description = "Open server discord url")
    fun discord(playerData: PlayerData, args: Array<out String>) {
        val url = DiscordService.conf.url
        if (url.isNotEmpty()) {
            Call.openURI(playerData.player.con(), url)
            playerData.let { Events.fire(DiscordURLOpen(it)) }
        }
    }
}
