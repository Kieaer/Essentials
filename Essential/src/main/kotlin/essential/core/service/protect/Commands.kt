package essential.core.service.protect

import arc.Core
import arc.Events
import arc.struct.ObjectSet
import arc.util.Log
import essential.common.bundle.Bundle
import essential.common.database.data.*
import essential.common.database.table.PlayerTable
import essential.common.event.CustomEvents
import essential.common.event.CustomEvents.PlayerDiscordRequested
import essential.common.event.CustomEvents.PlayerReported
import essential.common.log.LogType
import essential.common.log.writeLog
import essential.common.util.currentTime
import essential.core.Main.Companion.scope
import essential.core.service.protect.ProtectService.Companion.conf
import kotlinx.coroutines.launch
import ksp.command.ClientCommand
import mindustry.Vars
import mindustry.net.Administration.PlayerInfo
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.mindrot.jbcrypt.BCrypt

class Commands {
    companion object {
        private const val PLAYER_NOT_FOUND = "player.not.found"
    }
    @ClientCommand(name = "login", parameter = "<id> <password>", description = "Log-in to account.")
    fun login(playerData: PlayerData, arg: Array<out String>) {
        val bundle = playerData.bundle
        val player = playerData.player
        val currentUuid = player.uuid()

        scope.launch {
            val target = suspendTransaction {
                PlayerTable.selectAll().where { PlayerTable.accountID eq arg[0] }.mapToPlayerDataList()
            }.firstOrNull()
            val existingDeviceAccount = getPlayerData(currentUuid)

            Core.app.post {
                if (target == null) {
                    playerData.err("command.login.not.found")
                    return@post
                }
                val isBcryptHash = target.accountPW != null && (target.accountPW!!.startsWith($$"$2a$") || target.accountPW!!.startsWith($$"$2b$") || target.accountPW!!.startsWith(
                    $$"$2y$"
                ))

                if (!isBcryptHash) {
                    player.sendMessage(bundle["command.login.plaintext"])
                    return@post
                }

                try {
                    if(BCrypt.checkpw(arg[1], target.accountPW!!)) {
                        playerData.err("command.login.not.found")
                        return@post
                    }
                } catch (e: Exception) {
                    playerData.err("command.login.not.found")
                }

                if (target.isConnected) {
                    player.sendMessage(bundle["command.login.already"])
                    return@post
                }

                val hasConflict = existingDeviceAccount != null && existingDeviceAccount.id != target.id
                if (hasConflict) {
                    if (playerData.status["pendingLogin"] != arg[0]) {
                        playerData.status["pendingLogin"] = arg[0]
                        player.sendMessage(bundle["command.login.warn.delete"])
                        return@post
                    }
                    playerData.status.remove("pendingLogin")
                }

                scope.launch {
                    try {
                        if (hasConflict) {
                            deletePlayerData(currentUuid)
                        }
                        rebindAccountUuid(target.id, currentUuid)
                        target.uuid = currentUuid
                        Core.app.post {
                            Events.fire(CustomEvents.PlayerDataLoad(target))
                            player.sendMessage(bundle["command.login.success"])
                        }
                    } catch (e: Exception) {
                        Log.err("Failed to login account ${arg[0]}", e)
                        Core.app.post { playerData.err("command.login.not.found") }
                    }
                }
            }
        }
    }

    @ClientCommand(name = "reg", parameter = "<id> <password> <password_repeat>", description = "Register account")
    fun register(playerData: PlayerData, arg: Array<out String>) {
        val bundle = playerData.bundle
        val player = playerData.player
        
        if (conf.account.getAuthType() == ProtectConfig.AuthType.Password) {
            if (arg.size != 3) {
                player.sendMessage(bundle["command.reg.usage"])
            } else if (arg[1] != arg[2]) {
                player.sendMessage(bundle["command.reg.incorrect"])
            } else {
                val currentUuid = player.uuid()
                scope.launch {
                    val idTaken = !suspendTransaction {
                        PlayerTable.select(PlayerTable.accountID).where {
                            PlayerTable.accountID eq arg[0]
                        }.empty()
                    }
                    if (idTaken) {
                        Core.app.post { player.sendMessage(bundle["command.reg.exists"]) }
                        return@launch
                    }

                    val nameTaken = !suspendTransaction {
                        PlayerTable.select(PlayerTable.name).where {
                            PlayerTable.name eq player.plainName()
                        }.empty()
                    }
                    if (nameTaken) {
                        Core.app.post { player.sendMessage(bundle["command.reg.name.exists"]) }
                        return@launch
                    }

                    val existing = getPlayerData(currentUuid)
                    val data = if (existing != null) {
                        existing.accountID = arg[0]
                        existing.accountPW = BCrypt.hashpw(arg[1], BCrypt.gensalt())
                        existing.permission = "user"
                        existing.update()
                        existing
                    } else {
                        val created = createPlayerData(player.name(), currentUuid, arg[0], arg[1])
                        created.permission = "user"
                        created.update()
                        created
                    }
                    Log.info(bundle["log.data_created", player.plainName()])
                    Core.app.post {
                        Events.fire(CustomEvents.PlayerDataLoad(data))
                        player.sendMessage(bundle["command.reg.success"])
                    }
                }
            }
        } else if (conf.account.getAuthType() == ProtectConfig.AuthType.Discord) {
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
            val text: String = Bundle()["command.report.texts", it.plainLastName(), player.plainName(), reason, infos.lastName, infos.names, infos.id, infos.lastIP, infos.ips]
            writeLog(LogType.Report, date + text, it.plainLastName())
            Log.info(Bundle()["command.report.received", player.plainName(), it.plainLastName(), reason])
            playerData.send("command.report.done", it.plainLastName())
            Events.fire(PlayerReported(player.plainName(), it.plainLastName(), reason))
        } ?: run {
            playerData.err(PLAYER_NOT_FOUND)
        }
    }
}
