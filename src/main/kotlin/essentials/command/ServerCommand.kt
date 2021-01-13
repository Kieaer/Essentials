package essentials.command

import arc.util.CommandHandler
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object ServerCommand {
    lateinit var commands: CommandHandler
    val service: ExecutorService = Executors.newFixedThreadPool(2)

    fun register(handler: CommandHandler) {
        handler.register("gendocs", "Generate Essentials README.md"){
            service.submit(ServerCommandThread(Command.Gendocs, it))
        }
        handler.register("lobby", "Toggle lobby server features"){
            service.submit(ServerCommandThread(Command.Lobby, it))
        }
        handler.register("edit", "<uuid> <name> [value]", "Edit PlayerData directly"){
            service.submit(ServerCommandThread(Command.Edit, it))
        }
        handler.register("saveall", "Manually save all plugin data"){
            service.submit(ServerCommandThread(Command.Saveall, it))
        }
        handler.register("admin", "<name>", "Set admin status to player."){
            service.submit(ServerCommandThread(Command.Admin, it))
        }
        handler.register("bansync", "Synchronize ban list with server"){
            service.submit(ServerCommandThread(Command.BanSync, it))
        }
        handler.register("info", "<player/uuid>", "Show player information") {
            service.submit(ServerCommandThread(Command.Info, it))
        }
        handler.register("setperm", "<player_name/uuid> <group>", "Set player permission"){
            service.submit(ServerCommandThread(Command.Setperm, it))
        }
        handler.register("reload", "Reload Essential plugin data"){
            service.submit(ServerCommandThread(Command.Reload, it))
        }

        commands = handler
    }

    enum class Command{
        Gendocs, Lobby, Edit, Saveall, Admin, BanSync, Info, Setperm, Reload
    }
}