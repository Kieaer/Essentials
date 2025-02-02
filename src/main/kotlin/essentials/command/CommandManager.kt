package essentials.command

import arc.util.CommandHandler
import essentials.Manager
import essentials.command.client.ClientCommand
import essentials.command.client.changeMap
import essentials.command.server.ServerCommand

class CommandManager : Manager {
    val clientCommandList = mutableListOf<ClientCommand>()
    val serverCommandList = mutableListOf<ServerCommand>()

    override fun initialize() {
        registerServerCommands()
        registerClientCommands()
    }

    override fun terminate() {
        TODO("Not yet implemented")
    }

    fun registerServerCommands() {
        val handler = CommandHandler("")
    }

    fun registerClientCommands() {
        val handler = CommandHandler("/")

        changeMap(handler)
    }
}