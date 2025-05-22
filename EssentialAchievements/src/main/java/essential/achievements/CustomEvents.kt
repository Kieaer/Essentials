package essential.achievements

import essential.database.data.PlayerData

class CustomEvents {
    class AchievementClear(var achievement: Achievement?, var playerData: PlayerData)
}
