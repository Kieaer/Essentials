package essential.discord;

import essential.event.CustomEvents;

/**
 * This class provides static access to the event handler methods defined in DiscordEvent.java.
 * It's used by the generated code to call the methods.
 */
public class Discord {
    /**
     * Delegates to the configFileModified method in DiscordEvent.
     */
    public static void configFileModified(CustomEvents.ConfigFileModified event) {
        DiscordEvent.configFileModified(event);
    }
}