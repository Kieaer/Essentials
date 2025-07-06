package essential.achievements;

import arc.util.Strings;
import essential.PluginDataKt;
import essential.database.data.PlayerData;
import essential.permission.Permission;
import ksp.command.ClientCommand;
import ksp.command.ServerCommand;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class Commands {
    @ClientCommand(name = "achievements", parameter = "[page]", description = "Show your achievements")
    public void achievements(PlayerData playerData, String[] args) {
        java.util.List<String> temp = new java.util.ArrayList<>();
        ResourceBundle bundle;
        try {
            bundle = ResourceBundle.getBundle("bundle", new Locale(playerData.getPlayer().locale().split("_")[0]));
        } catch (MissingResourceException e) {
            bundle = ResourceBundle.getBundle("bundle", Locale.ENGLISH);
        }

        for (Achievement ach : Achievement.values()) {
            if (!ach.isHidden() || (ach.isHidden() && ach.success(playerData))) {
                String name = ach.toString().toLowerCase(Locale.getDefault());
                String cleared = ach.success(playerData) ? "[sky][" + bundle.getString("cleared") + "][] " : "";
                temp.add(cleared + bundle.getString("achievement." + name) + "[orange] (" + ach.current(playerData) + " / " + ach.value() + ")[][]\n");
                temp.add("[yellow]" + bundle.getString("description." + name) + "\n");
                temp.add("\n");
            }
        }

        StringBuilder result = new StringBuilder();
        int per = 9;
        int page = args.length == 0 ? 1 : Math.abs(Strings.parseInt(args[0]));
        int pages = (int) Math.ceil((temp.size() / (float) per));
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
        playerData.getPlayer().sendMessage(msg);
    }

    @ServerCommand(name = "setmapprovider", parameter = "<player>", description = "Set the MapProvider achievement for a player")
    public void setMapProvider(String[] args) {
        if (args.length == 0) {
            System.out.println("Please specify a player name");
            return;
        }

        String playerName = args[0];
        PlayerData player = PluginDataKt.getPlayers().stream()
                .filter(p -> p.getName().equalsIgnoreCase(playerName))
                .findFirst()
                .orElse(null);

        if (player == null) {
            System.out.println("Player not found: " + playerName);
            return;
        }

        // Set the achievement
        player.getStatus().put("record.map.provider", "1");
        if (Achievement.MapProvider.success(player)) {
            Achievement.MapProvider.set(player);
            System.out.println("MapProvider achievement set for player: " + player.getName());
        } else {
            System.out.println("Failed to set MapProvider achievement for player: " + player.getName());
        }
    }

    @ServerCommand(name = "setfeedbackprovider", parameter = "<player>", description = "Set the FeedbackProvider achievement for a player")
    public void setFeedbackProvider(String[] args) {
        if (args.length == 0) {
            System.out.println("Please specify a player name");
            return;
        }

        String playerName = args[0];
        PlayerData player = PluginDataKt.getPlayers().stream()
                .filter(p -> p.getName().equalsIgnoreCase(playerName))
                .findFirst()
                .orElse(null);

        if (player == null) {
            System.out.println("Player not found: " + playerName);
            return;
        }

        // Set the achievement
        player.getStatus().put("record.feedback.provider", "1");
        if (Achievement.FeedbackProvider.success(player)) {
            Achievement.FeedbackProvider.set(player);
            System.out.println("FeedbackProvider achievement set for player: " + player.getName());
        } else {
            System.out.println("Failed to set FeedbackProvider achievement for player: " + player.getName());
        }
    }

    @ClientCommand(name = "setmapprovider", parameter = "<player>", description = "Set the MapProvider achievement for a player")
    public void clientSetMapProvider(PlayerData playerData, String[] args) {
        // Check if the player has admin permission
        if (!Permission.INSTANCE.check(playerData, "admin")) {
            playerData.err("permission.denied");
            return;
        }

        if (args.length == 0) {
            playerData.err("Please specify a player name");
            return;
        }

        String playerName = args[0];
        PlayerData player = PluginDataKt.getPlayers().stream()
                .filter(p -> p.getName().equalsIgnoreCase(playerName))
                .findFirst()
                .orElse(null);

        if (player == null) {
            playerData.err("Player not found: " + playerName);
            return;
        }

        // Set the achievement
        player.getStatus().put("record.map.provider", "1");
        if (Achievement.MapProvider.success(player)) {
            Achievement.MapProvider.set(player);
            playerData.send("MapProvider achievement set for player: " + player.getName());
        } else {
            playerData.err("Failed to set MapProvider achievement for player: " + player.getName());
        }
    }

    @ClientCommand(name = "setfeedbackprovider", parameter = "<player>", description = "Set the FeedbackProvider achievement for a player")
    public void clientSetFeedbackProvider(PlayerData playerData, String[] args) {
        // Check if the player has admin permission
        if (!Permission.INSTANCE.check(playerData, "admin")) {
            playerData.err("permission.denied");
            return;
        }

        if (args.length == 0) {
            playerData.err("Please specify a player name");
            return;
        }

        String playerName = args[0];
        PlayerData player = PluginDataKt.getPlayers().stream()
                .filter(p -> p.getName().equalsIgnoreCase(playerName))
                .findFirst()
                .orElse(null);

        if (player == null) {
            playerData.err("Player not found: " + playerName);
            return;
        }

        // Set the achievement
        player.getStatus().put("record.feedback.provider", "1");
        if (Achievement.FeedbackProvider.success(player)) {
            Achievement.FeedbackProvider.set(player);
            playerData.send("FeedbackProvider achievement set for player: " + player.getName());
        } else {
            playerData.err("Failed to set FeedbackProvider achievement for player: " + player.getName());
        }
    }
}