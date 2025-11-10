package essential.feature.bridge

import essential.common.database.data.PlayerData
import ksp.command.ClientCommand
import mindustry.gen.Call

class Commands {
    @ClientCommand(
        name = "broadcast",
        parameter = "<message...>",
        description = "Send message to all connected servers"
    )
    fun broadcast(playerData: PlayerData?, arg: Array<out String>) {
        val message = arg[0]

        if (BridgeService.Companion.isServerMode) {
            (BridgeService.Companion.network as Server).sendAll("message", message)
            (BridgeService.Companion.network as Server).lastSentMessage = message
            Call.sendMessage(message)
        } else {
            (BridgeService.Companion.network as Client).message(message)
        }
    }
}
