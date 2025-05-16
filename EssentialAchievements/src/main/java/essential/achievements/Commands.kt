package essential.achievements

import arc.util.Strings
import essential.command.ClientCommand
import essential.database.data.PlayerData
import java.util.ResourceBundle
import kotlin.math.abs

class Commands {
    @ClientCommand(name = "achievements", parameter = "[page]", description = "Show your achievements")
    fun achievements(playerData: PlayerData, arg: Array<String?>) {
        val temp: MutableList<String?> = java.util.ArrayList<String?>()
        val bundle = try {
            ResourceBundle.getBundle("bundle", java.util.Locale.of(playerData.player.locale().split("_")[0]))
        } catch (e: java.util.MissingResourceException) {
            ResourceBundle.getBundle("bundle", java.util.Locale.ENGLISH)
        }

        for (ach in Achievement.entries) {
            if (!ach.isHidden || (ach.isHidden && ach.success(playerData))) {
                val name: String = ach.toString().lowercase(java.util.Locale.getDefault())
                val cleared = if (ach.success(playerData)) "[sky][" + bundle.getString("cleared") + "][] " else ""
                temp.add(cleared + bundle.getString("achievement.$name") + "[orange] (" + ach.current(playerData) + " / " + ach.value() + ")[][]\n")
                temp.add("[yellow]" + bundle.getString("description.$name") + "\n")
                temp.add("\n")
            }
        }

        val result = java.lang.StringBuilder()
        val per = 9
        var page = if (arg.isEmpty()) 1 else abs(Strings.parseInt(arg[0]))
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
}
