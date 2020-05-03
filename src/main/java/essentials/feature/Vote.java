package essentials.feature;

import arc.Events;
import arc.struct.Array;
import arc.util.Time;
import essentials.core.player.PlayerData;
import essentials.internal.Bundle;
import essentials.internal.Log;
import mindustry.Vars;
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
    public String reason;
    public Gamemode gamemode;
    public Map map;

    public VoteType type;
    public Array<String> voted = new Array<>();

    public double require;
    public int time = 0;
    public int message_time = 0;

    public Vote(Player player, VoteType voteType, Object... parameters) {
        this.player = player;
        this.playerData = playerDB.get(player.uuid);
        this.bundle = new Bundle(playerData.locale());
        this.type = voteType;

        switch (type) {
            case kick:
                this.target = (Player) parameters[0];
                this.reason = (String) parameters[1];
                tool.sendMessageAll("vote.suggester-name", player.name);
                tool.sendMessageAll("vote.reason", reason);
                tool.sendMessageAll("vote.kick", player.name, target.name);
                break;
            case gameover:
                tool.sendMessageAll("vote.gameover");
                break;
            case skipwave:
                tool.sendMessageAll("vote.skipwave");
                break;
            case rollback:
                tool.sendMessageAll("vote.rollback");
                break;
            case gamemode:
                if (parameters[0] instanceof Gamemode) {
                    this.gamemode = (Gamemode) parameters[0];
                    tool.sendMessageAll("vote-gamemode", gamemode.name());
                } else {
                    player.sendMessage("vote.wrong-gamemode");
                    return;
                }
                break;
            case map:
                if (parameters[0] instanceof Map) {
                    this.map = (Map) parameters[0];
                    tool.sendMessageAll("vote.map");
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
            if (time >= 60) success();
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



    public void success() {
        timer.cancel();
        time = 0;
        message_time = 0;
        voted.clear();

        if (voted.size >= require) {
            switch (type) {
                case gameover:
                    tool.sendMessageAll("vote.gameover.done");
                    Events.fire(new EventType.GameOverEvent(Team.crux));
                    break;
                case skipwave:
                    tool.sendMessageAll("vote.skipwave.done");
                    for (int a = 0; a < 10; a++) {
                        logic.runWave();
                    }
                    break;
                case kick:
                    tool.sendMessageAll("vote.kick.done");
                    target.getInfo().lastKicked = Time.millis() + (30 * 60) * 1000;
                    Call.onKick(target.con, Packets.KickReason.vote);
                    Log.write(Log.LogType.player, "log.player.kick");
                    break;
                case rollback:
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
                    tool.sendMessageAll("vote.map.done");

                    Gamemode current = Gamemode.survival;
                    if (state.rules.attackMode) {
                        current = Gamemode.attack;
                    } else if (state.rules.pvp) {
                        current = Gamemode.pvp;
                    } else if (state.rules.editor) {
                        current = Gamemode.editor;
                    }
                    world.loadMap(map, (map).applyRules(current));
                    Call.onWorldDataBegin();

                    for (Player p : playerGroup.all()) {
                        Vars.netServer.sendWorldData(p);
                        p.reset();

                        if (Vars.state.rules.pvp) p.setTeam(Vars.netServer.assignTeam(p, playerGroup.all()));
                    }
                    Log.info("Map rollbacked.");
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
                    tool.sendMessageAll("vote.kick.fail");
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
        success();
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
            success();
        }
    }

    public enum VoteType {
        gameover, skipwave, kick, rollback, gamemode, map
    }
}
