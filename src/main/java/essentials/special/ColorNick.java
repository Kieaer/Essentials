package essentials.special;

import essentials.EssentialPlayer;
import io.anuke.mindustry.entities.type.Player;
import org.json.JSONObject;

import static essentials.EssentialConfig.cupdatei;

public class ColorNick {
    private static int colorOffset = 0;
    private static long updateIntervalMs = cupdatei;

    public static void main(Player player){
        Thread thread = new Thread(() -> {
            int connected = 1;
            while (connected == 1) {
                JSONObject db = EssentialPlayer.getData(player.uuid);
                connected = Integer.parseInt(db.getString("connected"));
                String name = db.getString("name").replaceAll("\\[(.*?)]", "");
                try {
                    Thread.sleep(updateIntervalMs);
                    nickcolor(name, player);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });
        thread.start();
    }

    private static void nickcolor(String name, Player player) {
        StringBuilder stringBuilder = new StringBuilder();

        String[] colors = new String[11];
        colors[0] = "[#ff0000]";
        colors[1] = "[#ff7f00]";
        colors[2] = "[#ffff00]";
        colors[3] = "[#7fff00]";
        colors[4] = "[#00ff00]";
        colors[5] = "[#00ff7f]";
        colors[6] = "[#00ffff]";
        colors[7] = "[#007fff]";
        colors[8] = "[#0000ff]";
        colors[9] = "[#8000ff]";
        colors[10] = "[#ff00ff]";

        String[] newnick = new String[name.length()];
        for (int i = 0; i<name.length(); i+=2) {
            char c = name.charAt(i);
            int colorIndex = (i+colorOffset)%colors.length;
            if (colorIndex < 0) {
                colorIndex += colors.length;
            }
            String newtext = colors[colorIndex]+c;
            newnick[i]=newtext;
        }
        colorOffset--;
        for (String s : newnick) {
            stringBuilder.append(s);
        }
        player.name = stringBuilder.toString();
    }
}