package essential.achievements

import arc.util.Strings
import ksp.command.ClientCommand
import ksp.command.ServerCommand
import essential.database.data.PlayerData
import essential.permission.Permission
import essential.players
import java.util.Locale
import java.util.MissingResourceException
import java.util.ResourceBundle
import kotlin.math.abs

class Commands {
    @ClientCommand(name = "achievements", parameter = "[page]", description = "Show your achievements")
    fun achievements(playerData: PlayerData, args: Array<String>) {
        val temp: MutableList<String?> = ArrayList<String?>()
        val bundle = try {
            ResourceBundle.getBundle("bundle", Locale.of(playerData.player.locale().split("_")[0]))
        } catch (e: MissingResourceException) {
            ResourceBundle.getBundle("bundle", Locale.ENGLISH)
        }

        for (ach in Achievement.entries) {
            if (!ach.isHidden || (ach.isHidden && ach.success(playerData))) {
                val name: String = ach.toString().lowercase(Locale.getDefault())
                val cleared = if (ach.success(playerData)) "[sky][" + bundle.getString("cleared") + "][] " else ""
                temp.add(cleared + bundle.getString("achievement.$name") + "[orange] (" + ach.current(playerData) + " / " + ach.value() + ")[][]\n")
                temp.add("[yellow]" + bundle.getString("description.$name") + "\n")
                temp.add("\n")
            }
        }

        val result = java.lang.StringBuilder()
        val per = 9
        var page = if (args.isEmpty()) 1 else abs(Strings.parseInt(args[0]))
        val pages = kotlin.math.ceil((temp.size.toFloat() / per).toDouble()).toInt()
        page--

        if (page >= pages || page < 0) {
            playerData.err("command.page.range", pages)
            return
        }

        result.append("[orange]-- ").append(bundle.getString("command.page"))
            .append("[lightgray] ").append(page + 1)
            .append("[gray]/[lightgray]").append(pages)
            .append("[orange] --[white]\n")

        for (a in per * page..<kotlin.math.min(per * (page + 1), temp.size)) {
            result.append(temp[a])
        }

        val msg = result.substring(0, result.length - 1)
        playerData.player.sendMessage(msg)
    }

    @ServerCommand(name = "setmapprovider", parameter = "<player>", description = "Set the MapProvider achievement for a player")
    fun setMapProvider(args: Array<String>) {
        if (args.isEmpty()) {
            println("Please specify a player name")
            return
        }

        val playerName = args[0]
        val player = players.find { it.name.equals(playerName, ignoreCase = true) }

        if (player == null) {
            println("Player not found: $playerName")
            return
        }

        // Set the achievement
        player.status.put("record.map.provider", "1")
        if (Achievement.MapProvider.success(player)) {
            Achievement.MapProvider.set(player)
            println("MapProvider achievement set for player: ${player.name}")
        } else {
            println("Failed to set MapProvider achievement for player: ${player.name}")
        }
    }

    @ServerCommand(name = "setfeedbackprovider", parameter = "<player>", description = "Set the FeedbackProvider achievement for a player")
    fun setFeedbackProvider(args: Array<String>) {
        if (args.isEmpty()) {
            println("Please specify a player name")
            return
        }

        val playerName = args[0]
        val player = players.find { it.name.equals(playerName, ignoreCase = true) }

        if (player == null) {
            println("Player not found: $playerName")
            return
        }

        // Set the achievement
        player.status.put("record.feedback.provider", "1")
        if (Achievement.FeedbackProvider.success(player)) {
            Achievement.FeedbackProvider.set(player)
            println("FeedbackProvider achievement set for player: ${player.name}")
        } else {
            println("Failed to set FeedbackProvider achievement for player: ${player.name}")
        }
    }

    @ClientCommand(name = "setmapprovider", parameter = "<player>", description = "Set the MapProvider achievement for a player")
    fun clientSetMapProvider(playerData: PlayerData, args: Array<String>) {
        // Check if the player has admin permission
        if (!Permission.check(playerData, "admin")) {
            playerData.err("permission.denied")
            return
        }

        if (args.isEmpty()) {
            playerData.err("Please specify a player name")
            return
        }

        val playerName = args[0]
        val player = players.find { it.name.equals(playerName, ignoreCase = true) }

        if (player == null) {
            playerData.err("Player not found: $playerName")
            return
        }

        // Set the achievement
        player.status.put("record.map.provider", "1")
        if (Achievement.MapProvider.success(player)) {
            Achievement.MapProvider.set(player)
            playerData.send("MapProvider achievement set for player: ${player.name}")
        } else {
            playerData.err("Failed to set MapProvider achievement for player: ${player.name}")
        }
    }

    @ClientCommand(name = "setfeedbackprovider", parameter = "<player>", description = "Set the FeedbackProvider achievement for a player")
    fun clientSetFeedbackProvider(playerData: PlayerData, args: Array<String>) {
        // Check if the player has admin permission
        if (!Permission.check(playerData, "admin")) {
            playerData.err("permission.denied")
            return
        }

        if (args.isEmpty()) {
            playerData.err("Please specify a player name")
            return
        }

        val playerName = args[0]
        val player = players.find { it.name.equals(playerName, ignoreCase = true) }

        if (player == null) {
            playerData.err("Player not found: $playerName")
            return
        }

        // Set the achievement
        player.status.put("record.feedback.provider", "1")
        if (Achievement.FeedbackProvider.success(player)) {
            Achievement.FeedbackProvider.set(player)
            playerData.send("FeedbackProvider achievement set for player: ${player.name}")
        } else {
            playerData.err("Failed to set FeedbackProvider achievement for player: ${player.name}")
        }
    }
}
