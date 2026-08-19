package essential.core.service.achievements

import arc.Events
import arc.util.Timer
import essential.common.database.data.PlayerData
import essential.common.players
import essential.common.util.findPlayerData
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.game.EventType
import mindustry.net.Administration.ActionFilter
import mindustry.net.Administration.ActionType
import mindustry.net.Administration.PlayerAction
import mindustry.world.Block
import java.util.concurrent.ConcurrentHashMap

/**
 * APMTracker handles tracking and calculation of Actions Per Minute (APM) for players.
 * Actions are calculated realistically:
 * - Block placement, destruction, and cancellation: tracked per 1 buildPlan queue addition and removal
 * - Unit command, unit control, building configure, rotate, items/payload, ping: tracked per action
 * - Unit / tile clicks (TapEvent) and chat: tracked per action
 */
class APMTracker {
    data class PlanKey(
        val x: Int,
        val y: Int,
        val breaking: Boolean,
        val block: Block?,
        val rotation: Int
    )

    companion object {
        // Window size for APM calculation in milliseconds (5 minutes)
        private const val APM_WINDOW_SIZE = 5 * 60 * 1000

        // Maximum number of action timestamps to store per player
        private const val MAX_ACTION_TIMESTAMPS = 1000

        // Debounce intervals
        private const val DEBOUNCE_TAP_MS = 100L

        // Map of uuid -> map of threshold -> startTimestamp
        private val thresholdStartTimes = ConcurrentHashMap<String, MutableMap<Int, Long>>()
        private val lastTapTimes = ConcurrentHashMap<String, Long>()
        val playerPlans = ConcurrentHashMap<String, HashSet<PlanKey>>()

        private var initialized = false

        init {
            init()
        }

        fun init() {
            if (initialized) return
            initialized = true

            // Schedule periodic APM check
            Timer.schedule({
                for (data in players) {
                    updatePlayerAPM(data)
                }
            }, 0f, 1f)

            // Intercept player actions via ActionFilter
            Vars.netServer.admins.addActionFilter(object : ActionFilter {
                override fun allow(action: PlayerAction): Boolean {
                    val player = action.player ?: return true
                    val data = findPlayerData(player.uuid()) ?: return true

                    when (action.type) {
                        ActionType.commandUnits, ActionType.commandBuilding, ActionType.control,
                        ActionType.configure, ActionType.rotate, ActionType.withdrawItem,
                        ActionType.depositItem, ActionType.pickupBlock, ActionType.dropPayload,
                        ActionType.pingLocation -> {
                            trackAction(data)
                        }
                        else -> {}
                    }
                    return true
                }
            })

            // Track buildPlan queue addition and removal per tick
            Events.run(EventType.Trigger.update) {
                if (!Vars.state.isPlaying || Vars.world == null) return@run

                for (data in players) {
                    val player = data.player
                    val unit = player.unit()

                    if (unit == null || player.dead()) {
                        playerPlans.remove(data.uuid)
                        continue
                    }

                    val currentPlans = unit.plans() ?: continue
                    val currentSet = HashSet<PlanKey>(currentPlans.size)
                    for (plan in currentPlans) {
                        currentSet.add(PlanKey(plan.x, plan.y, plan.breaking, plan.block, plan.rotation))
                    }

                    val prev = playerPlans[data.uuid]
                    if (prev != null) {
                        // Check additions (each 1 buildPlan added to queue)
                        for (plan in currentSet) {
                            if (!prev.contains(plan)) {
                                trackAction(data)
                            }
                        }

                        // Check removals (each 1 buildPlan removed/canceled from queue before completion)
                        for (plan in prev) {
                            if (!currentSet.contains(plan)) {
                                val tile = Vars.world.tile(plan.x, plan.y)
                                val completed = if (tile == null) {
                                    false
                                } else if (plan.breaking) {
                                    tile.block() == Blocks.air
                                } else {
                                    tile.block() == plan.block
                                }

                                if (!completed) {
                                    // Removed/canceled by player before completion
                                    trackAction(data)
                                }
                            }
                        }
                    }

                    playerPlans[data.uuid] = currentSet
                }
            }

            // Player taps (clicking units, ground, etc.)
            Events.on(EventType.TapEvent::class.java) { event ->
                val player = event.player ?: return@on
                val data = findPlayerData(player.uuid()) ?: return@on
                val now = System.currentTimeMillis()
                val lastTap = lastTapTimes[player.uuid()] ?: 0L

                if (now - lastTap >= DEBOUNCE_TAP_MS) {
                    lastTapTimes[player.uuid()] = now
                    trackAction(data)
                }
            }

            // Player chat
            Events.on(EventType.PlayerChatEvent::class.java) { event ->
                val player = event.player ?: return@on
                val data = findPlayerData(player.uuid()) ?: return@on
                trackAction(data)
            }

            // Player join / leave lifecycle
            Events.on(EventType.PlayerJoin::class.java) { event ->
                val player = event.player ?: return@on
                val data = findPlayerData(player.uuid())
                if (data != null) {
                    initPlayer(data)
                }
            }

            Events.on(EventType.PlayerLeave::class.java) { event ->
                val player = event.player ?: return@on
                resetThresholds(player.uuid())
                lastTapTimes.remove(player.uuid())
                playerPlans.remove(player.uuid())
            }
        }

        // Track player actions
        fun trackAction(data: PlayerData) {
            data.apmTimestamps.add(System.currentTimeMillis())

            if (data.apmTimestamps.size > MAX_ACTION_TIMESTAMPS) {
                data.apmTimestamps.removeAt(0)
            }

            updatePlayerAPM(data)
        }

        // Calculate APM for a player based on action timestamps
        fun updatePlayerAPM(data: PlayerData) {
            val currentTime = System.currentTimeMillis()

            if (data.apmTimestamps.isEmpty()) {
                data.apm = 0
                resetThresholds(data.uuid)
                return
            }

            val recentTimestamps = data.apmTimestamps.filter { currentTime - it <= APM_WINDOW_SIZE }
            val windowSizeMinutes = APM_WINDOW_SIZE / (60.0 * 1000.0)
            val apm = if (recentTimestamps.isNotEmpty()) (recentTimestamps.size / windowSizeMinutes).toInt() else 0

            data.apm = apm

            val levels = listOf(
                Pair(50, Achievement.APM50),
                Pair(100, Achievement.APM100),
                Pair(200, Achievement.APM200)
            )

            val playerStarts = thresholdStartTimes.computeIfAbsent(data.uuid) { mutableMapOf() }

            for ((threshold, achievement) in levels) {
                if (data.apm >= threshold) {
                    val startTime = playerStarts[threshold]
                    if (startTime == null) {
                        playerStarts[threshold] = currentTime
                    } else if (currentTime - startTime >= 60 * 1000) {
                        if (achievement.success(data)) {
                            achievement.set(data)
                        }
                    }
                } else {
                    playerStarts.remove(threshold)
                }
            }
        }

        fun resetThresholds(uuid: String) {
            thresholdStartTimes.remove(uuid)
        }

        fun initPlayer(data: PlayerData) {
            data.apmTimestamps.clear()
            data.apm = 0
            resetThresholds(data.uuid)
            lastTapTimes.remove(data.uuid)
            playerPlans.remove(data.uuid)
        }

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
}
