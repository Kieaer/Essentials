package essential.bridge;

import essential.event.CustomEvents;

/**
 * This class provides static access to the event handler methods defined in BridgeEvent.java.
 * It's used by the generated code to call the methods.
 */
public class Bridge {
    /**
     * Delegates to the configFileModified method in BridgeEvent.
     */
    public static void configFileModified(CustomEvents.ConfigFileModified event) {
        BridgeEvent.configFileModified(event);
    }
}