package essential.bridge;

import essential.core.DB;
import essential.core.annotation.ClientCommand;
import mindustry.gen.Call;
import mindustry.gen.Playerc;

public class Commands {
    @ClientCommand(name = "broadcast", parameter = "<message...>", description = "Send message to all connected servers")
    void broadcast(Playerc player, DB.PlayerData playerData, String[] arg) {
        if (Main.connectType) {
            Trigger.Server.sendAll("message", arg[0])
            Trigger.Server.lastSentMessage = arg[0]
            Call.sendMessage(arg[0])
        } else {
            Trigger.Client.message(arg[0])
        }
    }
}
