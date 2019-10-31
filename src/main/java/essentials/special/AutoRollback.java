package essentials.special;

import essentials.Global;
import io.anuke.arc.collection.Array;
import io.anuke.arc.files.FileHandle;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.io.SaveIO;

import java.util.TimerTask;

import static essentials.EssentialConfig.slotnumber;
import static essentials.Global.printStackTrace;
import static io.anuke.mindustry.Vars.saveDirectory;
import static io.anuke.mindustry.Vars.saveExtension;

public class AutoRollback extends TimerTask {
    private boolean save() {
        try{
            FileHandle file = saveDirectory.child(slotnumber + "." + saveExtension);
            SaveIO.save(file);
            return true;
        }catch (Exception e){
            printStackTrace(e);
            return false;
        }
    }

    void load(){
        Array<Player> all = Vars.playerGroup.all();
        Array<Player> players = new Array<>();
        players.addAll(all);

        try {
            FileHandle file = saveDirectory.child(slotnumber + "." + saveExtension);
            SaveIO.load(file);
        } catch (SaveIO.SaveException e) {
            printStackTrace(e);
        }

        Call.onWorldDataBegin();

        for (Player p : players) {
            Vars.netServer.sendWorldData(p);
            p.reset();

            if (Vars.state.rules.pvp) {
                p.setTeam(Vars.netServer.assignTeam(p, new Array.ArrayIterable<>(players)));
            }
        }
        Global.log("Map rollbacked.");
        Call.sendMessage("[green]Map rollbacked.");
    }

    @Override
    public void run() {
        if(save()){
            Global.log("Map save completed.");
            Call.sendMessage("[scarlet]AutoSave complete");
        } else {
            Global.loge("Map save failed! Check your disk or config!");
        }
    }
}
