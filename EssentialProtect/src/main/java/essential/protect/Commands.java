package essential.protect;

import essential.core.Bundle;
import essential.core.DB;
import essential.core.annotation.ClientCommand;
import mindustry.gen.Playerc;

import java.util.Objects;

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
                player.sendMessage(bundle.get("command.login.default.password"))
            } else if (result.isConnected()) {
                player.sendMessage(bundle.get("command.login.already"))
            } else {
                if (findPlayerData(result.getUuid()) == null) {
                    database.getPlayers().removeIf(a -> a.getUuid().equals(player.uuid()));
                    result.setOldUUID(result.getUuid());
                    result.setUuid(player.uuid());
                    Trigger.loadPlayer(player, result, true)
                } else {
                    player.sendMessage(bundle.get("command.login.already"))
                }
            }
        } else {
            playerData.err("command.login.not.found")
        }
    }

    @ClientCommand(name = "reg", parameter = "<id> <password> <password_repeat>", description = "Register account")
    void register(Playerc player, DB.PlayerData playerData, String[] arg) {
        // todo protect 으로 이동
        if (conf.getAccount().getAuthType() != Config.Account.AuthType.None) {
            if (arg.length != 3) {
                playerData.send("command.reg.usage")
            } else if (!Objects.equals(arg[1], arg[2])) {
                playerData.err("command.reg.incorrect")
            } else {
                if (transaction {
                    DB.Player.selectAll().where {
                        DB.Player.accountID.eq(arg[0]).and(DB.Player.uuid.eq(player.uuid()))
                                .and(DB.Player.oldUUID.eq(player.uuid()))
                    }.firstOrNull()
                } == null) {
                    Trigger.createPlayer(player, arg[0], arg[1])
                    Log.info(new Bundle().get("log.data_created", player.plainName()))
                } else {
                    playerData.err("command.reg.exists");
                }
            }
        } else {
            playerData.err("command.reg.unavailable");
        }
    }
}
