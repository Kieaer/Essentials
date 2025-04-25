package essential.discord

import essential.core.CustomEvents

class Commands {
    @ClientCommand(name = "discord", description = "Open server discord url")
    fun discord(player: Playerc, playerData: PlayerData?, arg: kotlin.Array<kotlin.String?>?) {
        if (!essential.discord.Main.Companion.conf.getUrl().isEmpty()) {
            mindustry.gen.Call.openURI(player.con(), essential.discord.Main.Companion.conf.getUrl())
            arc.Events.< T > fire < T ? > (DiscordURLOpen(playerData))
        }
    }
}
