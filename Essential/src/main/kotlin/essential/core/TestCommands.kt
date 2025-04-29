package essential.core

import essential.core.annotation.ServerCommand
import essential.core.annotation.ClientCommand
import essential.ksp.GenerateServerCommand
import essential.ksp.GenerateClientCommand
import mindustry.gen.Playerc

class TestCommands {
    @GenerateServerCommand
    @ServerCommand(name = "test-server", parameter = "", description = "A test server command")
    fun testServer(args: Array<String>) {
        println("Test server command executed with args: ${args.joinToString()}")
    }

    @GenerateClientCommand
    @ClientCommand(name = "test-client", parameter = "", description = "A test client command")
    fun testClient(player: Playerc, data: DB.PlayerData, args: Array<String>) {
        println("Test client command executed by ${player.name()} with args: ${args.joinToString()}")
        data.send("You executed the test client command")
    }
}