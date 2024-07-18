package essential.anti;

import arc.Events;
import arc.net.Server;
import essential.core.Bundle;
import essential.core.CustomEvents;
import essential.core.DB;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.net.Packets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static essential.anti.Main.*;
import static essential.core.Event.log;
import static essential.core.Main.database;

public class Event {
    String[] coldData;

    void load() {
        setNetworkFilter();
        if (conf.getBlockNewUser()) {
            enableBlockNewUser();
        }

        Events.on(EventType.ConnectPacketEvent.class, event -> {
            String kickReason = "";
            if (!conf.isMobile() && event.connection.mobile) {
                event.connection.kick(new Bundle(event.packet.locale).get("event.player.not.allow.mobile"), 0L);
                kickReason = "mobile";
            } else if (conf.getMinimalNameConfig().getEnabled() && conf.getMinimalNameConfig().getLength() > event.packet.name.length()) {
                event.connection.kick(new Bundle(event.packet.locale).get("event.player.name.short"), 0L);
                kickReason = "name.short";
            } else if (conf.isVpn()) {
                for (String ip : pluginData.vpnList) {
                    essential.core.Event.IpAddressMatcher match = new essential.core.Event.IpAddressMatcher(ip);
                    if (match.matches(event.connection.address)) {
                        event.connection.kick(new Bundle(event.packet.locale).get("anti-grief.vpn"));
                        kickReason = "vpn";
                        break;
                    }
                }
            } else if (conf.getStrict() && Vars.netServer.admins.findByName(event.packet.name).size > 1) {
                event.connection.kick(Packets.KickReason.idInUse);
                kickReason = "ip";
            } else if (conf.getBlockNewUser() && !Arrays.asList(coldData).contains(event.packet.uuid)) {
                event.connection.kick(new Bundle(event.packet.locale).get("event.player.new.blocked"), 0L);
                kickReason = "newuser";
            }

            if (!kickReason.isEmpty()) {
                Bundle bundle = new Bundle();
                log(essential.core.Event.LogType.Player, bundle.get("event.player.kick", event.packet.name, event.packet.uuid, event.connection.address, bundle.get("event.player.kick.reason." + kickReason)));
                Events.fire(new CustomEvents.PlayerConnectKicked(event.packet.name, bundle.get("event.player.kick.reason." + kickReason)));
            }
        });
    }

    void setNetworkFilter() {
        Server.ServerConnectFilter filter = s -> !Vars.netServer.admins.bannedIPs.contains(s);
        Vars.platform.getNet().setConnectFilter(filter);
    }

    void enableBlockNewUser() {
        ArrayList<DB.PlayerData> list = database.getAll();
        coldData = new String[list.size()];

        int size = 0;
        for (DB.PlayerData playerData : list) {
            coldData[size++] = playerData.getUuid();
        }
    }
}
