package essential.achievements

import arc.util.Strings
import essential.database.data.PlayerData

class Commands {
    @ClientCommand(name = "achievements", parameter = "[page]", description = "Show your achievements")
    fun achievements(playerData: PlayerData, arg: Array<String?>) {
        val temp: MutableList<String?> = java.util.ArrayList<String?>()
        var bundle: java.util.ResourceBundle
        if (playerData.getStatus().containsKey("language")) {
            bundle = java.util.ResourceBundle.getBundle(
                "bundle",
                java.util.Locale(playerData.getStatus().get("language").split("_")[0])
            )
        } else {
            try {
                bundle = java.util.ResourceBundle.getBundle("bundle", java.util.Locale(player.locale().split("_")[0]))
            } catch (e: java.util.MissingResourceException) {
                bundle = java.util.ResourceBundle.getBundle("bundle", java.util.Locale.ENGLISH)
            }
        }

        for (ach in Achievement.entries) {
            if (!ach.isHidden() || (ach.isHidden() && ach.success(playerData))) {
                val name: kotlin.String? = ach.toString().lowercase(java.util.Locale.getDefault())
                val cleared = if (ach.success(playerData)) "[sky][" + bundle.getString("cleared") + "][] " else ""
                temp.add(cleared + bundle.getString("achievement." + name) + "[orange] (" + ach.current(playerData) + " / " + ach.value() + ")[][]\n")
                temp.add("[yellow]" + bundle.getString("description." + name) + "\n")
                temp.add("\n")
            }
        }

        val result = java.lang.StringBuilder()
        val per = 9
        var page = if (arg.size == 0) 1 else java.lang.Math.abs(Strings.parseInt(arg[0]))
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
            result.append(temp.get(a))
        }

        val msg = result.substring(0, result.length - 1)
        playerData.setLastSentMessage(msg)
        player.sendMessage(msg)
    }
}
