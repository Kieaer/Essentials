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
                String name = db.getString("name");
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
        for (int i = 0; i<name.length(); i++) {
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
/*


public class MulticolorTextAnimation
{
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                createAndShowGUI();
            }
        });
    }

    private static void createAndShowGUI()
    {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        MulticolorTextAnimationPanel m = new MulticolorTextAnimationPanel();
        f.getContentPane().add(m);

        Thread thread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                while (true)
                {
                    m.thisTickMethodThatYouHaveBeenTalkingAbout();
                    try
                    {
                        Thread.sleep(1);
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
        thread.setDaemon(true);
        thread.start();

        f.setSize(500,200);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}


class MulticolorTextAnimationPanel extends JPanel
{
    private String string;
    private Color colors[];
    private int colorOffset = 0;
    private long lastUpdateNs = -1;
    private final long updateIntervalMs = 250;

    public MulticolorTextAnimationPanel()
    {
        setFont(new Font("Dialog", Font.BOLD, 45));

        string = "I am a string!";

        colors = new Color[12];
        colors[0] = new Color(255, 0, 0);
        colors[1] = new Color(255, 127, 0);
        colors[2] = new Color(255, 255, 0);
        colors[3] = new Color(127, 255, 0);
        colors[4] = new Color(0, 255, 0);
        colors[5] = new Color(0, 255, 127);
        colors[6] = new Color(0, 255, 255);
        colors[7] = new Color(0, 127, 255);
        colors[8] = new Color(0, 0, 255);
        colors[9] = new Color(127, 0, 255);
        colors[10] = new Color(255, 0, 255);
        colors[11] = new Color(255, 0, 127);
    }

    public void thisTickMethodThatYouHaveBeenTalkingAbout()
    {
        long ns = System.nanoTime();
        if (lastUpdateNs < 0)
        {
            lastUpdateNs = ns;
        }
        long passedNs = (ns - lastUpdateNs);
        long passedMs = passedNs / 1000000;
        if (passedMs > updateIntervalMs)
        {
            // Increase or decrease the color offset,
            // depending on whether the colors should
            // cycle forward or backward
            colorOffset--;
            repaint();
            lastUpdateNs = ns;
        }

    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());

        FontMetrics fontMetrics = g.getFontMetrics();

        int x = 100;
        int y = 100;
        for (int i=0; i<string.length(); i++)
        {
            char c = string.charAt(i);
            int colorIndex = (i+colorOffset)%colors.length;
            if (colorIndex < 0)
            {
                colorIndex += colors.length;
            }
            g.setColor(colors[colorIndex]);
            g.drawString(String.valueOf(c), x, y);
            x += fontMetrics.charWidth(c);
        }


    }
}
*/