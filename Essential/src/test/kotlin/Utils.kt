package essential.test

import essential.database.data.PlayerDataEntity
import mindustry.gen.Call
import mindustry.gen.Playerc
import java.util.concurrent.ConcurrentHashMap

/**
 * Utility class for testing message sending functionality
 */
object MessageTracker {
    // Store messages sent to players
    private val playerMessages = ConcurrentHashMap<String, MutableList<String>>()

    // Store global messages sent via Call.sendMessage
    private val globalMessages = mutableListOf<String>()

    /**
     * Clear all tracked messages
     */
    fun clear() {
        playerMessages.clear()
        globalMessages.clear()
    }

    /**
     * Record a message sent to a specific player
     * @param player The player who received the message
     * @param message The message content
     */
    fun recordPlayerMessage(player: Playerc, message: String) {
        playerMessages.computeIfAbsent(player.uuid()) { mutableListOf() }.add(message)
    }

    /**
     * Record a global message sent via Call.sendMessage
     * @param message The message content
     */
    fun recordGlobalMessage(message: String) {
        synchronized(globalMessages) {
            globalMessages.add(message)
        }
    }

    /**
     * Get all messages sent to a specific player
     * @param player The player
     * @return List of messages sent to the player
     */
    fun getPlayerMessages(player: Playerc): List<String> {
        return playerMessages[player.uuid()] ?: emptyList()
    }

    /**
     * Get the last message sent to a specific player
     * @param player The player
     * @return The last message sent to the player, or null if no messages were sent
     */
    fun getLastPlayerMessage(player: Playerc): String? {
        return getPlayerMessages(player).lastOrNull()
    }

    /**
     * Get all global messages sent via Call.sendMessage
     * @return List of global messages
     */
    fun getGlobalMessages(): List<String> {
        return globalMessages.toList()
    }

    /**
     * Get the last global message sent via Call.sendMessage
     * @return The last global message, or null if no messages were sent
     */
    fun getLastGlobalMessage(): String? {
        return globalMessages.lastOrNull()
    }
}

/**
 * Extension property to access the last message sent to a player
 * This is used to simulate the behavior seen in tests where playerData.lastReceivedMessage is accessed
 */
var Playerc.lastReceivedMessage: String?
    get() = MessageTracker.getLastPlayerMessage(this)
    set(value) {
        if (value != null) {
            MessageTracker.recordPlayerMessage(this, value)
        }
    }

/**
 * Extension property to access the last message sent to a player via PlayerData
 * This is used to maintain compatibility with existing tests that use playerData.lastReceivedMessage
 */
var PlayerDataEntity.lastReceivedMessage: String?
    get() = player.lastReceivedMessage
    set(value) {
        player.lastReceivedMessage = value
    }

// Override Playerc.sendMessage to record messages
fun Playerc.sendMessage(message: String) {
    lastReceivedMessage = message
}

// Override Call.sendMessage to record messages
fun Call.sendMessage(message: String) {
    MessageTracker.recordGlobalMessage(message)
}

// Override Call.sendMessage with NetConnection to record messages
fun Call.sendMessage(con: mindustry.net.NetConnection, message: String) {
    // Find the player associated with this connection
    val player = mindustry.gen.Groups.player.find { it.con() == con }
    if (player != null) {
        player.lastReceivedMessage = message
    } else {
        // If no player is found, record as a global message
        MessageTracker.recordGlobalMessage(message)
    }
}

// Override Call.infoMessage to record messages
fun Call.infoMessage(con: mindustry.net.NetConnection, message: String) {
    // Find the player associated with this connection
    val player = mindustry.gen.Groups.player.find { it.con() == con }
    if (player != null) {
        player.lastReceivedMessage = message
    } else {
        // If no player is found, record as a global message
        MessageTracker.recordGlobalMessage(message)
    }
}

/**
 * Mock implementation for player.sendMessage that records the message
 * @param player The player to send the message to
 * @param message The message content
 */
fun mockPlayerSendMessage(player: Playerc, message: String) {
    MessageTracker.recordPlayerMessage(player, message)
}

/**
 * Mock implementation for Call.sendMessage that records the message
 * @param message The message content
 */
fun mockCallSendMessage(message: String) {
    MessageTracker.recordGlobalMessage(message)
}

/**
 * Extension property to access the last global message sent
 */
val lastGlobalMessage: String?
    get() = MessageTracker.getLastGlobalMessage()

/**
 * Example of how to use the message tracking utilities in tests
 * 
 * Usage example:
 * ```
 * // In your test setup
 * val player = createPlayer()
 * 
 * // Mock sending a message to a player
 * mockPlayerSendMessage(player, "Hello player!")
 * 
 * // Assert that the message was recorded
 * assertEquals("Hello player!", player.lastReceivedMessage)
 * 
 * // Mock sending a global message
 * mockCallSendMessage("Hello everyone!")
 * 
 * // Assert that the global message was recorded
 * assertEquals("Hello everyone!", lastGlobalMessage)
 * 
 * // Clear all tracked messages
 * MessageTracker.clear()
 * ```
 */
fun messageTrackingExample() {
    // This is just a documentation function and is not meant to be called
}
