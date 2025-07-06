package essential.protect;

import essential.event.CustomEvents;
import mindustry.game.EventType;

/**
 * This class provides static access to the event handler methods defined in ProtectEvent.java.
 * It's used by the generated code to call the methods.
 */
public class Protect {
    /**
     * Delegates to the configFileModified method in ProtectEvent.
     */
    public static void configFileModified(CustomEvents.ConfigFileModified event) {
        ProtectEvent.configFileModified(event);
    }

    /**
     * Delegates to the playerJoin method in ProtectEvent.
     */
    public static void playerJoin(EventType.PlayerJoin event) {
        ProtectEvent.playerJoin(event);
    }

    /**
     * Delegates to the playerChat method in ProtectEvent.
     */
    public static void playerChat(EventType.PlayerChatEvent event) {
        ProtectEvent.playerChat(event);
    }

    /**
     * Delegates to the blockBuildEnd method in ProtectEvent.
     */
    public static void blockBuildEnd(EventType.BlockBuildEndEvent event) {
        ProtectEvent.blockBuildEnd(event);
    }

    /**
     * Delegates to the gameOver method in ProtectEvent.
     */
    public static void gameOver(EventType.GameOverEvent event) {
        ProtectEvent.gameOver(event);
    }
}