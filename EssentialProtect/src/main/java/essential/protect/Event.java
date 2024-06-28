package essential.protect;

import arc.Core;
import arc.Events;
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
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.world.Build;
import mindustry.world.Tile;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.modules.PowerModule;
import org.jetbrains.exposed.sql.Op;
import org.jetbrains.exposed.sql.SqlExpressionBuilder;
import org.jetbrains.exposed.sql.Transaction;

import java.util.Arrays;
import java.util.Objects;

import static essential.core.Main.daemon;
import static essential.core.Main.database;
import static essential.protect.Main.conf;
import static java.lang.Math.abs;

public class Event {
    Integer pvpCount = 0;
    Float originalBlockMultiplier = 0f;
    Float originalUnitMultiplier = 0f;

    void start() {
        Events.on(EventType.WorldLoadEndEvent.class, e -> {
            if (conf.getPvp().getPeace().isEnabled()) {
                originalBlockMultiplier = Vars.state.rules.blockDamageMultiplier;
                originalUnitMultiplier = Vars.state.rules.unitDamageMultiplier;
                Vars.state.rules.blockDamageMultiplier = 0f;
                Vars.state.rules.unitDamageMultiplier = 0f;
                pvpCount = conf.getPvp().getPeace().getTime();
            }
        });

        Events.on(EventType.Trigger.update.getClass(), e -> {
            if (conf.getPvp().getPeace().isEnabled()) {
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

            if (conf.getPvp().getBorder().isEnabled()) {
                Groups.unit.forEach( unit -> {
                    if (unit.x < 0 || unit.y < 0 || unit.x > (Vars.world.width() * 8) || unit.y > (Vars.world.height() * 8)) {
                        unit.kill();
                    }
                });
            }

            if (conf.getProtect().isUnbreakableCore()) {
                Vars.state.rules.defaultTeam.cores().forEach ( core -> core.health(1.0E8f));
            }
        });

        Events.on(EventType.ConfigEvent.class, e -> {
            if (conf.getProtect().isPowerDetect() && e.value instanceof Integer) {
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
            if (conf.getAccount().getAuthType() == Config.Account.AuthType.Discord) {
                if (data == null) {
                    daemon.submit(() -> {
                        database.executeInTransaction(() -> {
                            if (trigger.checkUserNameExists(e.player.name)) {
                                Core.app.post(() -> e.player.con.kick(new Bundle(e.player.locale).get("event.player.name.duplicate"), 0L));
                            } else {
                                Core.app.post(() -> trigger.createPlayer(e.player, null, null));
                            }

                            return null;
                        });
                    });
                } else {
                    trigger.loadPlayer(e.player, data, false);
                }
            } else if (conf.getAccount().getAuthType() == Config.Account.AuthType.None && data != null) {
                trigger.loadPlayer(e.player, data, false);
            } else if (conf.getAccount().getAuthType() != Config.Account.AuthType.None) {
                e.player.sendMessage(new Bundle(e.player.locale).get("event.player.first.register"));
            } else if (conf.getAccount().getAuthType() == Config.Account.AuthType.None) {
                daemon.submit(() -> {
                    database.executeInTransaction(() -> {
                        if (trigger.checkUserNameExists(e.player.name)) {
                            Core.app.post(() -> trigger.createPlayer(e.player, null, null));
                        } else {
                            Core.app.post(() -> e.player.con.kick(new Bundle(e.player.locale).get("event.player.name.duplicate"), 0L));
                        }

                        return null;
                    });
                });
            }
        });

        Events.on(EventType.BlockDestroyEvent.class, e -> {
            if (Vars.state.rules.pvp && conf.getPvp().isDestroyCore() && Vars.state.rules.coreCapture) {
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
            if (conf.getAccount().getStrict()) {
                Groups.player.find( p -> Objects.equals(p.uuid(), e.getUuid())).name(e.getOldName());
            }
        });
    }

    void loadPlayer(DB.PlayerData data) {
        if (conf.getAccount().getAuthType() == Config.Account.AuthType.Discord && data.getDiscord() == null) {
            data.getPlayer().sendMessage(new Bundle(data.getLanguageTag()).get("event.discord.not.registered"));
        }
    }
}