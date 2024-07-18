package essential.achievements;

import essential.core.DB;

public class CustomEvents {
    public static class AchievementClear {
        public Achievement achievement;
        public DB.PlayerData playerData;

        public AchievementClear(Achievement achievement, DB.PlayerData playerData) {
            this.achievement = achievement;
            this.playerData = playerData;
        }
    }
}
