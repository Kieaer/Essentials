package essentials.command

import arc.util.CommandHandler
import essentials.data.Config
import mindustry.gen.Playerc

object ClientCommand {
    lateinit var commands: CommandHandler

    fun register(handler: CommandHandler) {
        if(Config.vote) {
            handler.removeCommand("votekick")
            handler.removeCommand("vote")
            handler.register("vote", "<mode> [parameter...]", "Voting system (Use /vote to check detail commands)") { arg: Array<String>, player: Playerc ->
                ClientCommandWork(Command.Vote, arg, player).run()
            }
        }
        handler.removeCommand("help")

        handler.register("ch", "Send chat to another server.") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Ch, arg, player).run()
        }
        handler.register("changepw", "<new_password> <new_password_repeat>", "Change account password") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Changepw, arg, player).run()
        }
        handler.register("chars", "<Text...>", "Make pixel texts") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Chars, arg, player).run()
        }
        handler.register("color", "Enable color nickname") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Color, arg, player).run()
        }
        handler.register("killall", "Kill all enemy units") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.KillAll, arg, player).run()
        }
        handler.register("help", "[page]", "Show command lists") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Help, arg, player).run()
        }
        handler.register("info", "Show your information") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Info, arg, player).run()
        }
        handler.register("warp", "<zone/block/count/total> [ip] [parameters...]", "Create a server-to-server warp zone.") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Warp, arg, player).run()
        }
        handler.register("kill", "[player]", "Kill player.") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Kill, arg, player).run()
        }
        handler.register("login", "<id> <password>", "Access your account") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Login, arg, player).run()
        }
        handler.register("maps", "[page]", "Show server maps") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Maps, arg, player).run()
        }
        handler.register("me", "<text...>", "broadcast * message") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Me, arg, player).run()
        }
        handler.register("motd", "Show server motd.") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Motd, arg, player).run()
        }
        handler.register("players", "[page]", "Show players list") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Players, arg, player).run()
        }
        handler.register("save", "Auto rollback map early save") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Save, arg, player).run()
        }
        handler.register("router", "Router") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Router, arg, player).run()
        }
        handler.register("register", if(Config.authType == Config.AuthType.Password) "<accountid> <password>" else "", "Register account") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Register, arg, player).run()
        }
        handler.register("spawn", "<unit/block> <name> [amount/rotate]", "Spawn mob in player position") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Spawn, arg, player).run()
        }
        handler.register("status", "Show server status") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Status, arg, player).run()
        }
        handler.register("team", "<team_name>", "Change team") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Team, arg, player).run()
        }
        handler.register("time", "Show server time") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Time, arg, player).run()
        }
        handler.register("tp", "<player>", "Teleport to other players") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Tp, arg, player).run()
        }
        handler.register("weather", "<rain/snow/sandstorm/sporestorm> <seconds>", "Change map light") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Weather, arg, player).run()
        }
        handler.register("mute", "<Player_name>", "Mute/unmute player") { arg: Array<String>, player: Playerc ->
            ClientCommandWork(Command.Mute, arg, player).run()
        }

        commands = handler
    }

    enum class Command {
        Vote, Ch, Changepw, Chars, Color, KillAll, Help, Info, Warp, Kill, Login, Me, Motd, Players, Save, Router, Register, Spawn, Status, Team, Time, Tp, Weather, Mute, Maps
    }
}
