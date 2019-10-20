package essentials.thread;

import io.anuke.arc.Core;
import io.anuke.mindustry.gen.Call;
import org.json.JSONArray;
import org.json.JSONTokener;

import static essentials.Global.printStackTrace;
import static io.anuke.mindustry.Vars.world;

public class Powerstat{
    public void main() {
        try {
            String db = Core.settings.getDataDirectory().child("mods/Essentials/powerblock.json").readString();
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
                    current = world.tile(target_x, target_y).entity.power.graph.getPowerBalance() * 60;
                    using = world.tile(target_x, target_y).entity.power.graph.getPowerNeeded() * 60;
                    // getPowerProduced() make random #iterator can't nested error.
                    product = world.tile(target_x, target_y).entity.power.graph.getPowerProduced() * 60;
                } catch (Exception e) {
                    printStackTrace(e);
                    current = 0;
                    using = 0;
                    product = 0;
                }

                if (current == 0 && using == 0 && product == 0) {
                    Call.onTileDestroyed(world.tile(x, y));
                    object.remove(i);
                    Core.settings.getDataDirectory().child("mods/Essentials/powerblock.json").writeString(String.valueOf(object));
                } else {
                    String text = "Power status\n" +
                            "Current: [sky]" + Math.round(current) + "[]\n" +
                            "Using: [red]" + Math.round(using) + "[]\n" +
                            "Production: [green]" + Math.round(product) + "[]";
                    Call.setMessageBlockText(null, world.tile(x, y), text);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            printStackTrace(e);
        }
    }
}
