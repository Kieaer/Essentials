package remake.core.player;

import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.net.Packets;
import remake.external.Tools;
import remake.internal.CrashReport;

import java.sql.SQLException;
import java.util.ArrayList;

import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;
import static remake.Main.*;
import static remake.Vars.serverIP;

public class PlayerCore {
    public static ArrayList<Player> pvpTeam = new ArrayList<>();

    public boolean load(Player player) {
        try {
            PlayerData playerData = playerDB.load(player.uuid);
            if (playerData.error) {
                new CrashReport(new Exception("DATA NOT FOUND"));
                return false;
            }

            playerData.uuid = player.uuid;
            playerData.connected = true;
            playerData.lastdate = tool.getTime();
            playerData.connserver = serverIP;

            if (playerData.banned) {
                netServer.admins.banPlayerID(player.uuid);
                Call.onKick(player.con, Packets.KickReason.banned);
                return false;
            }

            String motd = Tools.getMotd(playerData.locale);
            int count = motd.split("\r\n|\r|\n").length;
            if (count > 10) {
                Call.onInfoMessage(player.con, motd);
            } else {
                player.sendMessage(motd);
            }

            if (config.realname || config.passwordmethod.equals("discord")) player.name = playerData.name;

            playerData.exp = playerData.exp + playerData.joincount;

            if (playerData.colornick) colornick.targets.add(player);

            if (perm.isAdmin(player)) player.isAdmin = true;

            playerData.joincount = playerData.joincount++;

            player.kill();
            if (Vars.state.rules.pvp) {
                boolean match = false;
                for (Player t : pvpTeam) {
                    Team team = t.getTeam();
                    if (playerData.uuid.equals(t.uuid)) {
                        if (Vars.state.teams.get(team).cores.isEmpty()) {
                            break;
                        } else {
                            player.setTeam(team);
                            match = true;
                        }
                    }
                }
                if (!match) {
                    player.setTeam(netServer.assignTeam(player, playerGroup.all()));
                    pvpTeam.add(player);
                }
            } else {
                player.setTeam(Team.sharded);
            }
            return true;
        } catch (SQLException e) {
            new CrashReport(e);
            return false;
        }
    }

    public PlayerData NewData(String name, String uuid, String country, String country_code, String language, boolean connected, String connserver, String permission, Long udid, String email, String accountid, String accountpw) {
        return new PlayerData(
                name,
                uuid,
                country,
                country_code,
                language,
                false,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                "0/0",
                tool.getTime(),
                tool.getTime(),
                "none",
                "none",
                "",
                "00:00.00",
                0,
                0,
                0,
                0,
                0,
                0,
                "none",
                false,
                false,
                true,
                false,
                connected,
                connserver,
                permission,
                false,
                true,
                udid,
                email,
                accountid,
                accountpw
        );
    }
}
