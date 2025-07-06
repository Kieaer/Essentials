package essential.bridge;

import essential.database.data.PlayerData;
import ksp.command.ClientCommand;
import mindustry.gen.Call;

/**
 * Commands implementation for the EssentialBridge plugin.
 * Handles client commands.
 */
public class Commands {

    /**
     * Broadcast a message to all connected servers.
     * 
     * @param playerData The player data
     * @param arg The command arguments
     */
    @ClientCommand(
        name = "broadcast",
        parameter = "<message...>",
        description = "Send message to all connected servers"
    )
    public void broadcast(PlayerData playerData, String[] arg) {
        String message = arg[0];

        if (Main.isServerMode) {
            ((Server) Main.network).sendAll("message", message);
            ((Server) Main.network).setLastSentMessage(message);
            Call.sendMessage(message);
        } else {
            ((Client) Main.network).message(message);
        }
    }
}
