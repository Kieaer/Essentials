package essential.discord;

import essential.core.DB;
import essential.core.annotation.ClientCommand;
import mindustry.gen.Playerc;

public class Commands {
    @ClientCommand(name = "discord", description = "Open server discord url")
    void discord(Playerc player, DB.PlayerData playerData, String[] arg) {
        if (Config.discordURL.isNotEmpty()) Call.openURI(player.con(), Config.discordURL)
        Events.fire(CustomEvents.DiscordURLOpen(data))
    }
}
