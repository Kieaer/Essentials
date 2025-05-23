package essential.achievements

import arc.Events
import essential.achievements.APMTracker.Companion.findPlayerByUuid
import essential.bundle.Bundle
import essential.core.Main.Companion.scope
import essential.core.offlinePlayers
import essential.database.data.PlayerDataEntity
import essential.database.data.getPlayerAchievements
import essential.players
import essential.util.startInfiniteScheduler
import kotlinx.coroutines.launch
import ksp.event.Event
import mindustry.Vars.state
import mindustry.content.Planets
import mindustry.game.EventType
import mindustry.game.EventType.*
import mindustry.gen.Groups
import java.util.*

private fun incrementActionCount(data: PlayerDataEntity) {
    APMTracker.trackAction(data)
}

@Event
fun blockBuildEnd(event: BlockBuildEndEvent) {
    if (event.unit.isPlayer) {
        val data: PlayerDataEntity? = findPlayerByUuid(event.unit.player.uuid())
        if (data != null) {
            // Increment action count for APM calculation
            incrementActionCount(data)

            if (Achievement.Builder.success(data)) {
                Achievement.Builder.set(data)
            }
            if (Achievement.Deconstructor.success(data)) {
                Achievement.Deconstructor.set(data)
            }

            // Check for water extractor built on water tiles
            if (!event.breaking && event.tile.block().name == "water-extractor" && event.tile.floor().isLiquid) {
                val count = data.status.getOrDefault("record.build.waterextractor", "0").toInt() + 1
                data.status.put("record.build.waterextractor", count.toString())
                if (Achievement.WaterExtractor.success(data)) {
                    Achievement.WaterExtractor.set(data)
                }
            }

            // Check for power nodes for LowPowerClear achievement
            if (!event.breaking && (event.tile.block().name == "power-node-large")) {
                // Power node large has capacity > 2k, mark the achievement as unachievable
                data.status.put("record.map.clear.lowpower.failed", "1")
            }

            // Check for turrets for NoTurretsClear achievement
            if (!event.breaking && event.tile.block().name.contains("turret")) {
                data.status.put("record.map.clear.noturrets.failed", "1")
            }

            // Check for power generators for NoPowerClear achievement
            if (!event.breaking && (
                        event.tile.block().name.contains("generator") ||
                                event.tile.block().name.contains("solar-panel") ||
                                event.tile.block().name.contains("rtg") ||
                                event.tile.block().name.contains("reactor")
                        )
            ) {
                data.status.put("record.map.clear.nopower.failed", "1")
            }

            // Check for duo turrets for DuoTurretSurvival achievement
            if (!event.breaking && event.tile.block().name != "duo" && event.tile.block().name.contains("turret")) {
                data.status.put("record.wave.duo.failed", "1")
            }
        }
    }
}

@Event
fun gameover(event: GameOverEvent) {
    // Calculate PvP contribution points for each player
    if (state.rules.pvp) {
        val teamContributions = mutableMapOf<mindustry.game.Team, Int>()
        val playerContributions = mutableMapOf<String, Int>()

        // Calculate total contribution for each team and individual players
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

            // If player's team lost and player's contribution was more than double the rest of the team
            if (event.winner != data.player.team() &&
                data.player.team() != mindustry.game.Team.derelict &&
                playerContribution > otherPlayersContribution * 2 &&
                otherPlayersContribution > 0
            ) {

                data.status.put("record.pvp.contribution", "1")
                if (Achievement.PvPContribution.success(data)) {
                    Achievement.PvPContribution.set(data)
                }
            }

            // Track PvP win streak
            if (event.winner === data.player.team()) {
                val streak = data.status.getOrDefault("record.pvp.win.streak.current", "0").toInt() + 1
                data.status.put("record.pvp.win.streak.current", streak.toString())

                if (streak >= 5) {
                    data.status.put("record.pvp.win.streak", "1")
                    if (Achievement.PvPWinStreak.success(data)) {
                        Achievement.PvPWinStreak.set(data)
                    }
                }
            } else {
                // Reset win streak on loss
                data.status.put("record.pvp.win.streak.current", "0")

                // Track PvP defeat streak for other players
                val defeatStreak = data.status.getOrDefault("record.pvp.defeat.streak.current", "0").toInt() + 1
                data.status.put("record.pvp.defeat.streak.current", defeatStreak.toString())

                if (defeatStreak >= 5) {
                    data.status.put("record.pvp.defeat.streak", "1")
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
                val teamCounts = mutableMapOf<mindustry.game.Team, Int>()
                Groups.player.forEach { player ->
                    val team = player.team()
                    teamCounts[team] = (teamCounts[team] ?: 0) + 1
                }

                val winnerTeamCount = teamCounts[data.player.team()] ?: 0
                val largestEnemyTeamCount = teamCounts.filter { it.key != data.player.team() }
                    .maxByOrNull { it.value }?.value ?: 0

                // If the enemy team had 3 or more players than the winner team
                if (largestEnemyTeamCount >= winnerTeamCount + 3) {
                    data.status.put("record.pvp.underdog", "1")
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
        if (event.winner === data.player.team()) {
            if (Achievement.Asteroids.success(data)) {
                Achievement.Asteroids.set(data)
            }

            // Check if all maps have been cleared
            if (data.status.containsKey("record.map.clear.asteroids") &&
                data.status.containsKey("record.map.clear.transcendence")
            ) {
                data.status.put("record.map.clear.all", "1")
                if (Achievement.AllMaps.success(data)) {
                    Achievement.AllMaps.set(data)
                }
            }

            // Increment map clear count for MapClearMaster achievement
            val clearCount = data.status.getOrDefault("record.map.clear.count", "0").toInt() + 1
            data.status.put("record.map.clear.count", clearCount.toString())
            if (Achievement.MapClearMaster.success(data)) {
                Achievement.MapClearMaster.set(data)
            }

            // Check for SoloMapClear achievement
            if (Groups.player.size() == 1 && state.rules.attackMode) {
                data.status.put("record.map.clear.solo", "1")
                if (Achievement.SoloMapClear.success(data)) {
                    Achievement.SoloMapClear.set(data)
                }
            }

            // Check for NoMiningClear achievement
            if (!data.status.containsKey("record.map.clear.nomining.failed") && state.rules.attackMode) {
                data.status.put("record.map.clear.nomining", "1")
                if (Achievement.NoMiningClear.success(data)) {
                    Achievement.NoMiningClear.set(data)
                }
            }

            // Check for NoPowerClear achievement
            if (!data.status.containsKey("record.map.clear.nopower.failed") && state.rules.attackMode) {
                data.status.put("record.map.clear.nopower", "1")
                if (Achievement.NoPowerClear.success(data)) {
                    Achievement.NoPowerClear.set(data)
                }
            }

            // Check for NoTurretsClear achievement
            if (!data.status.containsKey("record.map.clear.noturrets.failed") && state.rules.attackMode) {
                data.status.put("record.map.clear.noturrets", "1")
                if (Achievement.NoTurretsClear.success(data)) {
                    Achievement.NoTurretsClear.set(data)
                }
            }

            // Check for LowPowerClear achievement
            if (!data.status.containsKey("record.map.clear.lowpower.failed") && state.rules.attackMode) {
                data.status.put("record.map.clear.lowpower", "1")
                if (Achievement.LowPowerClear.success(data)) {
                    Achievement.LowPowerClear.set(data)
                }
            }

            // Check for FlareOnlyClear achievement
            if (!data.status.containsKey("record.map.clear.flareonly.failed") && state.rules.attackMode) {
                data.status.put("record.map.clear.flareonly", "1")
                if (Achievement.FlareOnlyClear.success(data)) {
                    Achievement.FlareOnlyClear.set(data)
                }
            }
        } else {
            // Reset defeat streak on win
            data.status.put("record.pvp.defeat.streak.current", "0")
        }
    }
}

@Event
fun wave(event: WaveEvent) {
    players.forEach { data ->
        // Increment action count for APM calculation
        incrementActionCount(data)

        val value = data.status.getOrDefault("record.wave", "0").toInt() + 1
        data.status.put("record.wave", value.toString())
        if (Achievement.Defender.success(data)) {
            Achievement.Defender.set(data)
        }
    }
}

@Event
fun achievementClear(event: CustomEvents.AchievementClear) {
    val bundle = Bundle(java.util.ResourceBundle.getBundle("bundle", Locale.of(event.playerData.player.locale())))

    event.playerData.send(bundle, "event.achievement.success", event.achievement.toString().lowercase())
    players.forEach { data ->
        val b = Bundle(
            java.util.ResourceBundle.getBundle(
                "bundle",
                Locale.of(data.player.locale()),
                Main::class.java.getClassLoader()
            )
        )

        data.send(
            b,
            "event.achievement.success.other",
            event.playerData.name,
            b.get("achievement." + event.achievement.toString().lowercase())
        )
    }
}

@Event
fun playerChat(event: PlayerChatEvent) {
    if (!event.message.startsWith("/")) {
        val data: PlayerDataEntity? = findPlayerByUuid(event.player.uuid())
        if (data != null) {
            // Increment action count for APM calculation
            incrementActionCount(data)

            val value = data.status.getOrDefault("record.time.chat", "0").toInt() + 1
            data.status.put("record.time.chat", value.toString())
            if (Achievement.Chatter.success(data)) {
                Achievement.Chatter.set(data)
            }

            // Check for Korean New Year message
            if (event.message.contains("새해 복")) {
                data.status.put("record.chat.newyear", "1")
                if (Achievement.NewYear.success(data)) {
                    Achievement.NewYear.set(data)
                }
            }
        }
    } else if (event.message.startsWith("/apm")) {
        // Display current APM for testing
        val data: PlayerDataEntity? = findPlayerByUuid(event.player.uuid())
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
        val data: PlayerDataEntity? = findPlayerByUuid(event.player.uuid())
        if (data != null) {
            // Increment action count for APM calculation
            incrementActionCount(data)

            if (state.rules.planet === Planets.serpulo && event.unit.type.name.equals("quad", true)) {
                data.status.put("record.unit.serpulo.quad", "1")
                if (Achievement.SerpuloQuad.success(data)) {
                    Achievement.SerpuloQuad.set(data)
                }
            }

            // Reset unit-specific achievement tracking when changing units
            data.status.put("record.turret.quill.kill.time", "0")
            data.status.put("record.turret.zenith.kill.time", "0")

            // Check for FlareOnlyClear achievement - fail if player controls non-flare unit
            if (!event.unit.type.name.equals(
                    "flare",
                    true
                ) && event.unit.type.name != "alpha" && event.unit.type.name != "beta" && event.unit.type.name != "gamma"
            ) {
                data.status.put("record.map.clear.flareonly.failed", "1")
            }
        }
    }
}

@Event
fun unitDestroy(event: UnitDestroyEvent) {
    // For each player, check if they might have destroyed the unit
    for (player in Groups.player) {
        val data: PlayerDataEntity? = findPlayerByUuid(player.uuid())

        if (data != null && event.unit != null && event.unit.team() != player.team()) {
            // Increment action count for APM calculation
            incrementActionCount(data)

            // Check for CrawlerBlockDestroyer achievement
            if (player.unit().type.name.equals("crawler", true)) {
                // Check if the destroyed unit is a wall, turret, or factory
                if (event.unit.type.name.contains("wall") || event.unit.type.name.contains("turret") || event.unit.type.name.contains(
                        "factory"
                    )
                ) {
                    val count = data.status.getOrDefault("record.crawler.block.destroy", "0").toInt() + 1
                    data.status.put("record.crawler.block.destroy", count.toString())
                    if (Achievement.CrawlerBlockDestroyer.success(data)) {
                        Achievement.CrawlerBlockDestroyer.set(data)
                    }
                }
            }

            // Check for TurretMultiKill achievement
            if (player.unit().type.name.contains("turret")) {
                val multiKillCount =
                    data.status.getOrDefault("record.turret.multikill.current", "0").toInt() + 1
                data.status.put("record.turret.multikill.current", multiKillCount.toString())

                // If 5 or more units were destroyed simultaneously
                if (multiKillCount >= 5) {
                    data.status.put("record.turret.multikill", "1")
                    if (Achievement.TurretMultiKill.success(data)) {
                        Achievement.TurretMultiKill.set(data)
                    }
                    // Reset counter
                    data.status.put("record.turret.multikill.current", "0")
                }
            }

            // Check for QuillKiller achievement
            if (player.unit().type.name.contains("turret") && event.unit.type.name.equals("quill", true)) {
                val currentTime = System.currentTimeMillis()
                val lastKillTime = data.status.getOrDefault("record.turret.quill.kill.time", "0").toLong()
                val killCount = if (currentTime - lastKillTime < 10000) {
                    data.status.getOrDefault("record.turret.quill.kill", "0").toInt() + 1
                } else {
                    1
                }

                data.status.put("record.turret.quill.kill", killCount.toString())
                data.status.put("record.turret.quill.kill.time", currentTime.toString())

                if (killCount >= 5 && Achievement.QuillKiller.success(data)) {
                    Achievement.QuillKiller.set(data)
                }
            }

            // Check for ZenithKiller achievement
            if (player.unit().type.name.contains("turret") && event.unit.type.name.equals("zenith", true)) {
                val currentTime = System.currentTimeMillis()
                val lastKillTime = data.status.getOrDefault("record.turret.zenith.kill.time", "0").toLong()
                val killCount = if (currentTime - lastKillTime < 10000) {
                    data.status.getOrDefault("record.turret.zenith.kill", "0").toInt() + 1
                } else {
                    1
                }

                data.status.put("record.turret.zenith.kill", killCount.toString())
                data.status.put("record.turret.zenith.kill.time", currentTime.toString())

                if (killCount >= 30 && Achievement.ZenithKiller.success(data)) {
                    Achievement.ZenithKiller.set(data)
                }
            }

            // Check for OmuraHorizonKiller achievement
            if (player.unit().type.name.equals("omura", true) && event.unit.type.name.equals("horizon", true)) {
                val multiKillCount =
                    data.status.getOrDefault("record.omura.horizon.kill.current", "0").toInt() + 1
                data.status.put("record.omura.horizon.kill.current", multiKillCount.toString())

                // If 5 or more horizon units were destroyed simultaneously
                if (multiKillCount >= 5) {
                    data.status.put("record.omura.horizon.kill", "1")
                    if (Achievement.OmuraHorizonKiller.success(data)) {
                        Achievement.OmuraHorizonKiller.set(data)
                    }
                    // Reset counter
                    data.status.put("record.omura.horizon.kill.current", "0")
                }
            }

            // Check for ExplosionKiller achievement - when a unit explodes and kills other units
            if (event.unit.type.name.equals("crawler", true)) {
                val explosionKillCount =
                    data.status.getOrDefault("record.explosion.kill.current", "0").toInt() + 1
                data.status.put("record.explosion.kill.current", explosionKillCount.toString())

                if (explosionKillCount >= 10) {
                    data.status.put("record.explosion.kill", "1")
                    if (Achievement.ExplosionKiller.success(data)) {
                        Achievement.ExplosionKiller.set(data)
                    }
                    // Reset counter
                    data.status.put("record.explosion.kill.current", "0")
                }
            }
        }
    }
}

@Event
fun updateSecond() {
    scope.startInfiniteScheduler {
        for (data in players) {
            // Track time played on different planets
            if (state.rules.planet === Planets.serpulo) {
                val value = data.status.getOrDefault("record.time.serpulo", "0").toInt() + 1
                data.status.put("record.time.serpulo", value.toString())
                if (Achievement.Serpulo.success(data)) {
                    Achievement.Serpulo.set(data)
                }

                // Track PvP wins on Serpulo
                if (state.rules.pvp && data.status.getOrDefault("record.pvp.win.serpulo.tracked", "0")
                        .toInt() == 0
                ) {
                    val winCount = data.status.getOrDefault("record.pvp.win.serpulo", "0").toInt() + 1
                    data.status.put("record.pvp.win.serpulo", winCount.toString())
                    data.status.put("record.pvp.win.serpulo.tracked", "1") // Mark as tracked for this game
                    if (Achievement.SerpuloPvPWin.success(data)) {
                        Achievement.SerpuloPvPWin.set(data)
                    }

                    // Update both planets win count
                    if (data.status.getOrDefault("record.pvp.win.erekir", "0").toInt() > 0) {
                        val bothCount = data.status.getOrDefault("record.pvp.win.both", "0").toInt() + 1
                        data.status.put("record.pvp.win.both", bothCount.toString())
                        if (Achievement.BothPlanetsPvPWin.success(data)) {
                            Achievement.BothPlanetsPvPWin.set(data)
                        }
                    }
                }
            } else if (state.rules.planet === Planets.erekir) {
                val value = data.status.getOrDefault("record.time.erekir", "0").toInt() + 1
                data.status.put("record.time.erekir", value.toString())
                if (Achievement.Erekir.success(data)) {
                    Achievement.Erekir.set(data)
                }

                // Track PvP wins on Erekir
                if (state.rules.pvp && data.status.getOrDefault("record.pvp.win.erekir.tracked", "0")
                        .toInt() == 0
                ) {
                    val winCount = data.status.getOrDefault("record.pvp.win.erekir", "0").toInt() + 1
                    data.status.put("record.pvp.win.erekir", winCount.toString())
                    data.status.put("record.pvp.win.erekir.tracked", "1") // Mark as tracked for this game
                    if (Achievement.ErekirPvPWin.success(data)) {
                        Achievement.ErekirPvPWin.set(data)
                    }

                    // Update both planets win count
                    if (data.status.getOrDefault("record.pvp.win.serpulo", "0").toInt() > 0) {
                        val bothCount = data.status.getOrDefault("record.pvp.win.both", "0").toInt() + 1
                        data.status.put("record.pvp.win.both", bothCount.toString())
                        if (Achievement.BothPlanetsPvPWin.success(data)) {
                            Achievement.BothPlanetsPvPWin.set(data)
                        }
                    }
                }
            } else if (state.rules.infiniteResources) {
                val value = data.status.getOrDefault("record.time.sandbox", "0").toInt() + 1
                data.status.put("record.time.sandbox", value.toString())
                if (Achievement.Creator.success(data)) {
                    Achievement.Creator.set(data)
                }
            }

            // Track time played on one map for LongPlayNoAfk achievement
            if (!data.afk) {
                val mapTime = data.status.getOrDefault("record.time.noafk", "0").toInt() + 1
                data.status.put("record.time.noafk", mapTime.toString())
                if (Achievement.LongPlayNoAfk.success(data)) {
                    Achievement.LongPlayNoAfk.set(data)
                }
            }

            // APM calculation is now handled by APMTracker
        }

        // Check for owner presence
        var isOwnerMeet = false
        for (data in players) {
            if (data.permission.equals("owner")) {
                isOwnerMeet = true
                break
            }
        }

        if (isOwnerMeet) {
            for (data in players) {
                val value = data.status.getOrDefault("record.time.meetowner", "0").toInt() + 1
                data.status.put("record.time.meetowner", value.toString())
                if (Achievement.MeetOwner.success(data)) {
                    Achievement.MeetOwner.set(data)
                }
            }
        }
    }
}

@Event
fun playerJoin(event: PlayerJoin) {
    val data: PlayerDataEntity? = findPlayerByUuid(event.player.uuid())
    if (data != null) {
        // Load achievements from database
        scope.launch {
            try {
                // Get all completed achievements from the database
                val achievements = getPlayerAchievements(data)

                // Store them in data.status for use during this session
                achievements.forEach { achievement ->
                    data.status.put(
                        "achievement.${achievement.achievementName}",
                        achievement.completedAt.toString()
                    )
                }

                // Log the number of loaded achievements
                data.send("Loaded ${achievements.size} achievements from database")
            } catch (ex: Exception) {
                // Log any errors
                data.err("Error loading achievements from database: ${ex.message}")
            }
        }

        // Check for attendance achievement
        if (Achievement.Attendance.success(data)) {
            Achievement.Attendance.set(data)
        }

        // For Loyal achievement, we'll just set a flag that can be checked later
        // This is a simplified approach without date calculations
        data.status.put("record.login.loyal", "1")
        if (Achievement.Loyal.success(data)) {
            Achievement.Loyal.set(data)
        }

        // For LoyalSixMonths achievement
        data.status.put("record.login.loyal.sixmonths", "1")
        if (Achievement.LoyalSixMonths.success(data)) {
            Achievement.LoyalSixMonths.set(data)
        }

        // For LoyalOneYearSixMonths achievement
        data.status.put("record.login.loyal.oneyearsixmonths", "1")
        if (Achievement.LoyalOneYearSixMonths.success(data)) {
            Achievement.LoyalOneYearSixMonths.set(data)
        }

        // Reset tracked flags for PvP wins
        data.status.put("record.pvp.win.serpulo.tracked", "0")
        data.status.put("record.pvp.win.erekir.tracked", "0")

        // Reset map-specific achievement flags
        data.status.remove("record.map.clear.nomining.failed")
        data.status.remove("record.map.clear.nopower.failed")
        data.status.remove("record.map.clear.noturrets.failed")
        data.status.remove("record.map.clear.lowpower.failed")
        data.status.remove("record.map.clear.flareonly.failed")

        // Reset time tracking for LongPlayNoAfk achievement
        data.status.put("record.time.noafk", "0")

        // Check for WarpServerDisconnect achievement
        if (essential.core.Main.Companion.pluginData.data.warpBlock.isEmpty()) {
            data.status.put("record.warp.disconnect", "1")
            if (Achievement.WarpServerDisconnect.success(data)) {
                Achievement.WarpServerDisconnect.set(data)
            }
        }
    }
}

@Event
fun playerLeave(event: PlayerLeave) {
    val data: PlayerDataEntity? = findPlayerByUuid(event.player.uuid())
    if (data != null && state.rules.pvp) {
        // Add player to offline players list for LeaveAndLosePvP achievement
        offlinePlayers.add(data)

        // When game ends, check if this player's team lost
        Events.on(EventType.GameOverEvent::class.java) { gameOver ->
            if (gameOver.winner != event.player.team()) {
                // Player left and their team lost
                val leaveCount = data.status.getOrDefault("record.pvp.leave.lose", "0").toInt() + 1
                data.status.put("record.pvp.leave.lose", leaveCount.toString())
                if (Achievement.LeaveAndLosePvP.success(data)) {
                    Achievement.LeaveAndLosePvP.set(data)
                }
            }
        }
    }
}

@Event
fun withdraw(event: WithdrawEvent) {
    val data: PlayerDataEntity? = findPlayerByUuid(event.player.uuid())
    if (data != null) {
        // Increment action count for APM calculation
        incrementActionCount(data)
    }
}

@Event
fun deposit(event: DepositEvent) {
    val data: PlayerDataEntity? = findPlayerByUuid(event.player.uuid())
    if (data != null) {
        // Increment action count for APM calculation
        incrementActionCount(data)
    }
}

@Event
fun config(event: ConfigEvent) {
    val data: PlayerDataEntity? = findPlayerByUuid(event.player.uuid())
    if (data != null) {
        // Increment action count for APM calculation
        incrementActionCount(data)
    }
}

@Event
fun tap(event: TapEvent) {
    val data: PlayerDataEntity? = findPlayerByUuid(event.player.uuid())
    if (data != null) {
        // Increment action count for APM calculation
        incrementActionCount(data)
    }
}

@Event
fun blockBuildBegin(event: BlockBuildBeginEvent) {
    if (event.unit.isPlayer) {
        val data: PlayerDataEntity? = findPlayerByUuid(event.unit.player.uuid())
        if (data != null) {
            // Increment action count for APM calculation
            incrementActionCount(data)
        }
    }
}

@Event
fun blockDestroy(event: BlockDestroyEvent) {
    if (event.tile != null) {
        // Find the player who destroyed the block
        for (player in Groups.player) {
            val data: PlayerDataEntity? = findPlayerByUuid(player.uuid())
            if (data != null && player.team() != event.tile.team()) {
                // Increment action count for APM calculation
                incrementActionCount(data)
            }
        }
    }
}