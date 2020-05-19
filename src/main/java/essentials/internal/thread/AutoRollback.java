package essentials.internal.thread;

import arc.files.Fi;
import arc.struct.Array;
import essentials.internal.CrashReport;
import essentials.internal.Log;
import mindustry.core.GameState;
import mindustry.entities.type.Player;
import mindustry.gen.Call;
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
        Array<Player> players = new Array<>();
        for (Player p : playerGroup.all()) {
            players.add(p);
            p.setDead(true);
        }

        logic.reset();

        Call.onWorldDataBegin();

        try {
            Fi file = saveDirectory.child(config.slotNumber() + "." + saveExtension);
            SaveIO.load(file);

            logic.play();

            for (Player p : players) {
                if (p.con == null) continue;

                p.reset();
                if (state.rules.pvp) {
                    p.setTeam(netServer.assignTeam(p, new Array.ArrayIterable<>(players)));
                }
                netServer.sendWorldData(p);
            }
        } catch (SaveIO.SaveException e) {
            new CrashReport(e);
        }
        Log.info("Map rollbacked.");
        if (state.is(GameState.State.playing)) Call.sendMessage("[green]Map rollbacked.");
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Essential Auto rollback thread");
        save();
    }
}
