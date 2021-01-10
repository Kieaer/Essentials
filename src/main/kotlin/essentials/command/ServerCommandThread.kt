package essentials.command

import essentials.command.ClientCommand.Command.*

class ServerCommandThread(private val type: ClientCommand.Command, private val arg: Array<String>) : Thread() {
    override fun run() {
        when (type){
            Vote -> TODO()
            Alert -> TODO()
            Ch -> TODO()
            Changepw -> TODO()
            Chars -> TODO()
            Color -> TODO()
            KillAll -> TODO()
            Help -> TODO()
            Info -> TODO()
            Warp -> TODO()
            KickAll -> TODO()
            Kill -> TODO()
            Login -> TODO()
            Me -> TODO()
            Motd -> TODO()
            Players -> TODO()
            Save -> TODO()
            R -> TODO()
            Reset -> TODO()
            Router -> TODO()
            Register -> TODO()
            Spawn -> TODO()
            Setperm -> TODO()
            Status -> TODO()
            Team -> TODO()
            Ban -> TODO()
            Time -> TODO()
            Tp -> TODO()
            Weather -> TODO()
            Mute -> TODO()
        }
    }
}