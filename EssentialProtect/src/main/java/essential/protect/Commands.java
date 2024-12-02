package essential.protect;

import arc.Events;
import arc.struct.ObjectSet;
import arc.util.Log;
import essential.core.Event;
import essential.core.*;
import essential.core.annotation.ClientCommand;
import mindustry.Vars;
import mindustry.gen.Playerc;
import mindustry.net.Administration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import static essential.core.Commands.PLAYER_NOT_FOUND;
import static essential.core.Main.database;
import static essential.protect.Main.conf;

public class Commands {
    @ClientCommand(name = "login", parameter = "<id> <password>", description = "Log-in to account.")
    void login(Playerc player, DB.PlayerData playerData, String[] arg) {
        Bundle bundle = new Bundle(player.locale());
        if (Objects.equals(arg[0], arg[1])) {
            player.sendMessage(bundle.get("command.login.same.password"));
            return;
        }

        DB.PlayerData result = database.search(arg[0], arg[1]);
        if (result != null) {
            if (result.getAccountID().equals(result.getAccountPW())) {
                player.sendMessage(bundle.get("command.login.default.password"));
            } else if (result.isConnected()) {
                player.sendMessage(bundle.get("command.login.already"));
            } else {
                if (findPlayerData(result.getUuid()) == null) {
                    database.getPlayers().removeIf(a -> a.getUuid().equals(player.uuid()));
                    result.setOldUUID(result.getUuid());
                    result.setUuid(player.uuid());
                    new Trigger().loadPlayer(player, result, true);
                } else {
                    player.sendMessage(bundle.get("command.login.already"));
                }
            }
        } else {
            playerData.err("command.login.not.found");
        }
    }

    @ClientCommand(name = "reg", parameter = "<id> <password> <password_repeat>", description = "Register account")
    void register(Playerc player, DB.PlayerData playerData, String[] arg) {
        Bundle bundle = new Bundle(player.locale());
        if (conf.account.getAuthType() != Config.Account.AuthType.None) {
            if (arg.length != 3) {
                player.sendMessage(bundle.get("command.reg.usage"));
            } else if (!Objects.equals(arg[1], arg[2])) {
                player.sendMessage(bundle.get("command.reg.incorrect"));
            } else {
                Trigger trigger = new Trigger();
                if (trigger.checkUserExistsInDatabase(player.plainName(), player.uuid())) {
                    player.sendMessage(bundle.get("command.reg.exists"));
                } else {
                    trigger.createPlayer(player, arg[0], arg[1]);
                    Log.info(bundle.get("log.data_created", player.plainName()));
                }
            }
        } else if (conf.account.getAuthType() != Config.Account.AuthType.Discord){
            if (Vars.mods.getMod("essential-discord") != null) {
                Events.fire(new CustomEvents.PlayerDiscordRequested(player.uuid()));
            } else {
                player.sendMessage(bundle.get("command.reg.unavailable-server"));
            }
        } else {
            player.sendMessage(bundle.get("command.reg.unavailable"));
        }
    }

    @ClientCommand(name = "report", parameter = "<player> <reason...>", description = "Report a player")
    void report(Playerc player, DB.PlayerData playerData, String[] arg) {
        ObjectSet<Administration.PlayerInfo> target = Vars.netServer.admins.findByName(arg[0]);
        if (target != null) {
            String reason = arg[1];
            Administration.PlayerInfo infos = Vars.netServer.admins.findByName(target.first().plainLastName()).first();
            String date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String text = new Bundle().get("command.report.texts", target.first()
                    .plainLastName(), player.plainName(), reason, infos.lastName, infos.names, infos.id, infos.lastIP, infos.ips);
            essential.core.Event.log(Event.LogType.Report, date + text, target.first().plainLastName());
            Log.info(new Bundle().get("command.report.received", player.plainName(), target.first().plainLastName(), reason));
            playerData.send("command.report.done", target.first().plainLastName());
            Events.fire(new CustomEvents.PlayerReported(player.plainName(), target.first().plainLastName(), reason));
        } else {
            playerData.err(PLAYER_NOT_FOUND);
        }
    }

    DB.PlayerData findPlayerData(String uuid) {
        for (DB.PlayerData data : database.getPlayers()) {
            if ((data.getOldUUID() != null && data.getOldUUID().equals(uuid)) || data.getUuid().equals(uuid)) {
                return data;
            }
        }
        return null;
    }
}
