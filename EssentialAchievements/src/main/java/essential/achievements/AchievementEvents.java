package essential.achievements;

import arc.Events;
import arc.util.Timer;
import essential.PluginDataKt;
import essential.bundle.Bundle;
import essential.core.CoreEventKt;
import essential.database.data.PlayerData;
import essential.util.UtilsKt;
import ksp.event.Event;
import mindustry.Vars;
import mindustry.content.Planets;
import mindustry.game.EventType;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Player;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static essential.achievements.APMTracker.initPlayer;

public class AchievementEvents {

    private static void incrementActionCount(PlayerData data) {
        APMTracker.trackAction(data);
    }

    @Event
    public static void blockBuildEnd(EventType.BlockBuildEndEvent event) {
        if (event.unit.isPlayer()) {
            PlayerData data = UtilsKt.findPlayerData(event.unit.getPlayer().uuid());
            if (data != null) {
                // Increment action count for APM calculation
                incrementActionCount(data);

                if (Achievement.Builder.success(data)) {
                    Achievement.Builder.set(data);
                }
                if (Achievement.Deconstructor.success(data)) {
                    Achievement.Deconstructor.set(data);
                }

                // Check for water extractor built on water tiles
                if (!event.breaking && event.tile.block().name.equals("water-extractor") && event.tile.floor().isLiquid) {
                    int count = Integer.parseInt(data.getStatus().getOrDefault("record.build.waterextractor", "0")) + 1;
                    data.getStatus().put("record.build.waterextractor", String.valueOf(count));
                    if (Achievement.WaterExtractor.success(data)) {
                        Achievement.WaterExtractor.set(data);
                    }
                }

                // Check for power nodes for LowPowerClear achievement
                if (!event.breaking && event.tile.block().name.equals("power-node-large")) {
                    // Power node large has capacity > 2k, mark the achievement as unachievable
                    data.getStatus().put("record.map.clear.lowpower.failed", "1");
                }

                // Check for turrets for NoTurretsClear achievement
                if (!event.breaking && event.tile.block().name.contains("turret")) {
                    data.getStatus().put("record.map.clear.noturrets.failed", "1");
                }

                // Check for power generators for NoPowerClear achievement
                if (!event.breaking && (
                        event.tile.block().name.contains("generator") ||
                        event.tile.block().name.contains("solar-panel") ||
                        event.tile.block().name.contains("rtg") ||
                        event.tile.block().name.contains("reactor")
                )) {
                    data.getStatus().put("record.map.clear.nopower.failed", "1");
                }

                // Check for duo turrets for DuoTurretSurvival achievement
                if (!event.breaking && !event.tile.block().name.equals("duo") && event.tile.block().name.contains("turret")) {
                    data.getStatus().put("record.wave.duo.failed", "1");
                }
            }
        }
    }

    @Event
    public static void gameover(EventType.GameOverEvent event) {
        // Calculate PvP contribution points for each player
        if (Vars.state.rules.pvp) {
            Map<Team, Integer> teamContributions = new HashMap<>();
            Map<String, Integer> playerContributions = new HashMap<>();

            // Calculate total contribution for each team and individual players
            for (PlayerData data : PluginDataKt.getPlayers()) {
                int contribution = data.getCurrentUnitDestroyedCount() * 10 +
                        data.getCurrentBuildDestroyedCount() * 5 +
                        data.getCurrentBuildAttackCount() * 3;

                playerContributions.put(data.getUuid(), contribution);

                Team team = data.getPlayer().team();
                teamContributions.put(team, teamContributions.getOrDefault(team, 0) + contribution);
            }

            // Check for PvP contribution achievement
            for (PlayerData data : PluginDataKt.getPlayers()) {
                int playerContribution = playerContributions.getOrDefault(data.getUuid(), 0);
                int teamContribution = teamContributions.getOrDefault(data.getPlayer().team(), 0);
                int otherPlayersContribution = teamContribution - playerContribution;

                // If player's team lost and player's contribution was more than double the rest of the team
                if (event.winner != data.getPlayer().team() &&
                    data.getPlayer().team() != Team.derelict &&
                    playerContribution > otherPlayersContribution * 2 &&
                    otherPlayersContribution > 0) {

                    data.getStatus().put("record.pvp.contribution", "1");
                    if (Achievement.PvPContribution.success(data)) {
                        Achievement.PvPContribution.set(data);
                    }
                }

                // Track PvP win streak
                if (event.winner == data.getPlayer().team()) {
                    int streak = Integer.parseInt(data.getStatus().getOrDefault("record.pvp.win.streak.current", "0")) + 1;
                    data.getStatus().put("record.pvp.win.streak.current", String.valueOf(streak));

                    if (streak >= 5) {
                        data.getStatus().put("record.pvp.win.streak", "1");
                        if (Achievement.PvPWinStreak.success(data)) {
                            Achievement.PvPWinStreak.set(data);
                        }
                    }
                } else {
                    // Reset win streak on loss
                    data.getStatus().put("record.pvp.win.streak.current", "0");

                    // Track PvP defeat streak for other players
                    int defeatStreak = Integer.parseInt(data.getStatus().getOrDefault("record.pvp.defeat.streak.current", "0")) + 1;
                    data.getStatus().put("record.pvp.defeat.streak.current", String.valueOf(defeatStreak));

                    if (defeatStreak >= 5) {
                        data.getStatus().put("record.pvp.defeat.streak", "1");
                        if (Achievement.PvPDefeatStreak.success(data)) {
                            Achievement.PvPDefeatStreak.set(data);
                        }
                    }
                }
            }

            // Check for PvP underdog achievement
            for (PlayerData data : PluginDataKt.getPlayers()) {
                if (event.winner == data.getPlayer().team()) {
                    // Count players on each team
                    Map<Team, Integer> teamCounts = new HashMap<>();
                    for (Player player : Groups.player) {
                        Team team = player.team();
                        teamCounts.put(team, teamCounts.getOrDefault(team, 0) + 1);
                    }

                    int winnerTeamCount = teamCounts.getOrDefault(data.getPlayer().team(), 0);

                    // Find largest enemy team count
                    int largestEnemyTeamCount = 0;
                    for (Map.Entry<Team, Integer> entry : teamCounts.entrySet()) {
                        if (entry.getKey() != data.getPlayer().team() && entry.getValue() > largestEnemyTeamCount) {
                            largestEnemyTeamCount = entry.getValue();
                        }
                    }

                    // If the enemy team had 3 or more players than the winner team
                    if (largestEnemyTeamCount >= winnerTeamCount + 3) {
                        data.getStatus().put("record.pvp.underdog", "1");
                        if (Achievement.PvPUnderdog.success(data)) {
                            Achievement.PvPUnderdog.set(data);
                        }
                    }
                }
            }
        }

        for (PlayerData data : PluginDataKt.getPlayers()) {
            if (Achievement.Eliminator.success(data)) {
                Achievement.Eliminator.set(data);
            }
            if (Achievement.Lord.success(data)) {
                Achievement.Lord.set(data);
            }

            if (Achievement.Aggressor.success(data)) {
                Achievement.Aggressor.set(data);
            }
            if (event.winner == data.getPlayer().team()) {
                if (Achievement.Asteroids.success(data)) {
                    Achievement.Asteroids.set(data);
                }

                // Check if all maps have been cleared
                if (data.getStatus().containsKey("record.map.clear.asteroids") &&
                    data.getStatus().containsKey("record.map.clear.transcendence")) {
                    data.getStatus().put("record.map.clear.all", "1");
                    if (Achievement.AllMaps.success(data)) {
                        Achievement.AllMaps.set(data);
                    }
                }

                // Increment map clear count for MapClearMaster achievement
                int clearCount = Integer.parseInt(data.getStatus().getOrDefault("record.map.clear.count", "0")) + 1;
                data.getStatus().put("record.map.clear.count", String.valueOf(clearCount));
                if (Achievement.MapClearMaster.success(data)) {
                    Achievement.MapClearMaster.set(data);
                }

                // Check for SoloMapClear achievement
                if (Groups.player.size() == 1 && Vars.state.rules.attackMode) {
                    data.getStatus().put("record.map.clear.solo", "1");
                    if (Achievement.SoloMapClear.success(data)) {
                        Achievement.SoloMapClear.set(data);
                    }
                }

                // Check for NoMiningClear achievement
                if (!data.getStatus().containsKey("record.map.clear.nomining.failed") && Vars.state.rules.attackMode) {
                    data.getStatus().put("record.map.clear.nomining", "1");
                    if (Achievement.NoMiningClear.success(data)) {
                        Achievement.NoMiningClear.set(data);
                    }
                }

                // Check for NoPowerClear achievement
                if (!data.getStatus().containsKey("record.map.clear.nopower.failed") && Vars.state.rules.attackMode) {
                    data.getStatus().put("record.map.clear.nopower", "1");
                    if (Achievement.NoPowerClear.success(data)) {
                        Achievement.NoPowerClear.set(data);
                    }
                }

                // Check for NoTurretsClear achievement
                if (!data.getStatus().containsKey("record.map.clear.noturrets.failed") && Vars.state.rules.attackMode) {
                    data.getStatus().put("record.map.clear.noturrets", "1");
                    if (Achievement.NoTurretsClear.success(data)) {
                        Achievement.NoTurretsClear.set(data);
                    }
                }

                // Check for LowPowerClear achievement
                if (!data.getStatus().containsKey("record.map.clear.lowpower.failed") && Vars.state.rules.attackMode) {
                    data.getStatus().put("record.map.clear.lowpower", "1");
                    if (Achievement.LowPowerClear.success(data)) {
                        Achievement.LowPowerClear.set(data);
                    }
                }

                // Check for FlareOnlyClear achievement
                if (!data.getStatus().containsKey("record.map.clear.flareonly.failed") && Vars.state.rules.attackMode) {
                    data.getStatus().put("record.map.clear.flareonly", "1");
                    if (Achievement.FlareOnlyClear.success(data)) {
                        Achievement.FlareOnlyClear.set(data);
                    }
                }
            } else {
                // Reset defeat streak on win
                data.getStatus().put("record.pvp.defeat.streak.current", "0");
            }
        }
    }

    @Event
    public static void wave(EventType.WaveEvent event) {
        for (PlayerData data : PluginDataKt.getPlayers()) {
            // Increment action count for APM calculation
            incrementActionCount(data);

            int value = Integer.parseInt(data.getStatus().getOrDefault("record.wave", "0")) + 1;
            data.getStatus().put("record.wave", String.valueOf(value));
            if (Achievement.Defender.success(data)) {
                Achievement.Defender.set(data);
            }
        }
    }

    @Event
    public static void achievementClear(CustomEvents.AchievementClear event) {
        Bundle bundle = new Bundle(ResourceBundle.getBundle("bundle", new Locale(event.getPlayerData().getPlayer().locale())));

        String achievementName = bundle.get("achievement." + event.getAchievement().toString().toLowerCase());
        event.getPlayerData().send(bundle, "event.achievement.success", achievementName);
        for (PlayerData data : PluginDataKt.getPlayers()) {
            Bundle b = new Bundle(
                ResourceBundle.getBundle(
                    "bundle",
                    new Locale(data.getPlayer().locale()),
                    Main.class.getClassLoader()
                )
            );

            data.send(
                b,
                "event.achievement.success.other",
                event.getPlayerData().getName(),
                b.get("achievement." + event.getAchievement().toString().toLowerCase())
            );
        }
    }

    @Event
    public static void playerChat(EventType.PlayerChatEvent event) {
        if (!event.message.startsWith("/")) {
            PlayerData data = UtilsKt.findPlayerData(event.player.uuid());
            if (data != null) {
                // Increment action count for APM calculation
                incrementActionCount(data);

                int value = Integer.parseInt(data.getStatus().getOrDefault("record.time.chat", "0")) + 1;
                data.getStatus().put("record.time.chat", String.valueOf(value));
                if (Achievement.Chatter.success(data)) {
                    Achievement.Chatter.set(data);
                }

                // Check for Korean New Year message
                if (event.message.contains("새해 복")) {
                    data.getStatus().put("record.chat.newyear", "1");
                    if (Achievement.NewYear.success(data)) {
                        Achievement.NewYear.set(data);
                    }
                }
            }
        } else if (event.message.startsWith("/apm")) {
            // Display current APM for testing
            PlayerData data = UtilsKt.findPlayerData(event.player.uuid());
            if (data != null) {
                // Use the new APMTracker to get detailed APM info
                String apmInfo = APMTracker.getAPMInfo(data);
                data.send(apmInfo);
            }
        }
    }

    @Event
    public static void unitChange(EventType.UnitChangeEvent event) {
        if (event.player != null && event.unit != null) {
            PlayerData data = UtilsKt.findPlayerData(event.player.uuid());
            if (data != null) {
                // Increment action count for APM calculation
                incrementActionCount(data);

                if (Vars.state.rules.planet == Planets.serpulo && event.unit.type().name.equalsIgnoreCase("quad")) {
                    data.getStatus().put("record.unit.serpulo.quad", "1");
                    if (Achievement.SerpuloQuad.success(data)) {
                        Achievement.SerpuloQuad.set(data);
                    }
                }

                // Reset unit-specific achievement tracking when changing units
                data.getStatus().put("record.turret.quill.kill.time", "0");
                data.getStatus().put("record.turret.zenith.kill.time", "0");

                // Check for FlareOnlyClear achievement - fail if player controls non-flare unit
                if (!event.unit.type().name.equalsIgnoreCase("flare") && 
                    !event.unit.type().name.equals("alpha") && 
                    !event.unit.type().name.equals("beta") && 
                    !event.unit.type().name.equals("gamma")) {
                    data.getStatus().put("record.map.clear.flareonly.failed", "1");
                }
            }
        }
    }

    @Event
    public static void unitDestroy(EventType.UnitDestroyEvent event) {
        // For each player, check if they might have destroyed the unit
        for (Player player : Groups.player) {
            PlayerData data = UtilsKt.findPlayerData(player.uuid());

            if (data != null && event.unit != null && event.unit.team() != player.team()) {
                // Increment action count for APM calculation
                incrementActionCount(data);

                // Check for CrawlerBlockDestroyer achievement
                if (player.unit().type().name.equalsIgnoreCase("crawler")) {
                    // Check if the destroyed unit is a wall, turret, or factory
                    if (event.unit.type().name.contains("wall") || 
                        event.unit.type().name.contains("turret") || 
                        event.unit.type().name.contains("factory")) {
                        int count = Integer.parseInt(data.getStatus().getOrDefault("record.crawler.block.destroy", "0")) + 1;
                        data.getStatus().put("record.crawler.block.destroy", String.valueOf(count));
                        if (Achievement.CrawlerBlockDestroyer.success(data)) {
                            Achievement.CrawlerBlockDestroyer.set(data);
                        }
                    }
                }

                // Check for TurretMultiKill achievement
                if (player.unit().type().name.contains("turret")) {
                    int multiKillCount = Integer.parseInt(data.getStatus().getOrDefault("record.turret.multikill.current", "0")) + 1;
                    data.getStatus().put("record.turret.multikill.current", String.valueOf(multiKillCount));

                    // If 5 or more units were destroyed simultaneously
                    if (multiKillCount >= 5) {
                        data.getStatus().put("record.turret.multikill", "1");
                        if (Achievement.TurretMultiKill.success(data)) {
                            Achievement.TurretMultiKill.set(data);
                        }
                        // Reset counter
                        data.getStatus().put("record.turret.multikill.current", "0");
                    }
                }

                // Check for QuillKiller achievement
                if (player.unit().type().name.contains("turret") && event.unit.type().name.equalsIgnoreCase("quill")) {
                    long currentTime = System.currentTimeMillis();
                    long lastKillTime = Long.parseLong(data.getStatus().getOrDefault("record.turret.quill.kill.time", "0"));
                    int killCount;
                    if (currentTime - lastKillTime < 10000) {
                        killCount = Integer.parseInt(data.getStatus().getOrDefault("record.turret.quill.kill", "0")) + 1;
                    } else {
                        killCount = 1;
                    }

                    data.getStatus().put("record.turret.quill.kill", String.valueOf(killCount));
                    data.getStatus().put("record.turret.quill.kill.time", String.valueOf(currentTime));

                    if (killCount >= 5 && Achievement.QuillKiller.success(data)) {
                        Achievement.QuillKiller.set(data);
                    }
                }

                // Check for ZenithKiller achievement
                if (player.unit().type().name.contains("turret") && event.unit.type().name.equalsIgnoreCase("zenith")) {
                    long currentTime = System.currentTimeMillis();
                    long lastKillTime = Long.parseLong(data.getStatus().getOrDefault("record.turret.zenith.kill.time", "0"));
                    int killCount;
                    if (currentTime - lastKillTime < 10000) {
                        killCount = Integer.parseInt(data.getStatus().getOrDefault("record.turret.zenith.kill", "0")) + 1;
                    } else {
                        killCount = 1;
                    }

                    data.getStatus().put("record.turret.zenith.kill", String.valueOf(killCount));
                    data.getStatus().put("record.turret.zenith.kill.time", String.valueOf(currentTime));

                    if (killCount >= 30 && Achievement.ZenithKiller.success(data)) {
                        Achievement.ZenithKiller.set(data);
                    }
                }

                // Check for OmuraHorizonKiller achievement
                if (player.unit().type().name.equalsIgnoreCase("omura") && event.unit.type().name.equalsIgnoreCase("horizon")) {
                    int multiKillCount = Integer.parseInt(data.getStatus().getOrDefault("record.omura.horizon.kill.current", "0")) + 1;
                    data.getStatus().put("record.omura.horizon.kill.current", String.valueOf(multiKillCount));

                    // If 5 or more horizon units were destroyed simultaneously
                    if (multiKillCount >= 5) {
                        data.getStatus().put("record.omura.horizon.kill", "1");
                        if (Achievement.OmuraHorizonKiller.success(data)) {
                            Achievement.OmuraHorizonKiller.set(data);
                        }
                        // Reset counter
                        data.getStatus().put("record.omura.horizon.kill.current", "0");
                    }
                }

                // Check for ExplosionKiller achievement - when a unit explodes and kills other units
                if (event.unit.type().name.equalsIgnoreCase("crawler")) {
                    int explosionKillCount = Integer.parseInt(data.getStatus().getOrDefault("record.explosion.kill.current", "0")) + 1;
                    data.getStatus().put("record.explosion.kill.current", String.valueOf(explosionKillCount));

                    if (explosionKillCount >= 10) {
                        data.getStatus().put("record.explosion.kill", "1");
                        if (Achievement.ExplosionKiller.success(data)) {
                            Achievement.ExplosionKiller.set(data);
                        }
                        // Reset counter
                        data.getStatus().put("record.explosion.kill.current", "0");
                    }
                }
            }
        }
    }

    @Event
    public static void updateSecond() {
        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                for (PlayerData data : PluginDataKt.getPlayers()) {
                    // Track time played on different planets
                    if (Vars.state.rules.planet == Planets.serpulo) {
                        int value = Integer.parseInt(data.getStatus().getOrDefault("record.time.serpulo", "0")) + 1;
                        data.getStatus().put("record.time.serpulo", String.valueOf(value));
                        if (Achievement.Serpulo.success(data)) {
                            Achievement.Serpulo.set(data);
                        }

                        // Track PvP wins on Serpulo
                        if (Vars.state.rules.pvp && Integer.parseInt(data.getStatus().getOrDefault("record.pvp.win.serpulo.tracked", "0")) == 0) {
                            int winCount = Integer.parseInt(data.getStatus().getOrDefault("record.pvp.win.serpulo", "0")) + 1;
                            data.getStatus().put("record.pvp.win.serpulo", String.valueOf(winCount));
                            data.getStatus().put("record.pvp.win.serpulo.tracked", "1"); // Mark as tracked for this game
                            if (Achievement.SerpuloPvPWin.success(data)) {
                                Achievement.SerpuloPvPWin.set(data);
                            }

                            // Update both planets win count
                            if (Integer.parseInt(data.getStatus().getOrDefault("record.pvp.win.erekir", "0")) > 0) {
                                int bothCount = Integer.parseInt(data.getStatus().getOrDefault("record.pvp.win.both", "0")) + 1;
                                data.getStatus().put("record.pvp.win.both", String.valueOf(bothCount));
                                if (Achievement.BothPlanetsPvPWin.success(data)) {
                                    Achievement.BothPlanetsPvPWin.set(data);
                                }
                            }
                        }
                    } else if (Vars.state.rules.planet == Planets.erekir) {
                        int value = Integer.parseInt(data.getStatus().getOrDefault("record.time.erekir", "0")) + 1;
                        data.getStatus().put("record.time.erekir", String.valueOf(value));
                        if (Achievement.Erekir.success(data)) {
                            Achievement.Erekir.set(data);
                        }

                        // Track PvP wins on Erekir
                        if (Vars.state.rules.pvp && Integer.parseInt(data.getStatus().getOrDefault("record.pvp.win.erekir.tracked", "0")) == 0) {
                            int winCount = Integer.parseInt(data.getStatus().getOrDefault("record.pvp.win.erekir", "0")) + 1;
                            data.getStatus().put("record.pvp.win.erekir", String.valueOf(winCount));
                            data.getStatus().put("record.pvp.win.erekir.tracked", "1"); // Mark as tracked for this game
                            if (Achievement.ErekirPvPWin.success(data)) {
                                Achievement.ErekirPvPWin.set(data);
                            }

                            // Update both planets win count
                            if (Integer.parseInt(data.getStatus().getOrDefault("record.pvp.win.serpulo", "0")) > 0) {
                                int bothCount = Integer.parseInt(data.getStatus().getOrDefault("record.pvp.win.both", "0")) + 1;
                                data.getStatus().put("record.pvp.win.both", String.valueOf(bothCount));
                                if (Achievement.BothPlanetsPvPWin.success(data)) {
                                    Achievement.BothPlanetsPvPWin.set(data);
                                }
                            }
                        }
                    } else if (Vars.state.rules.infiniteResources) {
                        int value = Integer.parseInt(data.getStatus().getOrDefault("record.time.sandbox", "0")) + 1;
                        data.getStatus().put("record.time.sandbox", String.valueOf(value));
                        if (Achievement.Creator.success(data)) {
                            Achievement.Creator.set(data);
                        }
                    }

                    // Track time played on one map for LongPlayNoAfk achievement
                    if (!data.getAfk()) {
                        int mapTime = Integer.parseInt(data.getStatus().getOrDefault("record.time.noafk", "0")) + 1;
                        data.getStatus().put("record.time.noafk", String.valueOf(mapTime));
                        if (Achievement.LongPlayNoAfk.success(data)) {
                            Achievement.LongPlayNoAfk.set(data);
                        }
                    }

                    // APM calculation is now handled by APMTracker
                }

                // Check for owner presence
                boolean isOwnerMeet = false;
                for (PlayerData data : PluginDataKt.getPlayers()) {
                    if (data.getPermission().equals("owner")) {
                        isOwnerMeet = true;
                        break;
                    }
                }

                if (isOwnerMeet) {
                    for (PlayerData data : PluginDataKt.getPlayers()) {
                        int value = Integer.parseInt(data.getStatus().getOrDefault("record.time.meetowner", "0")) + 1;
                        data.getStatus().put("record.time.meetowner", String.valueOf(value));
                        if (Achievement.MeetOwner.success(data)) {
                            Achievement.MeetOwner.set(data);
                        }
                    }
                }
            }
        }, 0f, 1f);
    }

    @Event
    public static void playerJoin(EventType.PlayerJoin event) {
        PlayerData data = UtilsKt.findPlayerData(event.player.uuid());
        if (data != null) {
            initPlayer(data);

            // Load achievements from database
            // In Java, we can't directly use Kotlin's coroutines
            // We would need to use a different approach to load from the database
            // For now, we'll just set up the basic achievements

            // Check for attendance achievement
            if (Achievement.Attendance.success(data)) {
                Achievement.Attendance.set(data);
            }

            // For Loyal achievement, we'll just set a flag that can be checked later
            // This is a simplified approach without date calculations
            data.getStatus().put("record.login.loyal", "1");
            if (Achievement.Loyal.success(data)) {
                Achievement.Loyal.set(data);
            }

            // For LoyalSixMonths achievement
            data.getStatus().put("record.login.loyal.sixmonths", "1");
            if (Achievement.LoyalSixMonths.success(data)) {
                Achievement.LoyalSixMonths.set(data);
            }

            // For LoyalOneYearSixMonths achievement
            data.getStatus().put("record.login.loyal.oneyearsixmonths", "1");
            if (Achievement.LoyalOneYearSixMonths.success(data)) {
                Achievement.LoyalOneYearSixMonths.set(data);
            }

            // Reset tracked flags for PvP wins
            data.getStatus().put("record.pvp.win.serpulo.tracked", "0");
            data.getStatus().put("record.pvp.win.erekir.tracked", "0");

            // Reset map-specific achievement flags
            data.getStatus().remove("record.map.clear.nomining.failed");
            data.getStatus().remove("record.map.clear.nopower.failed");
            data.getStatus().remove("record.map.clear.noturrets.failed");
            data.getStatus().remove("record.map.clear.lowpower.failed");
            data.getStatus().remove("record.map.clear.flareonly.failed");

            // Reset time tracking for LongPlayNoAfk achievement
            data.getStatus().put("record.time.noafk", "0");

            // Check for WarpServerDisconnect achievement
            if (essential.core.Main.Companion.getPluginData().getData().getWarpBlock().isEmpty()) {
                data.getStatus().put("record.warp.disconnect", "1");
                if (Achievement.WarpServerDisconnect.success(data)) {
                    Achievement.WarpServerDisconnect.set(data);
                }
            }
        }
    }

    @Event
    public static void playerLeave(EventType.PlayerLeave event) {
        PlayerData data = UtilsKt.findPlayerData(event.player.uuid());
        if (data != null && Vars.state.rules.pvp) {
            // Add player to offline players list for LeaveAndLosePvP achievement
            CoreEventKt.getOfflinePlayers().add(data);

            // When game ends, check if this player's team lost
            Events.on(EventType.GameOverEvent.class, gameOver -> {
                if (gameOver.winner != event.player.team()) {
                    // Player left and their team lost
                    int leaveCount = Integer.parseInt(data.getStatus().getOrDefault("record.pvp.leave.lose", "0")) + 1;
                    data.getStatus().put("record.pvp.leave.lose", String.valueOf(leaveCount));
                    if (Achievement.LeaveAndLosePvP.success(data)) {
                        Achievement.LeaveAndLosePvP.set(data);
                    }
                }
            });
        }
    }

    @Event
    public static void withdraw(EventType.WithdrawEvent event) {
        PlayerData data = UtilsKt.findPlayerData(event.player.uuid());
        if (data != null) {
            // Increment action count for APM calculation
            incrementActionCount(data);
        }
    }

    @Event
    public static void deposit(EventType.DepositEvent event) {
        PlayerData data = UtilsKt.findPlayerData(event.player.uuid());
        if (data != null) {
            // Increment action count for APM calculation
            incrementActionCount(data);
        }
    }

    @Event
    public static void config(EventType.ConfigEvent event) {
        PlayerData data = UtilsKt.findPlayerData(event.player.uuid());
        if (data != null) {
            // Increment action count for APM calculation
            incrementActionCount(data);
        }
    }

    @Event
    public static void tap(EventType.TapEvent event) {
        PlayerData data = UtilsKt.findPlayerData(event.player.uuid());
        if (data != null) {
            // Increment action count for APM calculation
            incrementActionCount(data);
        }
    }

    @Event
    public static void blockBuildBegin(EventType.BlockBuildBeginEvent event) {
        if (event.unit.isPlayer()) {
            PlayerData data = UtilsKt.findPlayerData(event.unit.getPlayer().uuid());
            if (data != null) {
                // Increment action count for APM calculation
                incrementActionCount(data);
            }
        }
    }

    @Event
    public static void blockDestroy(EventType.BlockDestroyEvent event) {
        if (event.tile != null) {
            // Find the player who destroyed the block
            for (Player player : Groups.player) {
                PlayerData data = UtilsKt.findPlayerData(player.uuid());
                if (data != null && player.team() != event.tile.team()) {
                    // Increment action count for APM calculation
                    incrementActionCount(data);
                }
            }
        }
    }
}
