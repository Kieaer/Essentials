package essential.achievements

import essential.database.data.PlayerData

internal class CustomEvents {
    class AchievementClear(var achievement: Achievement?, var playerData: PlayerData)
}
