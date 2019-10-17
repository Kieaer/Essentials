package essentials.thread;

import essentials.Global;
import io.anuke.arc.Core;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.Tile;
import org.json.JSONArray;
import org.json.JSONTokener;

import static essentials.Global.printStackTrace;
import static io.anuke.mindustry.Vars.world;

public class Powerstat extends Block{
    private boolean active;

    public Powerstat(String name) {
        super(name);
        super.update = true;
    }

    @Override
    public void update(Tile tile) {
        super.update(tile);

        Global.log("DONE!");
        while(active){
            try {
                String db = Core.settings.getDataDirectory().child("plugins/Essentials/powerblock.json").readString();
                JSONTokener parser = new JSONTokener(db);
                JSONArray object = new JSONArray(parser);
                for (int i = 0; i < object.length(); i++) {
                    String raw = object.getString(i);

                    String[] data = raw.split("/");

                    int x = Integer.parseInt(data[0]);
                    int y = Integer.parseInt(data[1]);
                    int target_x = Integer.parseInt(data[2]);
                    int target_y = Integer.parseInt(data[3]);

                    float current;
                    float product;
                    float using;
                    try {
                        current = world.tileWorld(target_x * 8, target_y * 8).entity.power.graph.getPowerBalance() * 60;
                        using = world.tileWorld(target_x * 8, target_y * 8).entity.power.graph.getPowerNeeded() * 60;
                        // getPowerProduced() make random #iterator can't nested error.
                        product = world.tileWorld(target_x * 8, target_y * 8).entity.power.graph.getPowerProduced() * 60;
                    } catch (Exception ignored) {
                        current = 0;
                        using = 0;
                        product = 0;
                    }

                    if (current == 0 && using == 0 && product == 0) {
                        //Call.onTileDestroyed(world.tileWorld(x * 8, y * 8));
                        object.remove(i);
                        Core.settings.getDataDirectory().child("plugins/Essentials/powerblock.json").writeString(String.valueOf(object));
                    } else {
                        String text = "Power status\n" +
                                "Current: [sky]" + Math.round(current) + "[]\n" +
                                "Using: [red]" + Math.round(using) + "[]\n" +
                                "Production: [green]" + Math.round(product) + "[]";
                        Call.setMessageBlockText(null, world.tileWorld(x * 8, y * 8), text);
                    }
                }
            } catch (Exception e) {
                printStackTrace(e);
            }
        }
    }
}
