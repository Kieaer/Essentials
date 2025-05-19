package essential.achievements

import arc.util.Strings
import essential.command.ClientCommand
import essential.database.data.PlayerData
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
}
