package essential.achievements;

import essential.database.data.PlayerData;

public class CustomEvents {
    public static class AchievementClear {
        private final Achievement achievement;
        private final PlayerData playerData;

        public AchievementClear(Achievement achievement, PlayerData playerData) {
            this.achievement = achievement;
            this.playerData = playerData;
        }

        public Achievement getAchievement() {
            return achievement;
        }

        public PlayerData getPlayerData() {
            return playerData;
        }
    }
}