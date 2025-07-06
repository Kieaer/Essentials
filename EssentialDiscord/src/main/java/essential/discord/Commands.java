package essential.discord;

import arc.Events;
import essential.database.data.PlayerData;
import essential.event.CustomEvents.DiscordURLOpen;
import ksp.command.ClientCommand;
import mindustry.gen.Call;

/**
 * Commands implementation for the EssentialDiscord plugin.
 */
public class Commands {

    /**
     * Open the Discord URL for a player.
     *
     * @param playerData The player data
     * @param args The command arguments
     */
    @ClientCommand(name = "discord", description = "Open server discord url")
    public void discord(PlayerData playerData, String[] args) {
        String url = Main.conf.getUrl();
        if (!url.isEmpty()) {
            Call.openURI(playerData.getPlayer().con(), url);
            Events.fire(new DiscordURLOpen(playerData));
        }
    }
}