package essential.bridge

import essential.core.DB

class Commands {
    @ClientCommand(
        name = "broadcast",
        parameter = "<message...>",
        description = "Send message to all connected servers"
    )
    fun broadcast(player: Playerc?, playerData: PlayerData?, arg: Array<String?>) {
        if (Main.Companion.isServerMode) {
            (Main.Companion.network as Server).sendAll("message", arg[0])
            (Main.Companion.network as Server).lastSentMessage = arg[0]
            Call.sendMessage(arg[0])
        } else {
            (Main.Companion.network as Client).message(arg[0])
        }
    }
}
