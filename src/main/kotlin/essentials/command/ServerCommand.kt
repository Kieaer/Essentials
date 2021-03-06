package essentials.command

import arc.util.CommandHandler
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object ServerCommand {
    lateinit var commands: CommandHandler

    fun register(handler: CommandHandler) {
        handler.register("gendocs", "Generate Essentials README.md"){
            ServerCommandThread(Command.Gendocs, it).run()
        }
        handler.register("lobby", "Toggle lobby server features"){
            ServerCommandThread(Command.Lobby, it).run()
        }
        handler.register("saveall", "Manually save all plugin data"){
            ServerCommandThread(Command.Saveall, it).run()
        }
        handler.register("bansync", "Synchronize ban list with server"){
            ServerCommandThread(Command.BanSync, it).run()
        }
        handler.register("info", "<player/uuid>", "Show player information") {
            ServerCommandThread(Command.Info, it).run()
        }
        handler.register("reload", "Reload Essential plugin data"){
            ServerCommandThread(Command.Reload, it).run()
        }
        handler.register("blacklist","<add/remove> [name]"){
            ServerCommandThread(Command.Blacklist, it).run()
        }

        commands = handler
    }

    enum class Command{
        Gendocs, Lobby, Saveall, BanSync, Info, Reload, Blacklist
    }
}