package essential.bridge;

import essential.core.DB;
import essential.core.annotation.ClientCommand;
import mindustry.gen.Call;
import mindustry.gen.Playerc;

public class Commands {
    Server server = new Server();
    Client client = new Client();

    @ClientCommand(name = "broadcast", parameter = "<message...>", description = "Send message to all connected servers")
    void broadcast(Playerc player, DB.PlayerData playerData, String[] arg) {
        if (Main.isServerMode) {
            server.sendAll("message", arg[0]);
            server.lastSentMessage = arg[0];
            Call.sendMessage(arg[0]);
        } else {
            client.message(arg[0]);
        }
    }
}
