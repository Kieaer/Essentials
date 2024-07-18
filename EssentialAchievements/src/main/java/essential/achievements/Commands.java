package essential.achievements;

import arc.util.Strings;
import essential.core.DB;
import essential.core.annotation.ClientCommand;
import mindustry.Vars;
import mindustry.gen.Call;
import mindustry.gen.Playerc;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Commands {
    @ClientCommand(name = "achievements", parameter = "[page]", description = "Show your achievements")
    void achievements(Playerc player, DB.PlayerData playerData, String[] arg) {
        List<String> temp = new ArrayList<>();
        ResourceBundle bundle;
        if (playerData.getStatus().containsKey("language")) {
            bundle = ResourceBundle.getBundle(playerData.getStatus().get("language"));
        } else {
            bundle = ResourceBundle.getBundle(player.locale());
        }

        for (Achievement ach : Achievement.values()) {
            String name = ach.toString().toLowerCase();
            temp.add("[green]" + bundle.getString("achievement." + name) + "[] (" + ach.get(playerData)[0] + " / " + ach.get(playerData)[1] + ")\n");
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
                .append("[orange] --\n");

        for (int a = per * page; a < Math.min(per * (page + 1), temp.size()); a++) {
            result.append(temp.get(a));
        }

        String msg = result.substring(0, result.length() - 1);
        playerData.setLastSentMessage(msg);
        player.sendMessage(msg);
    }
}
