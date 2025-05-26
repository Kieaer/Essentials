package essential.protect

import arc.Core
import arc.Events
import arc.util.Log
import essential.bundle.Bundle
import ksp.command.ClientCommand
import essential.event.CustomEvents.PlayerDiscordRequested
import essential.event.CustomEvents.PlayerReported
import mindustry.Vars
import arc.struct.ObjectSet
import essential.core.LogType
import essential.core.Main.Companion.scope
import essential.core.Trigger
import essential.core.log
import essential.database.data.PlayerData
import essential.database.data.entity.PlayerDataEntity
import essential.database.data.entity.createPlayerData
import essential.database.data.entity.toData
import essential.database.table.PlayerTable
import essential.protect.Main.Companion.conf
import essential.util.currentTime
import essential.util.findPlayerData
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mindustry.net.Administration.PlayerInfo
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class Commands {
    companion object {
        private const val PLAYER_NOT_FOUND = "player.not.found"
    }
    @ClientCommand(name = "login", parameter = "<id> <password>", description = "Log-in to account.")
    fun login(playerData: PlayerData, arg: Array<out String>) {
        val bundle = playerData.bundle
        val player = playerData.player
        if (arg[0] == arg[1]) {
            player.sendMessage(bundle["command.login.same.password"])
            return
        }

        scope.launch {
            val result = newSuspendedTransaction {
                PlayerDataEntity.find { (PlayerTable.accountID eq arg[0]) and (PlayerTable.accountPW eq arg[1]) }
            }

            Core.app.post {
                if (!result.empty()) {
                    val playerData = result.first()

                    if (playerData.accountID == arg[1]) {
                        player.sendMessage(bundle["command.login.default.password"])
                    } else if (playerData.isConnected) {
                        player.sendMessage(bundle["command.login.already"])
                    } else {
                        if (findPlayerData(playerData.uuid) == null) {
                            Trigger().loadPlayer(playerData.toData())
                        } else {
                            player.sendMessage(bundle["command.login.already"])
                        }
                    }
                } else {
                    playerData.err("command.login.not.found")
                }
            }
        }
    }

    @ClientCommand(name = "reg", parameter = "<id> <password> <password_repeat>", description = "Register account")
    fun register(playerData: PlayerData, arg: Array<out String>) {
        val bundle = playerData.bundle
        val player = playerData.player
        
        if (conf.account.getAuthType() != ProtectConfig.AuthType.None) {
            if (arg.size != 3) {
                player.sendMessage(bundle["command.reg.usage"])
            } else if (arg[1] != arg[2]) {
                player.sendMessage(bundle["command.reg.incorrect"])
            } else {
                val exists = runBlocking {
                    PlayerDataEntity.find {
                        (PlayerTable.accountID eq arg[0]) or (PlayerTable.name eq player.plainName())
                    }.empty()
                }
                if (exists) {
                    player.sendMessage(bundle["command.reg.exists"])
                } else {
                    createPlayerData(player.name(), player.uuid(), arg[0], arg[1])
                    Log.info(bundle["log.data_created", player.plainName()])
                }
            }
        } else if (conf.account.getAuthType() != ProtectConfig.AuthType.Discord) {
            if (Vars.mods.getMod("essential-discord") != null) {
                Events.fire(PlayerDiscordRequested(player.uuid()))
            } else {
                player.sendMessage(bundle["command.reg.unavailable-server"])
            }
        } else {
            player.sendMessage(bundle["command.reg.unavailable"])
        }
    }

    @ClientCommand(name = "report", parameter = "<player> <reason...>", description = "Report a player")
    fun report(playerData: PlayerData, arg: Array<out String>) {
        val player = playerData.player
        val target: ObjectSet<PlayerInfo?> = Vars.netServer.admins.findByName(arg[0])
        target.first()?.let {
            val reason = arg[1]
            val infos: PlayerInfo = Vars.netServer.admins.findByName(it.plainLastName()).first()
            val date = currentTime()
            val text: String? = Bundle()["command.report.texts", it.plainLastName(), player.plainName(), reason, infos.lastName, infos.names, infos.id, infos.lastIP, infos.ips]
            log(LogType.Report, date + text, it.plainLastName())
            Log.info(Bundle()["command.report.received", player.plainName(), it.plainLastName(), reason])
            playerData.send("command.report.done", it.plainLastName())
            Events.fire(PlayerReported(player.plainName(), it.plainLastName(), reason))
        } ?: run {
            playerData.err(PLAYER_NOT_FOUND)
        }
    }
}
