package essential.discord;

import arc.Events;
import essential.core.CustomEvents;
import essential.core.DB;
import essential.core.annotation.ClientCommand;
import mindustry.gen.Call;
import mindustry.gen.Playerc;

import static essential.discord.Main.conf;

public class Commands {
    @ClientCommand(name = "discord", description = "Open server discord url")
    void discord(Playerc player, DB.PlayerData playerData, String[] arg) {
        if (!conf.getUrl().isEmpty()) {
            Call.openURI(player.con(), conf.getUrl());
            Events.fire(new CustomEvents.DiscordURLOpen(playerData));
        }
    }
}
