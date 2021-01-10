package essentials.command

import arc.util.CommandHandler
import essentials.Config
import mindustry.gen.Playerc
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object ClientCommand {
    lateinit var commands: CommandHandler

    fun register(handler: CommandHandler) {
        val service: ExecutorService = Executors.newFixedThreadPool(6)

        if(Config.vote) {
            handler.removeCommand("votekick")
            handler.removeCommand("vote")
            handler.register("vote", "<mode> [parameter...]", "Voting system (Use /vote to check detail commands)"){ arg: Array<String>, player: Playerc ->
                service.submit(ClientCommandThread(Command.Vote, arg, player))
            }
        }
        handler.removeCommand("help")

        handler.register("alert", "Turn on/off alerts"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Login, arg, player))
        }
        handler.register("ch", "Send chat to another server."){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Ch, arg, player))
        }
        handler.register("changepw", "<new_password> <new_password_repeat>", "Change account password"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Changepw, arg, player))
        }
        handler.register("chars", "<Text...>", "Make pixel texts"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Chars, arg, player))
        }
        handler.register("color", "Enable color nickname"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Color, arg, player))
        }
        handler.register("killall", "Kill all enemy units"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.KillAll, arg, player))
        }
        handler.register("help", "[page]", "Show command lists"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Help, arg, player))
        }
        handler.register("info", "Show your information"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Info, arg, player))
        }
        handler.register("warp", "<zone/block/count/total> [ip] [parameters...]", "Create a server-to-server warp zone."){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Warp, arg, player))
        }
        handler.register("kickall", "Kick all players"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.KickAll, arg, player))
        }
        handler.register("kill", "[player]", "Kill player."){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Kill, arg, player))
        }
        handler.register("login", "<id> <password>", "Access your account"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Login, arg, player))
        }
        handler.register("maps", "[page]", "Show server maps"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Maps, arg, player))
        }
        handler.register("me", "<text...>", "broadcast * message"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Me, arg, player))
        }
        handler.register("motd", "Show server motd."){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Motd, arg, player))
        }
        handler.register("players", "[page]", "Show players list"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Players, arg, player))
        }
        handler.register("save", "Auto rollback map early save"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Save, arg, player))
        }
        handler.register("r", "<player> [message]", "Send Direct message to target player"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.R, arg, player))
        }
        handler.register("reset", "<zone/count/total/block> [ip]", "Remove a server-to-server warp zone data."){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Reset, arg, player))
        }
        handler.register("router", "Router"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Router, arg, player))
        }
        handler.register("register", if (Config.passwordMethod.equals("password", ignoreCase = true)) "<accountid> <password>" else "", "Register account"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Register, arg, player))
        }
        handler.register("spawn", "<mob_name> <count> [team] [playerName]", "Spawn mob in player position"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Spawn, arg, player))
        }
        handler.register("setperm", "<player_name> <group>", "Set player permission"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Setperm, arg, player))
        }
        handler.register("status", "Show server status"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Status, arg, player))
        }
        handler.register("suicide", "Kill yourself."){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Suicide, arg, player))
        }
        handler.register("team", "<team_name>", "Change team"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Team, arg, player))
        }
        handler.register("tempban", "<player> <time> <reason>", "Temporarily ban player. time unit: 1 minute"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Ban, arg, player))
        }
        handler.register("time", "Show server time"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Time, arg, player))
        }
        handler.register("tp", "<player>", "Teleport to other players"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Tp, arg, player))
        }
        handler.register("weather", "<rain/snow/sandstorm/sporestorm> <seconds>", "Change map light"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Weather, arg, player))
        }
        handler.register("mute", "<Player_name>", "Mute/unmute player"){ arg: Array<String>, player: Playerc ->
            service.submit(ClientCommandThread(Command.Mute, arg, player))
        }

        commands = handler
    }

    enum class Command{
        Vote, Alert, Ch, Changepw, Chars, Color, KillAll, Help, Info, Warp, KickAll, Kill, Login, Me, Motd, Players, Save, R, Reset, Router, Register, Spawn, Setperm, Status, Team, Ban, Time, Tp, Weather, Mute, Maps, Suicide
    }
}
