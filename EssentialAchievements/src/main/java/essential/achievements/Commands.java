package essential.achievements;

import arc.util.Strings;
import essential.core.DB;
import essential.core.annotation.ClientCommand;
import mindustry.gen.Playerc;

import java.util.*;

public class Commands {
    @ClientCommand(name = "achievements", parameter = "[page]", description = "Show your achievements")
    void achievements(Playerc player, DB.PlayerData playerData, String[] arg) {
        List<String> temp = new ArrayList<>();
        ResourceBundle bundle;
        if (playerData.getStatus().containsKey("language")) {
            bundle = ResourceBundle.getBundle("bundle", new Locale(playerData.getStatus().get("language").split("_")[0]));
        } else {
            try {
                bundle = ResourceBundle.getBundle("bundle", new Locale(player.locale().split("_")[0]));
            } catch (MissingResourceException e) {
                bundle = ResourceBundle.getBundle("bundle", Locale.ENGLISH);
            }
        }

        for (Achievement ach : Achievement.values()) {
            String name = ach.toString().toLowerCase();
            String cleared = ach.success(playerData) ? bundle.getString("cleared") : "";
            temp.add(cleared + " " + bundle.getString("achievement." + name) + "[orange] (" + ach.current(playerData) + " / " + ach.value() + ")[][]\n");
            temp.add("[white]" + bundle.getString("target." + name) + "\n");
            temp.add("[yellow]" + bundle.getString("description." + name) + "\n");
            temp.add("\n");
        }

        StringBuilder result = new StringBuilder();
        int per = 9;
        int page = (arg.length == 0) ? 1 : Math.abs(Strings.parseInt(arg[0]));
        int pages = (int) Math.ceil((float) temp.size() / per);
        page--;

        if (page >= pages || page < 0) {
            playerData.err("command.page.range", pages);
            return;
        }

        result.append("[orange]-- ").append(bundle.getString("command.page"))
                .append("[lightgray] ").append(page + 1)
                .append("[gray]/[lightgray]").append(pages)
                .append("[orange] --[white]\n");

        for (int a = per * page; a < Math.min(per * (page + 1), temp.size()); a++) {
            result.append(temp.get(a));
        }

        String msg = result.substring(0, result.length() - 1);
        playerData.setLastSentMessage(msg);
        player.sendMessage(msg);
    }
}
