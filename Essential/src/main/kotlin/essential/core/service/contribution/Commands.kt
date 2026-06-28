package essential.core.service.contribution

import essential.common.database.data.PlayerData
import essential.common.database.data.getAverageContribution
import essential.common.database.data.getContributionCount
import essential.common.database.data.getPlayerDataByName
import essential.core.Main.Companion.scope
import kotlinx.coroutines.launch
import ksp.command.ClientCommand

class Commands {
    @ClientCommand(name = "contribution", parameter = "[player...]", description = "Show average contribution score")
    fun contribution(playerData: PlayerData, args: Array<String>) {
        scope.launch {
            val target: PlayerData = if (args.isNotEmpty()) {
                getPlayerDataByName(args[0]) ?: run {
                    playerData.err("command.contribution.player.notfound", args[0])
                    return@launch
                }
            } else {
                playerData
            }

            val average = getAverageContribution(target)
            val count = getContributionCount(target)
            playerData.send(
                "command.contribution.result",
                target.name,
                String.format("%.1f", target.currentContribution),
                String.format("%.1f", average),
                count
            )
        }
    }
}
