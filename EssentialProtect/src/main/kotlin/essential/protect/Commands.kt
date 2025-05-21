package essential.protect

import arc.Events

class Commands {
    @ClientCommand(name = "login", parameter = "<id> <password>", description = "Log-in to account.")
    fun login(player: Playerc, playerData: PlayerData, arg: kotlin.Array<kotlin.String?>) {
        val bundle: Bundle = Bundle(player.locale())
        if (arg[0] == arg[1]) {
            player.sendMessage(bundle.get("command.login.same.password"))
            return
        }

        val result: PlayerData? = database.search(arg[0], arg[1])
        if (result != null) {
            if (result.getAccountID().equals(result.getAccountPW())) {
                player.sendMessage(bundle.get("command.login.default.password"))
            } else if (result.isConnected()) {
                player.sendMessage(bundle.get("command.login.already"))
            } else {
                if (findPlayerData(result.getUuid()) == null) {
                    database.getPlayers().removeIf({ a -> a.getUuid().equals(player.uuid()) })
                    result.setOldUUID(result.getUuid())
                    result.setUuid(player.uuid())
                    Trigger().loadPlayer(player, result, true)
                } else {
                    player.sendMessage(bundle.get("command.login.already"))
                }
            }
        } else {
            playerData.err("command.login.not.found")
        }
    }

    @ClientCommand(name = "reg", parameter = "<id> <password> <password_repeat>", description = "Register account")
    fun register(player: Playerc, playerData: PlayerData?, arg: kotlin.Array<kotlin.String?>) {
        val bundle: Bundle = Bundle(player.locale())
        if (essential.protect.Main.Companion.conf.account.getAuthType() != AuthType.None) {
            if (arg.size != 3) {
                player.sendMessage(bundle.get("command.reg.usage"))
            } else if (arg[1] != arg[2]) {
                player.sendMessage(bundle.get("command.reg.incorrect"))
            } else {
                val trigger: Trigger = Trigger()
                if (trigger.checkUserExistsInDatabase(player.plainName(), player.uuid())) {
                    player.sendMessage(bundle.get("command.reg.exists"))
                } else {
                    trigger.createPlayer(player, arg[0], arg[1])
                    Log.info(bundle.get("log.data_created", player.plainName()))
                }
            }
        } else if (essential.protect.Main.Companion.conf.account.getAuthType() != AuthType.Discord) {
            if (Vars.mods.getMod("essential-discord") != null) {
                Events.fire(PlayerDiscordRequested(player.uuid()))
            } else {
                player.sendMessage(bundle.get("command.reg.unavailable-server"))
            }
        } else {
            player.sendMessage(bundle.get("command.reg.unavailable"))
        }
    }

    @ClientCommand(name = "report", parameter = "<player> <reason...>", description = "Report a player")
    fun report(player: Playerc, playerData: PlayerData, arg: kotlin.Array<kotlin.String?>) {
        val target: ObjectSet<PlayerInfo?>? = Vars.netServer.admins.findByName(arg[0])
        if (target != null) {
            val reason = arg[1]
            val infos: PlayerInfo = Vars.netServer.admins.findByName(target.first().plainLastName()).first()
            val date = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            val text: kotlin.String? = Bundle().get(
                "command.report.texts",
                target.first()
                    .plainLastName(),
                player.plainName(),
                reason,
                infos.lastName,
                infos.names,
                infos.id,
                infos.lastIP,
                infos.ips
            )
            essential.core.Event.log(Event.LogType.Report, date + text, target.first().plainLastName())
            Log.info(
                Bundle().get(
                    "command.report.received",
                    player.plainName(),
                    target.first().plainLastName(),
                    reason
                )
            )
            playerData.send("command.report.done", target.first().plainLastName())
            Events.fire(PlayerReported(player.plainName(), target.first().plainLastName(), reason))
        } else {
            playerData.err(PLAYER_NOT_FOUND)
        }
    }

    fun findPlayerData(uuid: kotlin.String?): PlayerData? {
        for (data in database.getPlayers()) {
            if ((data.getOldUUID() != null && data.getOldUUID().equals(uuid)) || data.getUuid().equals(uuid)) {
                return data
            }
        }
        return null
    }
}
