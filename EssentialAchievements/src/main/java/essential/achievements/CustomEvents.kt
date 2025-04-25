package essential.achievements

import essential.core.DB

class CustomEvents {
    class AchievementClear(var achievement: Achievement?, playerData: PlayerData?) {
        var playerData: PlayerData?

        init {
            this.playerData = playerData
        }
    }
}
