package essentials.command.client

import arc.util.CommandHandler
import essentials.PluginVariables.playerList
import mindustry.Vars
import mindustry.game.Gamemode
import mindustry.gen.Player
import mindustry.maps.Map
import mindustry.net.WorldReloader

fun changeMap(handler: CommandHandler) {
    handler.register<Player>("changemap", "<name> [gamemode]", "Change map") { arg, player ->
        val playerData = playerList.find { it.uuid == player.uuid() }
        if (playerData != null) {
            // 맵을 이름 순서로 정렬하고 변수에 저장 해 놓습니다.
            val sortedMaps = mutableMapOf<Int, Map>()
            Vars.maps.all().sortedBy { it.name() }.forEachIndexed { i, it ->
                sortedMaps[i] = it
            }

            // 맵 번호 또는 이름으로 맵을 검색 합니다.
            val selectedMap: Map? = if (arg[0].toIntOrNull() != null) {
                sortedMaps[arg[0].toInt()]
            } else {
                sortedMaps.filter { it.value.name().contains(arg[0], true) }.values.firstOrNull()
            }

            if (selectedMap != null) {
                val gameMode = try {
                    Gamemode.valueOf(arg[1])
                } catch (e: Exception) {
                    Vars.state.rules.mode()
                }

                // 맵을 교체 합니다.
                val reload = WorldReloader()
                reload.begin()
                Vars.world.loadMap(selectedMap, selectedMap.applyRules(gameMode))
                Vars.state.rules = Vars.state.map.applyRules(gameMode)
                Vars.logic.play()
                reload.end()
            } else {
                playerData.err("command.changemap.map.not.found", arg[0])
            }
        } else {
            player.sendMessage("You don't have permission")
        }
    }
}