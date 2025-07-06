package essential.achievements;

import mindustry.game.EventType;

/**
 * This class provides static access to the event handler methods defined in AchievementEvents.java.
 * It's used by the generated code to call the methods.
 */
public class Achievements {
    /**
     * Delegates to the blockBuildEnd method in AchievementEvents.
     */
    public static void blockBuildEnd(EventType.BlockBuildEndEvent event) {
        AchievementEvents.blockBuildEnd(event);
    }

    /**
     * Delegates to the gameover method in AchievementEvents.
     */
    public static void gameover(EventType.GameOverEvent event) {
        AchievementEvents.gameover(event);
    }

    /**
     * Delegates to the wave method in AchievementEvents.
     */
    public static void wave(EventType.WaveEvent event) {
        AchievementEvents.wave(event);
    }

    /**
     * Delegates to the achievementClear method in AchievementEvents.
     */
    public static void achievementClear(CustomEvents.AchievementClear event) {
        AchievementEvents.achievementClear(event);
    }

    /**
     * Delegates to the playerChat method in AchievementEvents.
     */
    public static void playerChat(EventType.PlayerChatEvent event) {
        AchievementEvents.playerChat(event);
    }

    /**
     * Delegates to the unitChange method in AchievementEvents.
     */
    public static void unitChange(EventType.UnitChangeEvent event) {
        AchievementEvents.unitChange(event);
    }

    /**
     * Delegates to the unitDestroy method in AchievementEvents.
     */
    public static void unitDestroy(EventType.UnitDestroyEvent event) {
        AchievementEvents.unitDestroy(event);
    }

    /**
     * Delegates to the updateSecond method in AchievementEvents.
     */
    public static void updateSecond() {
        AchievementEvents.updateSecond();
    }

    /**
     * Delegates to the playerJoin method in AchievementEvents.
     */
    public static void playerJoin(EventType.PlayerJoin event) {
        AchievementEvents.playerJoin(event);
    }

    /**
     * Delegates to the playerLeave method in AchievementEvents.
     */
    public static void playerLeave(EventType.PlayerLeave event) {
        AchievementEvents.playerLeave(event);
    }

    /**
     * Delegates to the withdraw method in AchievementEvents.
     */
    public static void withdraw(EventType.WithdrawEvent event) {
        AchievementEvents.withdraw(event);
    }

    /**
     * Delegates to the deposit method in AchievementEvents.
     */
    public static void deposit(EventType.DepositEvent event) {
        AchievementEvents.deposit(event);
    }

    /**
     * Delegates to the config method in AchievementEvents.
     */
    public static void config(EventType.ConfigEvent event) {
        AchievementEvents.config(event);
    }

    /**
     * Delegates to the tap method in AchievementEvents.
     */
    public static void tap(EventType.TapEvent event) {
        AchievementEvents.tap(event);
    }

    /**
     * Delegates to the blockBuildBegin method in AchievementEvents.
     */
    public static void blockBuildBegin(EventType.BlockBuildBeginEvent event) {
        AchievementEvents.blockBuildBegin(event);
    }

    /**
     * Delegates to the blockDestroy method in AchievementEvents.
     */
    public static void blockDestroy(EventType.BlockDestroyEvent event) {
        AchievementEvents.blockDestroy(event);
    }
}