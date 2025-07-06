package essential.achievements;

import arc.util.Timer;
import essential.PluginDataKt;
import essential.database.data.PlayerData;

import java.util.List;

/**
 * APMTracker handles tracking and calculation of Actions Per Minute (APM) for players.
 * APM is calculated using a sliding window approach similar to Starcraft.
 */
public class APMTracker {
    // Window size for APM calculation in milliseconds (5 minutes)
    private static final int APM_WINDOW_SIZE = 5 * 60 * 1000;

    // Update interval for APM calculation in milliseconds (5 seconds)
    private static final int APM_UPDATE_INTERVAL = 5 * 1000;

    // Maximum number of action timestamps to store per player
    private static final int MAX_ACTION_TIMESTAMPS = 1000;

    static {
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                for (PlayerData data : PluginDataKt.getPlayers()) {
                    updatePlayerAPM(data);
                }
            }
        }, 0f, APM_UPDATE_INTERVAL / 1000f);
    }

    // Track player actions
    public static void trackAction(PlayerData data) {
        // Add current timestamp to the player's apmTimestamps list
        data.getApmTimestamps().add(System.currentTimeMillis());

        // Keep only the most recent MAX_ACTION_TIMESTAMPS timestamps to prevent memory issues
        if (data.getApmTimestamps().size() > MAX_ACTION_TIMESTAMPS) {
            data.getApmTimestamps().remove(0);
        }

        // Update APM immediately for responsive feedback
        updatePlayerAPM(data);
    }

    // Calculate APM for a player based on action timestamps
    public static void updatePlayerAPM(PlayerData data) {
        long currentTime = System.currentTimeMillis();

        if (data.getApmTimestamps().isEmpty()) {
            data.setApm(0);
            return;
        }

        // Filter timestamps within the window
        List<Long> recentTimestamps = data.getApmTimestamps().stream()
                .filter(timestamp -> currentTime - timestamp <= APM_WINDOW_SIZE)
                .toList();

        // Calculate APM: (actions / window size in minutes)
        double windowSizeMinutes = APM_WINDOW_SIZE / (60.0 * 1000.0);
        int apm = recentTimestamps.isEmpty() ? 0 : (int) (recentTimestamps.size() / windowSizeMinutes);

        // Update player's APM
        data.setApm(apm);

        // Check APM achievements
        if (data.getApm() >= 200 && Achievement.APM200.success(data)) {
            Achievement.APM200.set(data);
        } else if (data.getApm() >= 100 && Achievement.APM100.success(data)) {
            Achievement.APM100.set(data);
        } else if (data.getApm() >= 50 && Achievement.APM50.success(data)) {
            Achievement.APM50.set(data);
        }
    }

    // Initialize APM tracking for a player
    public static void initPlayer(PlayerData data) {
        data.getApmTimestamps().clear();
        data.setApm(0);
    }

    // Get detailed APM info for display
    public static String getAPMInfo(PlayerData data) {
        if (data.getApmTimestamps().isEmpty()) {
            return "APM: 0 (No actions recorded)";
        }

        long currentTime = System.currentTimeMillis();
        List<Long> recentTimestamps = data.getApmTimestamps().stream()
                .filter(timestamp -> currentTime - timestamp <= APM_WINDOW_SIZE)
                .toList();

        double windowSizeMinutes = APM_WINDOW_SIZE / (60.0 * 1000.0);
        int totalActions = recentTimestamps.size();

        return "APM: " + data.getApm() + " (" + totalActions + " actions in last " + (int) windowSizeMinutes + " minutes)";
    }
}
