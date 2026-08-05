package essential.core.service.bridge

import arc.util.Log
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

        when (val network = BridgeService.network) {
            is Server -> {
                network.sendAll("message", message)
                network.lastSentMessage = message
                Call.sendMessage(message)
            }
            is Client -> network.message(message)
            null -> Log.warn("Bridge is not configured; broadcast was not sent")
        }
    }
}
