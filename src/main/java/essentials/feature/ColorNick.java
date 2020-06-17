package essentials.feature;

import arc.struct.Seq;
import essentials.core.player.PlayerData;
import mindustry.gen.Playerc;

import static essentials.Main.config;
import static essentials.Main.playerDB;

public class ColorNick implements Runnable {
    private static int colorOffset = 0;
    public Seq<Playerc> targets = new Seq<>();

    @Override
    public void run() {
        Thread.currentThread().setName("Essential color nickname thread");
        while (!Thread.currentThread().isInterrupted()) {
            for (Playerc player : targets) {
                PlayerData p = playerDB.get(player.uuid());
                if (p.connected()) {
                    String name = p.name().replaceAll("\\[(.*?)]", "");
                    nickcolor(name, player);
                }
            }
            try {
                Thread.sleep(config.cupdatei());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void nickcolor(String name, Playerc player) {
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
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            int colorIndex = (i + colorOffset) % colors.length;
            if (colorIndex < 0) {
                colorIndex += colors.length;
            }
            String newtext = colors[colorIndex] + c;
            newnick[i] = newtext;
        }
        colorOffset--;
        for (String s : newnick) {
            stringBuilder.append(s);
        }
        player.name(stringBuilder.toString());
    }
}
