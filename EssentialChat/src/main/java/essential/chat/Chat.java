package essential.chat;

import essential.event.CustomEvents;
import mindustry.game.EventType;

/**
 * This class provides static access to the event handler methods defined in ChatEvents.java.
 * It's used by the generated code to call the methods.
 */
public class Chat {
    /**
     * Delegates to the serverLoaded method in ChatEvents.
     */
    public static void serverLoaded(EventType.ServerLoadEvent event) {
        ChatEvents.serverLoaded(event);
    }

    /**
     * Delegates to the configFileModified method in ChatEvents.
     */
    public static void configFileModified(CustomEvents.ConfigFileModified event) {
        ChatEvents.configFileModified(event);
    }
}