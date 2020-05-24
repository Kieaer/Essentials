package essentials.feature;

import arc.Events;
import arc.struct.Array;
import arc.util.Time;
import essentials.core.player.PlayerData;
import essentials.internal.Bundle;
import essentials.internal.Log;
import mindustry.entities.type.Player;
import mindustry.game.EventType;
import mindustry.game.Gamemode;
import mindustry.game.Rules;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.maps.Map;
import mindustry.net.Packets;

import java.util.Timer;
import java.util.TimerTask;

import static essentials.Main.*;
import static mindustry.Vars.*;

public class Vote {
    Timer timer = new Timer(true);

    public Player player;
    public PlayerData playerData;
    private Bundle bundle;

    public Player target;
    public Gamemode gamemode;
    public Map map;

    public VoteType type;
    public Array<String> voted = new Array<>();

    public int require;
    public int time = 0;
    public int message_time = 0;

    public Vote(Player player, VoteType voteType, Object... parameters) {
        this.player = player;
        this.playerData = playerDB.get(player.uuid);
        this.bundle = new Bundle(playerData.locale());
        this.type = voteType;

        require = (int) Math.round((double) vars.playerData().size / 3);

        switch (type) {
            case kick:
                this.target = (Player) parameters[0];
                tool.sendMessageAll("vote.suggester-name", player.name);
                tool.sendMessageAll("vote.kick", target.name);
                break;
            case gameover:
                tool.sendMessageAll("vote.suggester-name", player.name);
                tool.sendMessageAll("vote.gameover");
                break;
            case skipwave:
                tool.sendMessageAll("vote.suggester-name", player.name);
                tool.sendMessageAll("vote.skipwave");
                break;
            case rollback:
                tool.sendMessageAll("vote.suggester-name", player.name);
                tool.sendMessageAll("vote.rollback");
                break;
            case gamemode:
                if (parameters[0] instanceof Gamemode) {
                    this.gamemode = (Gamemode) parameters[0];
                    tool.sendMessageAll("vote.suggester-name", player.name);
                    tool.sendMessageAll("vote-gamemode", gamemode.name());
                } else {
                    player.sendMessage("vote.wrong-gamemode");
                    return;
                }
                break;
            case map:
                if (parameters[0] instanceof Map) {
                    this.map = (Map) parameters[0];
                    tool.sendMessageAll("vote.suggester-name", player.name);
                    tool.sendMessageAll("vote.map", map.name());
                }
                break;
            default:
                player.sendMessage(bundle.get("vote.wrong-mode"));
                return;
        }

        timer.scheduleAtFixedRate(counting, 0, 1000);
        timer.scheduleAtFixedRate(alert, 10000, 10000);
    }

    TimerTask counting = new TimerTask() {
        @Override
        public void run() {
            time++;
            if (time >= 60) success(voted.size >= require);
        }
    };
    TimerTask alert = new TimerTask() {
        @Override
        public void run() {
            Thread.currentThread().setName("Vote alert timertask");
            String[] bundlename = {"vote.count.50", "vote.count.40", "vote.count.30", "vote.count.20", "vote.count.10"};

            if (message_time <= 4) {
                if (playerGroup.size() > 0) {
                    tool.sendMessageAll(bundlename[message_time]);
                }
                message_time++;
            }
        }
    };


    public void success(boolean success) {
        timer.cancel();
        time = 0;
        message_time = 0;
        voted.clear();

        // TODO 투표 성공 메세지 bundle 추가
        if (success) {
            switch (type) {
                case gameover:
                    Log.info("Vote gameover passed!");
                    tool.sendMessageAll("vote.gameover.done");
                    Events.fire(new EventType.GameOverEvent(Team.crux));
                    break;
                case skipwave:
                    Log.info("Vote skipwave passed!");
                    tool.sendMessageAll("vote.skipwave.done");
                    for (int a = 0; a < 10; a++) {
                        logic.runWave();
                    }
                    break;
                case kick:
                    Log.info("Vote kick passed!");
                    playerDB.get(target.uuid).kickcount(playerDB.get(target.uuid).kickcount() + 1);
                    tool.sendMessageAll("vote.kick.done");
                    target.getInfo().lastKicked = Time.millis() + (30 * 60) * 1000;
                    Call.onKick(target.con, Packets.KickReason.vote);
                    Log.write(Log.LogType.player, "log.player.kick");
                    break;
                case rollback:
                    Log.info("Vote rollback passed!");
                    tool.sendMessageAll("vote.rollback.done");
                    rollback.load();
                    break;
                case gamemode:
                    Map m = world.getMap();
                    Rules rules = world.getMap().rules();
                    if (rules.attackMode) rules.attackMode = false;

                    world.loadMap(world.getMap(), rules);
                    // TODO 게임 모드 변경 만들기
                    break;
                case map:
                    Log.info("Vote map passed!");
                    tool.sendMessageAll("vote.map.done");

                    Array<Player> players = new Array<>();
                    for (Player p : playerGroup.all()) {
                        players.add(p);
                        p.setDead(true);
                    }

                    logic.reset();

                    Gamemode current = Gamemode.survival;
                    if (state.rules.attackMode) {
                        current = Gamemode.attack;
                    } else if (state.rules.pvp) {
                        current = Gamemode.pvp;
                    } else if (state.rules.editor) {
                        current = Gamemode.editor;
                    }
                    Call.onWorldDataBegin();

                    world.loadMap(map, (map).applyRules(current));

                    logic.play();

                    for (Player p : players) {
                        if (p.con == null) continue;

                        p.reset();
                        if (state.rules.pvp) {
                            p.setTeam(netServer.assignTeam(p, new Array.ArrayIterable<>(players)));
                        }
                        netServer.sendWorldData(p);
                        Call.onSetRules(p.con, (map).applyRules(current));
                    }
                    tool.sendMessageAll("vote.map.done");
                    break;
            }
        } else {
            switch (type) {
                case gameover:
                    tool.sendMessageAll("vote.gameover.fail");
                    break;
                case skipwave:
                    tool.sendMessageAll("vote.skipwave.fail");
                    break;
                case kick:
                    tool.sendMessageAll("vote.kick.fail", target.name);
                    break;
                case rollback:
                    tool.sendMessageAll("vote.rollback.fail");
                    break;
                case gamemode:
                    tool.sendMessageAll("vote.gamemode.fail");
                    break;
                case map:
                    tool.sendMessageAll("vote.map.fail");
                    break;
            }
        }
        vote.clear();
    }
    public void interrupt() {
        timer.cancel();
        success(voted.size >= require);
    }

    public Array<String> getVoted() {
        return voted;
    }

    public void set(String uuid) {
        voted.add(uuid);
        for (Player others : playerGroup.all()) {
            PlayerData p = playerDB.get(others.uuid);
            if (!p.error() && require - voted.size != -1) {
                others.sendMessage(new Bundle(p.locale()).prefix("vote.current-voted", voted.size, require - voted.size));
            }
        }

        if (voted.size >= require) {
            timer.cancel();
            success(true);
        }
    }

    public enum VoteType {
        gameover, skipwave, kick, rollback, gamemode, map
    }
}
