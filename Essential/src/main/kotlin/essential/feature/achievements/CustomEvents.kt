package essential.feature.achievements

import essential.common.database.data.PlayerData

class CustomEvents {
    class AchievementClear(var achievement: Achievement?, var playerData: PlayerData)
}
