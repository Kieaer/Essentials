package essential.core.service.achievements

import arc.Events
import arc.util.Timer
import essential.common.bundle.Bundle
import essential.common.database.data.PlayerData
import essential.common.database.data.getPlayerAchievements
import essential.common.event.CustomEvents
import essential.common.offlinePlayers
import essential.common.players
import essential.common.pluginData
import essential.common.systemTimezone
import essential.common.util.findPlayerData
import kotlinx.datetime.daysUntil
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.toLocalDateTime
import ksp.event.Event
import mindustry.Vars.state
import mindustry.content.Planets
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.world.blocks.power.PowerGraph
import java.util.*
import kotlin.time.Clock
import kotlinx.coroutines.runBlocking

private var isNoMiningFailed = false
private var isNoPowerFailed = false
private var isLowPowerFailed = false
private var isNoTurretsFailed = false
private var isFlareOnlyFailed = false
private var isDuoTurretFailed = false

/** Executes achievement initialization after the core player-data load flow. */
object AchievementHooks {
    fun processPlayerDataLoad(playerData: PlayerData) {
        runBlocking {
            getPlayerAchievements(playerData).forEach { achievement ->
                playerData.achievementStatus.add(achievement.achievementName)
            }
        }

        for (achievement in Achievement.entries) {
            if (achievement.isHidden) continue
            try {
                if (achievement.success(playerData)) {
                    achievement.set(playerData)
                }
            } catch (e: Exception) {
                arc.util.Log.err("Failed to evaluate achievement ${achievement.name} for ${playerData.name}", e)
            }
        }
    }

    fun awardVotingBan(playerData: PlayerData) {
        Achievement.VotingBan.set(playerData)
    }
}

@Event
fun blockBuildEnd(event: BlockBuildEndEvent) {
    val unit = event.unit ?: return
    if (unit.isPlayer) {
        val player = unit.player
        val data: PlayerData? = if (player != null) findPlayerData(player.uuid()) else null
        if (data != null) {
            if (Achievement.Builder.success(data)) {
                Achievement.Builder.set(data)
            }
            if (Achievement.Deconstructor.success(data)) {
                Achievement.Deconstructor.set(data)
            }

            // Check for a water extractor built on water tiles
            if (!event.breaking && event.tile.block().name == "water-extractor" && event.tile.floor().isLiquid) {
                val count = data.status.getOrDefault("record.build.waterextractor", "0").toInt() + 1
                data.status["record.build.waterextractor"] = count.toString()
                if (Achievement.WaterExtractor.success(data)) {
                    Achievement.WaterExtractor.set(data)
                }
            }

            // Check for power nodes for LowPowerClear achievement
            if (!event.breaking && (event.tile.block().name == "power-node-large" || event.tile.block().name == "surge-tower")) {
                // Power node large / surge tower has capacity > 2k, mark the achievement as unachievable
                isLowPowerFailed = true
            }

            // Check for turrets for NoTurretsClear achievement
            if (!event.breaking && event.tile.block().name.contains("turret")) {
                isNoTurretsFailed = true
            }

            // Check for power generators for NoPowerClear achievement
            if (!event.breaking && (
                        event.tile.block().name.contains("generator") ||
                                event.tile.block().name.contains("solar-panel") ||
                                event.tile.block().name.contains("rtg") ||
                                event.tile.block().name.contains("reactor")
                        )
            ) {
                isNoPowerFailed = true
            }

            // Check for duo turrets for DuoTurretSurvival achievement
            if (!event.breaking && event.tile.block().name != "duo" && event.tile.block().name.contains("turret")) {
                isDuoTurretFailed = true
            }
        }
    }
}

@Event
fun gameover(event: GameOverEvent) {
    // Calculate PvP contribution points for each player
    if (state.rules.pvp) {
        val teamContributions = mutableMapOf<Team, Int>()
        val playerContributions = mutableMapOf<String, Int>()

        // Calculate the total contribution for each team and individual players
        players.forEach { data ->
            val contribution = data.currentUnitDestroyedCount * 10 +
                    data.currentBuildDestroyedCount * 5 +
                    data.currentBuildAttackCount * 3

            playerContributions[data.uuid] = contribution

            val team = data.player.team()
            teamContributions[team] = (teamContributions[team] ?: 0) + contribution
        }

        // Check for PvP contribution achievement
        players.forEach { data ->
            val playerContribution = playerContributions[data.uuid] ?: 0
            val teamContribution = teamContributions[data.player.team()] ?: 0
            val otherPlayersContribution = teamContribution - playerContribution

            // If a player's team lost, and player's contribution was more than double the rest of the team
            if (event.winner != data.player.team() &&
                data.player.team() != Team.derelict &&
                playerContribution > otherPlayersContribution * 2 &&
                otherPlayersContribution > 0
            ) {

                data.status["record.pvp.contribution"] = "1"
                if (Achievement.PvPContribution.success(data)) {
                    Achievement.PvPContribution.set(data)
                }
            }

            // Track PvP win streak
            if (event.winner === data.player.team()) {
                val streak = data.status.getOrDefault("record.pvp.win.streak.current", "0").toInt() + 1
                data.status["record.pvp.win.streak.current"] = streak.toString()

                if (streak >= 5) {
                    data.status["record.pvp.win.streak"] = "1"
                    if (Achievement.PvPWinStreak.success(data)) {
                        Achievement.PvPWinStreak.set(data)
                    }
                }

                // Track PvP wins on Serpulo
                if (state.rules.planet === Planets.serpulo) {
                    val winCount = data.status.getOrDefault("record.pvp.win.serpulo", "0").toInt() + 1
                    data.status["record.pvp.win.serpulo"] = winCount.toString()
                    if (Achievement.SerpuloPvPWin.success(data)) {
                        Achievement.SerpuloPvPWin.set(data)
                    }

                    // Update both planets win count
                    if (data.status.getOrDefault("record.pvp.win.erekir", "0").toInt() > 0) {
                        val bothCount = data.status.getOrDefault("record.pvp.win.both", "0").toInt() + 1
                        data.status["record.pvp.win.both"] = bothCount.toString()
                        if (Achievement.BothPlanetsPvPWin.success(data)) {
                            Achievement.BothPlanetsPvPWin.set(data)
                        }
                    }
                } else if (state.rules.planet === Planets.erekir) {
                    // Track PvP wins on Erekir
                    val winCount = data.status.getOrDefault("record.pvp.win.erekir", "0").toInt() + 1
                    data.status["record.pvp.win.erekir"] = winCount.toString()
                    if (Achievement.ErekirPvPWin.success(data)) {
                        Achievement.ErekirPvPWin.set(data)
                    }

                    // Update both planets win count
                    if (data.status.getOrDefault("record.pvp.win.serpulo", "0").toInt() > 0) {
                        val bothCount = data.status.getOrDefault("record.pvp.win.both", "0").toInt() + 1
                        data.status["record.pvp.win.both"] = bothCount.toString()
                        if (Achievement.BothPlanetsPvPWin.success(data)) {
                            Achievement.BothPlanetsPvPWin.set(data)
                        }
                    }
                }
            } else {
                // Reset win streak on loss
                data.status["record.pvp.win.streak.current"] = "0"

                // Track PvP defeat streak for other players
                val defeatStreak = data.status.getOrDefault("record.pvp.defeat.streak.current", "0").toInt() + 1
                data.status["record.pvp.defeat.streak.current"] = defeatStreak.toString()

                if (defeatStreak >= 5) {
                    data.status["record.pvp.defeat.streak"] = "1"
                    if (Achievement.PvPDefeatStreak.success(data)) {
                        Achievement.PvPDefeatStreak.set(data)
                    }
                }
            }
        }

        // Check for PvP underdog achievement
        players.forEach { data ->
            if (event.winner === data.player.team()) {
                // Count players on each team
                val teamCounts = mutableMapOf<Team, Int>()
                Groups.player.forEach { player ->
                    val team = player.team()
                    teamCounts[team] = (teamCounts[team] ?: 0) + 1
                }

                val winnerTeamCount = teamCounts[data.player.team()] ?: 0
                val largestEnemyTeamCount = teamCounts.filter { it.key != data.player.team() }
                    .maxByOrNull { it.value }?.value ?: 0

                // If the enemy team had 3 or more players than the winner team
                if (largestEnemyTeamCount >= winnerTeamCount + 3 && data.player.team() != Team.derelict) {
                    data.status["record.pvp.underdog"] = "1"
                    if (Achievement.PvPUnderdog.success(data)) {
                        Achievement.PvPUnderdog.set(data)
                    }
                }
            }
        }
    }

    players.forEach { data ->
        if (Achievement.Eliminator.success(data)) {
            Achievement.Eliminator.set(data)
        }
        if (Achievement.Lord.success(data)) {
            Achievement.Lord.set(data)
        }

        if (Achievement.Aggressor.success(data)) {
            Achievement.Aggressor.set(data)
        }
        val isWin = if (state.rules.attackMode) {
            event.winner === data.player.team() && state.rules.waveTeam.cores().isEmpty
        } else {
            event.winner === data.player.team() && !state.teams.playerCores().isEmpty
        }

        if (isWin) {
            if (Achievement.Asteroids.success(data)) {
                Achievement.Asteroids.set(data)
            }

            // Check if all maps have been cleared
            if (data.status.containsKey("record.map.clear.asteroids") &&
                data.status.containsKey("record.map.clear.transcendence")
            ) {
                data.status["record.map.clear.all"] = "1"
                if (Achievement.AllMaps.success(data)) {
                    Achievement.AllMaps.set(data)
                }
            }

            // Increment map clear count for MapClearMaster achievement
            val clearCount = data.status.getOrDefault("record.map.clear.count", "0").toInt() + 1
            data.status["record.map.clear.count"] = clearCount.toString()
            if (Achievement.MapClearMaster.success(data)) {
                Achievement.MapClearMaster.set(data)
            }

            // Check for SoloMapClear achievement
            if (Groups.player.size() == 1 && state.rules.attackMode) {
                data.status["record.map.clear.solo"] = "1"
                if (Achievement.SoloMapClear.success(data)) {
                    Achievement.SoloMapClear.set(data)
                }
            }

            // Check for NoMiningClear achievement
            if (!isNoMiningFailed && state.rules.attackMode) {
                data.status["record.map.clear.nomining"] = "1"
                if (Achievement.NoMiningClear.success(data)) {
                    Achievement.NoMiningClear.set(data)
                }
            }

            // Check for NoPowerClear achievement
            if (!isNoPowerFailed && state.rules.attackMode) {
                data.status["record.map.clear.nopower"] = "1"
                if (Achievement.NoPowerClear.success(data)) {
                    Achievement.NoPowerClear.set(data)
                }
            }

            // Check for NoTurretsClear achievement
            if (!isNoTurretsFailed && state.rules.attackMode) {
                data.status["record.map.clear.noturrets"] = "1"
                if (Achievement.NoTurretsClear.success(data)) {
                    Achievement.NoTurretsClear.set(data)
                }
            }

            // Check for LowPowerClear achievement
            if (!isLowPowerFailed && state.rules.attackMode) {
                data.status["record.map.clear.lowpower"] = "1"
                if (Achievement.LowPowerClear.success(data)) {
                    Achievement.LowPowerClear.set(data)
                }
            }

            // Check for FlareOnlyClear achievement
            if (!isFlareOnlyFailed && state.rules.attackMode) {
                data.status["record.map.clear.flareonly"] = "1"
                if (Achievement.FlareOnlyClear.success(data)) {
                    Achievement.FlareOnlyClear.set(data)
                }
            }
        } else {
            // Reset defeat streak on win
            data.status["record.pvp.defeat.streak.current"] = "0"
        }
    }
}

@Event
fun wave(event: WaveEvent) {
    players.forEach { data ->
        val value = data.status.getOrDefault("record.wave", "0").toInt() + 1
        data.status["record.wave"] = value.toString()
        if (Achievement.Defender.success(data)) {
            Achievement.Defender.set(data)
        }

        // DuoTurretSurvival progress
        if (!isDuoTurretFailed) {
            val duoWaves = data.status.getOrDefault("record.wave.duo", "0").toInt() + 1
            data.status["record.wave.duo"] = duoWaves.toString()
            if (Achievement.DuoTurretSurvival.success(data)) {
                Achievement.DuoTurretSurvival.set(data)
            }
        }
    }
}

@Event
fun achievementClear(event: CustomEvents.AchievementClear) {
    val bundle = Bundle(ResourceBundle.getBundle("bundles/achievements/bundle", Locale.forLanguageTag(event.playerData.player.locale().replace("_", "-"))))

    event.playerData.send(bundle, "event.achievement.success", bundle["achievement." + event.achievement.toString().lowercase()])
    players.forEach { data ->
        val b = Bundle(
            ResourceBundle.getBundle(
                "bundles/achievements/bundle",
                Locale.forLanguageTag(data.player.locale().replace("_", "-")),
                AchievementService::class.java.getClassLoader()
            )
        )

        data.send(
            b,
            "event.achievement.success.other",
            event.playerData.name,
            b["achievement." + event.achievement.toString().lowercase()]
        )
    }
}

@Event
fun playerChat(event: PlayerChatEvent) {
    if (!event.message.startsWith("/")) {
        val data: PlayerData? = findPlayerData(event.player.uuid())
        if (data != null) {
            val value = data.status.getOrDefault("record.time.chat", "0").toInt() + 1
            data.status["record.time.chat"] = value.toString()
            if (Achievement.Chatter.success(data)) {
                Achievement.Chatter.set(data)
            }

            // Check for the Korean New Year message
            if (event.message.contains("새해 복")) {
                data.status["record.chat.newyear"] = "1"
                if (Achievement.NewYear.success(data)) {
                    Achievement.NewYear.set(data)
                }
            }

            // If the chat sender has "owner" permission, award MeetOwner to everyone
            if (data.permission == "owner") {
                players.forEach { otherPlayer ->
                    otherPlayer.status["record.time.meetowner"] = "60"
                    if (Achievement.MeetOwner.success(otherPlayer)) {
                        Achievement.MeetOwner.set(otherPlayer)
                    }
                }
            }
        }
    } else if (event.message.startsWith("/apm")) {
        // Display the current APM for testing
        val data: PlayerData? = findPlayerData(event.player.uuid())
        if (data != null) {
            // Use the new APMTracker to get detailed APM info
            val apmInfo = APMTracker.getAPMInfo(data)
            data.send(apmInfo)
        }
    }
}

@Event
fun unitChange(event: UnitChangeEvent) {
    if (event.player != null && event.unit != null) {
        val data: PlayerData? = findPlayerData(event.player.uuid())
        if (data != null) {
            if (state.rules.planet === Planets.serpulo && event.unit.type.name.equals("quad", true)) {
                data.status["record.unit.serpulo.quad"] = "1"
                if (Achievement.SerpuloQuad.success(data)) {
                    Achievement.SerpuloQuad.set(data)
                }
            }

            // Reset unit-specific achievement tracking when changing units
            data.status["record.turret.quill.kill.time"] = "0"
            data.status["record.turret.zenith.kill.time"] = "0"

            // Check for FlareOnlyClear achievement - fail if the player controls non-flare unit
            if (!event.unit.type.name.equals(
                    "flare",
                    true
                ) && event.unit.type.name != "alpha" && event.unit.type.name != "beta" && event.unit.type.name != "gamma"
            ) {
                isFlareOnlyFailed = true
            }
        }
    }
}

@Event
fun unitDestroy(event: UnitDestroyEvent) {
    // For each player, check if they might have destroyed the unit
    for (player in Groups.player) {
        val data: PlayerData? = findPlayerData(player.uuid())

        if (data != null && event.unit != null && event.unit.team() != player.team()) {
            val playerUnit = player.unit() ?: continue

            // Check for CrawlerBlockDestroyer achievement
            if (playerUnit.type.name.equals("crawler", true)) {
                // Check if the destroyed unit is a wall, turret, or factory
                if (event.unit.type.name.contains("wall") || event.unit.type.name.contains("turret") || event.unit.type.name.contains(
                        "factory"
                    )
                ) {
                    val count = data.status.getOrDefault("record.crawler.block.destroy", "0").toInt() + 1
                    data.status["record.crawler.block.destroy"] = count.toString()
                    if (Achievement.CrawlerBlockDestroyer.success(data)) {
                        Achievement.CrawlerBlockDestroyer.set(data)
                    }
                }
            }

            // Check for TurretMultiKill achievement
            if (playerUnit.type.name.contains("turret")) {
                val multiKillCount =
                    data.status.getOrDefault("record.turret.multikill.current", "0").toInt() + 1
                data.status["record.turret.multikill.current"] = multiKillCount.toString()

                // If 5 or more units were destroyed simultaneously
                if (multiKillCount >= 5) {
                    data.status["record.turret.multikill"] = "1"
                    if (Achievement.TurretMultiKill.success(data)) {
                        Achievement.TurretMultiKill.set(data)
                    }
                    // Reset counter
                    data.status["record.turret.multikill.current"] = "0"
                }
            }

            // Check for QuillKiller achievement
            if (playerUnit.type.name.contains("turret") && event.unit.type.name.equals("quill", true)) {
                val currentTime = System.currentTimeMillis()
                val lastKillTime = data.status.getOrDefault("record.turret.quill.kill.time", "0").toLong()
                val killCount = if (currentTime - lastKillTime < 10000) {
                    data.status.getOrDefault("record.turret.quill.kill", "0").toInt() + 1
                } else {
                    1
                }

                data.status["record.turret.quill.kill"] = killCount.toString()
                data.status["record.turret.quill.kill.time"] = currentTime.toString()

                if (killCount >= 5 && Achievement.QuillKiller.success(data)) {
                    Achievement.QuillKiller.set(data)
                }
            }

            // Check for ZenithKiller achievement
            if (playerUnit.type.name.contains("turret") && event.unit.type.name.equals("zenith", true)) {
                val currentTime = System.currentTimeMillis()
                val lastKillTime = data.status.getOrDefault("record.turret.zenith.kill.time", "0").toLong()
                val killCount = if (currentTime - lastKillTime < 10000) {
                    data.status.getOrDefault("record.turret.zenith.kill", "0").toInt() + 1
                } else {
                    1
                }

                data.status["record.turret.zenith.kill"] = killCount.toString()
                data.status["record.turret.zenith.kill.time"] = currentTime.toString()

                if (killCount >= 30 && Achievement.ZenithKiller.success(data)) {
                    Achievement.ZenithKiller.set(data)
                }
            }

            // Check for OmuraHorizonKiller achievement
            if (playerUnit.type.name.equals("omura", true) && event.unit.type.name.equals("horizon", true)) {
                val multiKillCount =
                    data.status.getOrDefault("record.omura.horizon.kill.current", "0").toInt() + 1
                data.status["record.omura.horizon.kill.current"] = multiKillCount.toString()

                // If 5 or more horizon units were destroyed simultaneously
                if (multiKillCount >= 5) {
                    data.status["record.omura.horizon.kill"] = "1"
                    if (Achievement.OmuraHorizonKiller.success(data)) {
                        Achievement.OmuraHorizonKiller.set(data)
                    }
                    // Reset counter
                    data.status["record.omura.horizon.kill.current"] = "0"
                }
            }

            // Check for ExplosionKiller achievement - when a unit explodes and kills other units
            if (event.unit.type.name.equals("crawler", true)) {
                val explosionKillCount =
                    data.status.getOrDefault("record.explosion.kill.current", "0").toInt() + 1
                data.status["record.explosion.kill.current"] = explosionKillCount.toString()

                if (explosionKillCount >= 10) {
                    data.status["record.explosion.kill"] = "1"
                    if (Achievement.ExplosionKiller.success(data)) {
                        Achievement.ExplosionKiller.set(data)
                    }
                    // Reset counter
                    data.status["record.explosion.kill.current"] = "0"
                }
            }
        }
    }
}

@Event
fun updateSecond() {
    Timer.schedule({
        for (data in players) {
            // Track time played on different planets
            if (state.rules.planet === Planets.serpulo) {
                val value = data.status.getOrDefault("record.time.serpulo", "0").toInt() + 1
                data.status["record.time.serpulo"] = value.toString()
                if (Achievement.Serpulo.success(data)) {
                    Achievement.Serpulo.set(data)
                }
            } else if (state.rules.planet === Planets.erekir) {
                val value = data.status.getOrDefault("record.time.erekir", "0").toInt() + 1
                data.status["record.time.erekir"] = value.toString()
                if (Achievement.Erekir.success(data)) {
                    Achievement.Erekir.set(data)
                }
            } else if (state.rules.infiniteResources) {
                val value = data.status.getOrDefault("record.time.sandbox", "0").toInt() + 1
                data.status["record.time.sandbox"] = value.toString()
                if (Achievement.Creator.success(data)) {
                    Achievement.Creator.set(data)
                }
            }

            // Track time played on one map for LongPlayNoAfk achievement
            if (!data.afk) {
                val mapTime = data.status.getOrDefault("record.time.noafk", "0").toInt() + 1
                data.status["record.time.noafk"] = mapTime.toString()
                if (Achievement.LongPlayNoAfk.success(data)) {
                    Achievement.LongPlayNoAfk.set(data)
                }
            }

            // WarpServerDisconnect tracking - require 30 seconds of all warp servers being offline
            if (pluginData.data.warpBlock.isNotEmpty() && pluginData.data.warpBlock.all { !it.online }) {
                val warpOfflineTime = data.status.getOrDefault("record.warp.disconnect.duration", "0").toInt() + 1
                data.status["record.warp.disconnect.duration"] = warpOfflineTime.toString()
                if (warpOfflineTime >= 30) {
                    data.status["record.warp.disconnect"] = "1"
                    if (Achievement.WarpServerDisconnect.success(data)) {
                        Achievement.WarpServerDisconnect.set(data)
                    }
                }
            } else {
                data.status["record.warp.disconnect.duration"] = "0"
            }

            // APM calculation is now handled by APMTracker
        }

        // Check if any player unit is mining
        if (!isNoMiningFailed) {
            for (player in Groups.player) {
                if (player.team() == Team.sharded && player.unit()?.mining() == true) {
                    isNoMiningFailed = true
                    break
                }
            }
        }

        // Check team power metrics
        var totalPowerProduced = 0f
        var hasPower = false
        val checkedGraphs = mutableSetOf<PowerGraph>()
        for (build in Groups.build) {
            if (build.team() == Team.sharded && build.power != null) {
                val graph = build.power.graph
                if (graph != null) {
                    if (graph.lastPowerProduced > 0f || graph.lastPowerStored > 0f) {
                        hasPower = true
                    }
                    if (!checkedGraphs.contains(graph)) {
                        checkedGraphs.add(graph)
                        totalPowerProduced += graph.lastPowerProduced
                    }
                }
            }
        }

        if (hasPower) {
            isNoPowerFailed = true
        }
        if (totalPowerProduced > 2000f) {
            isLowPowerFailed = true
        }

        // Check for owner presence
        var isOwnerMeet = false
        for (data in players) {
            if (data.permission == "owner") {
                isOwnerMeet = true
                break
            }
        }

        if (isOwnerMeet) {
            for (data in players) {
                val value = data.status.getOrDefault("record.time.meetowner", "0").toInt() + 1
                data.status["record.time.meetowner"] = value.toString()
                if (Achievement.MeetOwner.success(data)) {
                    Achievement.MeetOwner.set(data)
                }
            }
        }
    }, 0f, 1f)
}

@Event
fun playerJoin(event: PlayerJoin) {
    val data: PlayerData? = findPlayerData(event.player.uuid())
    if (data != null) {
        // Check for attendance achievement
        if (Achievement.Attendance.success(data)) {
            Achievement.Attendance.set(data)
        }

        // Calculate absence duration for Loyal achievements
        val lastLogout = data.lastLogoutDate
        if (lastLogout != null) {
            val current = Clock.System.now().toLocalDateTime(systemTimezone)
            val daysAbsent = lastLogout.date.daysUntil(current.date)
            val monthsAbsent = lastLogout.date.monthsUntil(current.date)

            if (daysAbsent >= 5) {
                data.status["record.login.loyal"] = "1"
                if (Achievement.Loyal.success(data)) {
                    Achievement.Loyal.set(data)
                }
            }

            if (monthsAbsent >= 6) {
                data.status["record.login.loyal.sixmonths"] = "1"
                if (Achievement.LoyalSixMonths.success(data)) {
                    Achievement.LoyalSixMonths.set(data)
                }
            }

            if (monthsAbsent >= 18) {
                data.status["record.login.loyal.oneyearsixmonths"] = "1"
                if (Achievement.LoyalOneYearSixMonths.success(data)) {
                    Achievement.LoyalOneYearSixMonths.set(data)
                }
            }
        }

        // Reset map-specific achievement flags
        data.status.remove("record.map.clear.nomining.failed")
        data.status.remove("record.map.clear.nopower.failed")
        data.status.remove("record.map.clear.noturrets.failed")
        data.status.remove("record.map.clear.lowpower.failed")
        data.status.remove("record.map.clear.flareonly.failed")

        // Reset time tracking for LongPlayNoAfk achievement
        data.status["record.time.noafk"] = "0"
        data.status["record.warp.disconnect.duration"] = "0"
    }
}

@Event
fun playerLeave(event: PlayerLeave) {
    val data: PlayerData? = findPlayerData(event.player.uuid())
    if (data != null && state.rules.pvp) {
        // Add a player to the offline players list for LeaveAndLosePvP achievement
        offlinePlayers.add(data)

        // When the game ends, check if this player's team lost
        Events.on(GameOverEvent::class.java) { gameOver ->
            if (gameOver.winner != event.player.team()) {
                // Player left and their team lost
                val leaveCount = data.status.getOrDefault("record.pvp.leave.lose", "0").toInt() + 1
                data.status["record.pvp.leave.lose"] = leaveCount.toString()
                if (Achievement.LeaveAndLosePvP.success(data)) {
                    Achievement.LeaveAndLosePvP.set(data)
                }
            }
        }
    }
}

@Event
fun worldLoadEnd(event: WorldLoadEndEvent) {
    isNoMiningFailed = false
    isNoPowerFailed = false
    isLowPowerFailed = false
    isNoTurretsFailed = false
    isFlareOnlyFailed = false
    isDuoTurretFailed = false
}
