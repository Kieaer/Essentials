package essential.achievements

import essential.core.Main.Companion.scope
import essential.database.data.PlayerData
import ksp.event.Event
import essential.players
import essential.util.startInfiniteScheduler
import mindustry.game.EventType

/**
 * APMTracker handles tracking and calculation of Actions Per Minute (APM) for players.
 * APM is calculated using a sliding window approach similar to Starcraft.
 */
class APMTracker {
    companion object {
        // Window size for APM calculation in milliseconds (5 minutes)
        private const val APM_WINDOW_SIZE = 5 * 60 * 1000

        // Update interval for APM calculation in milliseconds (5 seconds)
        private const val APM_UPDATE_INTERVAL = 5 * 1000

        // Maximum number of action timestamps to store per player
        private const val MAX_ACTION_TIMESTAMPS = 1000

        init {
            scope.startInfiniteScheduler(APM_UPDATE_INTERVAL.toLong()) {
                for (data in players) {
                    updatePlayerAPM(data)
                }
            }
        }

        // Track player actions
        fun trackAction(data: PlayerData) {
            // Add current timestamp to the player's apmTimestamps list
            data.apmTimestamps.add(System.currentTimeMillis())

            // Keep only the most recent MAX_ACTION_TIMESTAMPS timestamps to prevent memory issues
            if (data.apmTimestamps.size > MAX_ACTION_TIMESTAMPS) {
                data.apmTimestamps.removeAt(0)
            }

            // Update APM immediately for responsive feedback
            updatePlayerAPM(data)
        }

        // Calculate APM for a player based on action timestamps
        fun updatePlayerAPM(data: PlayerData) {
            val currentTime = System.currentTimeMillis()

            if (data.apmTimestamps.isEmpty()) {
                data.apm = 0
                return
            }

            // Filter timestamps within the window
            val recentTimestamps = data.apmTimestamps.filter { currentTime - it <= APM_WINDOW_SIZE }

            // Calculate APM: (actions / window size in minutes)
            val windowSizeMinutes = APM_WINDOW_SIZE / (60.0 * 1000.0)
            val apm = if (recentTimestamps.isNotEmpty()) (recentTimestamps.size / windowSizeMinutes).toInt() else 0

            // Update player's APM
            data.apm = apm

            // Check APM achievements
            if (data.apm >= 200 && Achievement.APM200.success(data)) {
                Achievement.APM200.set(data)
            } else if (data.apm >= 100 && Achievement.APM100.success(data)) {
                Achievement.APM100.set(data)
            } else if (data.apm >= 50 && Achievement.APM50.success(data)) {
                Achievement.APM50.set(data)
            }
        }

        // Initialize APM tracking for a player
        fun initPlayer(data: PlayerData) {
            data.apmTimestamps.clear()
            data.apm = 0
        }

        // Find player data by UUID
        fun findPlayerByUuid(uuid: String?): PlayerData? {
            return players.find { it.uuid == uuid }
        }

        // Get detailed APM info for display
        fun getAPMInfo(data: PlayerData): String {
            if (data.apmTimestamps.isEmpty()) {
                return "APM: 0 (No actions recorded)"
            }

            val currentTime = System.currentTimeMillis()
            val recentTimestamps = data.apmTimestamps.filter { currentTime - it <= APM_WINDOW_SIZE }

            val windowSizeMinutes = APM_WINDOW_SIZE / (60.0 * 1000.0)
            val totalActions = recentTimestamps.size

            return "APM: ${data.apm} (${totalActions} actions in last ${windowSizeMinutes.toInt()} minutes)"
        }
    }

    @Event
    fun playerJoin(it: EventType.PlayerJoin) {
        val data = findPlayerByUuid(it.player.uuid())
        if (data != null) {
            initPlayer(data)
        }
    }
}
