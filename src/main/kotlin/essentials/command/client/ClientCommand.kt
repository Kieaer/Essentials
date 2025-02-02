package essentials.command.client

import arc.util.CommandHandler.CommandRunner
import mindustry.gen.Player

data class ClientCommand(
    val command: String, val parameter: String? = null, val description: String, val runner: CommandRunner<Player>
)
