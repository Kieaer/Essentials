package essentials.core.player;

import essentials.internal.CrashReport;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
import mindustry.net.Packets;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;

import static essentials.Main.*;
import static mindustry.Vars.netServer;

public class PlayerCore {
    public void load(Player player, String... AccountID) {
        playerDB.remove(player.uuid);
        PlayerData playerData;
        if (AccountID.length > 0) {
            playerData = playerDB.load(player.uuid);
        } else {
            playerData = playerDB.load(player.uuid, AccountID);
        }
        if (playerData.error()) {
            new CrashReport(new Exception("DATA NOT FOUND"));
            return;
        }

        if (playerData.banned()) {
            netServer.admins.banPlayerID(player.uuid);
            Call.onKick(player.con, Packets.KickReason.banned);
            return;
        }

        String motd = tool.getMotd(playerData.locale());
        int count = motd.split("\r\n|\r|\n").length;
        if (count > 10) {
            Call.onInfoMessage(player.con, motd);
        } else {
            player.sendMessage(motd);
        }

        if (playerData.colornick()) colornick.targets.add(player);
        if (perm.permission_user.get(playerData.uuid()) == null) {
            perm.create(playerData);
            perm.saveAll();
        } else {
            if (config.realname() || config.passwordmethod().equals("discord")) {
                player.name = playerData.name();
            } else {
                player.name = perm.permission_user.get(playerData.uuid()).asObject().get("name").asString();
            }
        }

        player.isAdmin = perm.isAdmin(player);

        playerData.uuid(player.uuid);
        playerData.connected(true);
        playerData.lastdate(tool.getTime());
        playerData.connserver(vars.serverIP());
        playerData.exp(playerData.exp() + playerData.joincount());
        playerData.joincount(playerData.joincount() + 1);
        playerData.login(true);
    }

    public PlayerData NewData(String name, String uuid, String country, String country_code, String language, boolean connected, String connserver, String permission, Long udid, String accountid, String accountpw) {
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
                false,
                false,
                connected,
                connserver,
                permission,
                false,
                true,
                udid,
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
        try (PreparedStatement pstmt = database.conn.prepareStatement("SELECT * from players WHERE accountid=? AND accountpw=?");
             ResultSet rs = pstmt.executeQuery()) {
            pstmt.setString(1, id);
            pstmt.setString(2, pw);
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
