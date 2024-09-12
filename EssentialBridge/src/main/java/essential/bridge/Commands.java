package essential.bridge;

import essential.core.DB;
import essential.core.annotation.ClientCommand;
import mindustry.gen.Call;
import mindustry.gen.Playerc;

public class Commands {
    @ClientCommand(name = "broadcast", parameter = "<message...>", description = "Send message to all connected servers")
    void broadcast(Playerc player, DB.PlayerData playerData, String[] arg) {
        if (Main.isServerMode) {
            ((Server) Main.network).sendAll("message", arg[0]);
            ((Server) Main.network).lastSentMessage = arg[0];
            Call.sendMessage(arg[0]);
        } else {
            ((Client) Main.network).message(arg[0]);
        }
    }
}
