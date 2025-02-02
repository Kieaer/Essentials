package essentials.command.server

import arc.func.Cons

data class ServerCommand(
    val command: String, val parameter: String? = null, val description: String, val runner: Cons<Array<out String>>
)
