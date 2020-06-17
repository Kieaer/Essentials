package essentials.internal.thread;

import arc.files.Fi;
import arc.struct.Seq;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import mindustry.core.GameState;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Playerc;
import mindustry.io.SaveIO;

import java.util.TimerTask;

import static essentials.Main.config;
import static mindustry.Vars.*;

public class AutoRollback extends TimerTask {
    public void save() {
        try {
            Fi file = saveDirectory.child(config.slotNumber() + "." + saveExtension);
            if (state.is(GameState.State.playing)) SaveIO.save(file);
        } catch (Exception e) {
            new CrashReport(e);
        }
    }

    public void load() {
        Seq<Playerc> players = new Seq<>();
        for (Playerc p : Groups.player) {
            players.add(p);
            p.dead();
        }

        logic.reset();

        Call.onWorldDataBegin();

        try {
            Fi file = saveDirectory.child(config.slotNumber() + "." + saveExtension);
            SaveIO.load(file);

            logic.play();

            for (Playerc p : players) {
                if (p.con() == null) continue;

                p.reset();
                if (state.rules.pvp) {
                    p.team(netServer.assignTeam(p, new Seq.SeqIterable<>(players)));
                }
                netServer.sendWorldData(p);
            }
        } catch (SaveIO.SaveException e) {
            new CrashReport(e);
        }
        Log.info("Map rollbacked.");
        /*new Thread(() -> {
            try {
                float orignal = state.rules.respawnTime;
                state.rules.respawnTime = 0f;
                Call.onSetRules(state.rules);
                sleep(3000);
                state.rules.respawnTime = orignal;
                Call.onSetRules(state.rules);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

        }).start();*/
        if (state.is(GameState.State.playing)) Call.sendMessage("[green]Map rollbacked.");
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Essential Auto rollback thread");
        save();
    }
}
