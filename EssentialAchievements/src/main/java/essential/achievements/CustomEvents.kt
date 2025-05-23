package essential.achievements

import essential.database.data.PlayerDataEntity

class CustomEvents {
    class AchievementClear(var achievement: Achievement?, var playerData: PlayerDataEntity)
}
