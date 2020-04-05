package essentials.core.player;

import essentials.internal.CrashReport;
import mindustry.Vars;
import mindustry.entities.type.Player;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.net.Packets;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;

import static essentials.Main.*;
import static essentials.PluginVars.serverIP;
import static mindustry.Vars.netServer;
import static mindustry.Vars.playerGroup;

public class PlayerCore {
    public ArrayList<Player> pvpTeam = new ArrayList<>();

    public boolean load(Player player, @Nullable String... AccountID) {
        PlayerData playerData = playerDB.load(player.uuid);
        if (playerData.error) {
            new CrashReport(new Exception("DATA NOT FOUND"));
            return false;
        }

        playerData.uuid(player.uuid);
        playerData.connected(true);
        playerData.lastdate(tool.getTime());
        playerData.connserver(serverIP);

        if (playerData.banned) {
            netServer.admins.banPlayerID(player.uuid);
            Call.onKick(player.con, Packets.KickReason.banned);
            return false;
        }

        String motd = tool.getMotd(playerData.locale);
        int count = motd.split("\r\n|\r|\n").length;
        if (count > 10) {
            Call.onInfoMessage(player.con, motd);
        } else {
            player.sendMessage(motd);
        }

        if (config.realname || config.passwordmethod.equals("discord")) player.name = playerData.name;

        playerData.exp(playerData.exp + playerData.joincount);

        if (playerData.colornick) colornick.targets.add(player);

        if (perm.isAdmin(player)) player.isAdmin = true;

        playerData.joincount(playerData.joincount++);

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

        playerData.login(true);
        return true;
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
                "00:00:00",
                0,
                0,
                0,
                0,
                0,
                "00:00:00",
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

    public boolean isLocal(Player player) {
        try {
            InetAddress addr = InetAddress.getByName(netServer.admins.getInfo(player.uuid).lastIP);
            if (addr.isAnyLocalAddress() || addr.isLoopbackAddress()) return true;
            return NetworkInterface.getByInetAddress(addr) != null;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean login(Player player, String id, String pw) {
        try {
            PreparedStatement pstmt = database.conn.prepareStatement("SELECT * from players WHERE accountid=? AND accountpw=?");
            pstmt.setString(1, id);
            pstmt.setString(2, pw);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            new CrashReport(e);
            return false;
        }
    }

    public void tempban(Player player, LocalTime time, String reason) {
        PlayerData playerData = playerDB.get(player.uuid);
        playerData.bantimeset(time.toString());
    }
}
