package essentials;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.mindustry.game.EventType;
import io.anuke.mindustry.gen.Call;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;

import static essentials.EssentialPlayer.getData;

public class EssentialEPG {
    public static void main(){
        Events.on(EventType.BlockBuildEndEvent.class, e -> {
            if(!e.breaking){
                JSONObject db = getData(e.player.uuid);
                String name = e.tile.block().name;
                int level = (int) db.get("level");
                Yaml yaml = new Yaml();
                Map<String, Object> obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/BlockReqExp.txt").readString()));
                int blockreqlevel = 100;
                if(String.valueOf(obj.get(name)) != null) {
                    blockreqlevel = Integer.parseInt(String.valueOf(obj.get(name)));
                } else if(e.tile.block().name.equals("air")){
                    Global.loge(name+" block require level data isn't found!");
                } else {
                    return;
                }

                if(level < blockreqlevel){
                    Call.onDeconstructFinish(e.tile, e.tile.block(), e.player.id);
                    e.player.sendMessage(name+" block requires "+blockreqlevel+" level.");
                }
            }
        });
    }
}
