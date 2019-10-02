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
                Map<String, Object> obj = yaml.load(String.valueOf(Core.settings.getDataDirectory().child("plugins/Essentials/Exp.txt").readString()));
                int blockreqlevel;
                if(String.valueOf(obj.get(name)) != null) {
                    blockreqlevel = Integer.parseInt(String.valueOf(obj.get(name)));
                } else {
                    blockreqlevel = 100;
                    Global.loge(name+" block require level data isn't found!");
                }

                if(level < blockreqlevel){
                    Call.onDeconstructFinish(e.tile, e.tile.block(), e.player.id);
                    Global.log(name+" is cleared!");
                }
            }
        });
    }
}
