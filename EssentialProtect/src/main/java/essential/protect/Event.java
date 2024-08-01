package essential.protect;

import arc.Core;
import arc.Events;
import arc.util.Log;
import essential.core.Bundle;
import essential.core.CustomEvents;
import essential.core.DB;
import essential.core.Trigger;
import mindustry.Vars;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Damage;
import mindustry.game.EventType;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.net.Packets;
import mindustry.world.Tile;
import mindustry.world.blocks.power.PowerGraph;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static essential.core.Event.log;
import static essential.core.Main.daemon;
import static essential.core.Main.database;
import static essential.protect.Main.conf;
import static essential.protect.Main.pluginData;
import static java.lang.Math.abs;

public class Event {
    Integer pvpCount = 0;
    Float originalBlockMultiplier = 0f;
    Float originalUnitMultiplier = 0f;
    String[] coldData;

    void start() {
        Events.on(EventType.WorldLoadEndEvent.class, e -> {
            if (conf.pvp.peace.enabled) {
                originalBlockMultiplier = Vars.state.rules.blockDamageMultiplier;
                originalUnitMultiplier = Vars.state.rules.unitDamageMultiplier;
                Vars.state.rules.blockDamageMultiplier = 0f;
                Vars.state.rules.unitDamageMultiplier = 0f;
                pvpCount = conf.pvp.peace.time;
            }
        });

        Events.on(EventType.Trigger.update.getClass(), e -> {
            if (conf.pvp.peace.enabled) {
                if (pvpCount != 0) {
                    pvpCount--;
                } else {
                    Vars.state.rules.blockDamageMultiplier = originalBlockMultiplier;
                    Vars.state.rules.unitDamageMultiplier = originalUnitMultiplier;
                    for (DB.PlayerData data : database.getPlayers()) {
                        data.send("event.pvp.peace.end");
                    }
                }
            }

            if (conf.pvp.border.enabled) {
                Groups.unit.forEach( unit -> {
                    if (unit.x < 0 || unit.y < 0 || unit.x > (Vars.world.width() * 8) || unit.y > (Vars.world.height() * 8)) {
                        unit.kill();
                    }
                });
            }

            if (conf.protect.unbreakableCore) {
                Vars.state.rules.defaultTeam.cores().forEach ( core -> core.health(1.0E8f));
            }
        });

        Events.on(EventType.ConfigEvent.class, e -> {
            if (conf.protect.powerDetect && e.value instanceof Integer) {
                Building entity = e.tile;
                Tile other = Vars.world.tile((int) e.value);
                boolean valid = other != null && entity.power != null && other.block().hasPower && other.block().outputsPayload && other.block() != Blocks.massDriver && other.block() == Blocks.payloadMassDriver && other.block() == Blocks.largePayloadMassDriver;
                if (valid) {
                    PowerGraph oldGraph = entity.power.graph;
                    PowerGraph newGraph = other.build.power.graph;
                    int oldGraphCount = Arrays.stream(oldGraph.toString()
                            .substring(oldGraph.toString().indexOf("all=["), oldGraph.toString().indexOf("], graph"))
                            .replaceFirst("all=\\[", "").split(",")).toArray().length;
                    int newGraphCount = Arrays.stream(newGraph.toString()
                            .substring(newGraph.toString().indexOf("all=["), newGraph.toString().indexOf("], graph"))
                            .replaceFirst("all=\\[", "").split(",")).toArray().length;
                    if (abs(oldGraphCount - newGraphCount) > 10) {
                        database.getPlayers().forEach( a ->
                                a.send("event.antigrief.node", e.player.name, Math.max(oldGraphCount, newGraphCount), Math.min(oldGraphCount, newGraphCount), e.tile.x + ", " + e.tile.y)
                        );
                    }
                }
            }
        });

        Events.on(EventType.PlayerJoin.class, e -> {
            Trigger trigger = new Trigger();
            e.player.admin(false);

            DB.PlayerData data = database.get(e.player.uuid());
            if (conf.account.getAuthType() == Config.Account.AuthType.None || !conf.account.enabled) {
                if (data == null) {
                    daemon.submit(() -> {
                        if (!trigger.checkUserNameExists(e.player.name)) {
                            Core.app.post(() -> trigger.createPlayer(e.player, null, null));
                        } else {
                            Core.app.post(() -> e.player.con.kick(new Bundle(e.player.locale).get("event.player.name.duplicate"), 0L));
                        }
                    });
                } else {
                    trigger.loadPlayer(e.player, data, false);
                }
            } else {
                if (conf.account.getAuthType() == Config.Account.AuthType.Discord) {
                    if (data == null) {
                        daemon.submit(() -> {
                            if (trigger.checkUserNameExists(e.player.name)) {
                                Core.app.post(() -> e.player.con.kick(new Bundle(e.player.locale).get("event.player.name.duplicate"), 0L));
                            } else {
                                Core.app.post(() -> trigger.createPlayer(e.player, null, null));
                            }
                        });
                    } else {
                        trigger.loadPlayer(e.player, data, false);
                    }
                } else {
                    e.player.sendMessage(new Bundle(e.player.locale).get("event.player.first.register"));
                }
            }
        });

        Events.on(EventType.BlockDestroyEvent.class, e -> {
            if (Vars.state.rules.pvp && conf.pvp.destroyCore && Vars.state.rules.coreCapture) {
                Fx.spawnShockwave.at(e.tile.getX(), e.tile.getY(), Vars.state.rules.dropZoneRadius);
                Damage.damage(
                        Vars.world.tile(e.tile.pos()).team(),
                        e.tile.getX(),
                        e.tile.getY(),
                        Vars.state.rules.dropZoneRadius,
                        1.0E8f,
                        true
                );
            }
        });

        Events.on(CustomEvents.PlayerDataLoaded.class, e -> {
            if (conf.rules.strict) {
                Groups.player.find( p -> Objects.equals(p.uuid(), e.getPlayerData().getUuid())).name(e.getPlayerData().getName());
            }
        });

        Events.on(EventType.ServerLoadEvent.class, e -> {
            if (Vars.mods.getMod("essential-discord") == null) {
                Log.warn(new Bundle().get("command.reg.plugin-enough"));
            }
        });

        if (conf.rules.blockNewUser) {
            enableBlockNewUser();
        }

        Events.on(EventType.ConnectPacketEvent.class, event -> {
            String kickReason = "";
            if (!conf.rules.mobile && event.connection.mobile) {
                event.connection.kick(new Bundle(event.packet.locale).get("event.player.not.allow.mobile"), 0L);
                kickReason = "mobile";
            } else if (conf.rules.minimalName.enabled && conf.rules.minimalName.length > event.packet.name.length()) {
                event.connection.kick(new Bundle(event.packet.locale).get("event.player.name.short"), 0L);
                kickReason = "name.short";
            } else if (conf.rules.vpn) {
                for (String ip : pluginData.vpnList) {
                    IpAddressMatcher match = new IpAddressMatcher(ip);
                    if (match.matches(event.connection.address)) {
                        event.connection.kick(new Bundle(event.packet.locale).get("anti-grief.vpn"));
                        kickReason = "vpn";
                        break;
                    }
                }
            } else if (conf.rules.strict && Vars.netServer.admins.findByName(event.packet.name).size > 1) {
                event.connection.kick(Packets.KickReason.idInUse);
                kickReason = "ip";
            } else if (conf.rules.blockNewUser && !Arrays.asList(coldData).contains(event.packet.uuid)) {
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

    void loadPlayer(DB.PlayerData data) {
        if (conf.account.getAuthType() == Config.Account.AuthType.Discord && data.getDiscord() == null) {
            data.getPlayer().sendMessage(new Bundle(data.getLanguageTag()).get("event.discord.not.registered"));
        }
    }

    void enableBlockNewUser() {
        ArrayList<DB.PlayerData> list = database.getAll();
        coldData = new String[list.size()];

        int size = 0;
        for (DB.PlayerData playerData : list) {
            coldData[size++] = playerData.getUuid();
        }
    }

    class IpAddressMatcher {
        private int nMaskBits = 0;
        private final InetAddress requiredAddress;

        IpAddressMatcher(String ipAddress) {
            String address = ipAddress;
            if (address.indexOf('/') > 0) {
                String[] addressAndMask = address.split("/");
                address = addressAndMask[0];
                nMaskBits = Integer.parseInt(addressAndMask[1]);
            } else {
                nMaskBits = -1;
            }
            requiredAddress = parseAddress(address);
            if (requiredAddress.getAddress().length * 8 < nMaskBits) {
                throw new IllegalArgumentException(
                        String.format("IP address %s is too short for bitmask of length %d", address, nMaskBits)
                );
            }
        }

        boolean matches(String address) {
            InetAddress remoteAddress = parseAddress(address);
            if (!requiredAddress.getClass().equals(remoteAddress.getClass())) {
                return false;
            }
            if (nMaskBits < 0) {
                return remoteAddress.equals(requiredAddress);
            }
            byte[] remAddr = remoteAddress.getAddress();
            byte[] reqAddr = requiredAddress.getAddress();
            int nMaskFullBytes = nMaskBits / 8;
            byte finalByte = (byte) (0xFF00 >> (nMaskBits & 0x07));
            for (int i = 0; i < nMaskFullBytes; i++) {
                if (remAddr[i] != reqAddr[i]) {
                    return false;
                }
            }
            if (finalByte != 0) {
                return (remAddr[nMaskFullBytes] & finalByte) == (reqAddr[nMaskFullBytes] & finalByte);
            } else {
                return true;
            }
        }

        private InetAddress parseAddress(String address) {
            try {
                return InetAddress.getByName(address);
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Failed to parse address " + address, e);
            }
        }
    }
}
