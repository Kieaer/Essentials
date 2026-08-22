package essential.core

import PluginTest.Companion.loadGame
import PluginTest.Companion.newPlayer
import PluginTest.Companion.player
import PluginTest.Companion.leavePlayer
import mindustry.Vars
import mindustry.Vars.*
import mindustry.content.UnitTypes
import mindustry.content.Blocks
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.world.Tile
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.random.Random
import arc.util.Time
import arc.Core
import java.lang.Thread.sleep
import java.io.File
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import kotlin.test.Ignore

@Ignore
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class PerformanceTest {
    companion object {
        private var done = false
        private val resultsFile = File("d:\\Github\\Essentials\\benchmark_results.md")
    }

    private fun logResult(message: String) {
        println(message)
        try {
            resultsFile.appendText(message + "\n")
        } catch (e: Exception) {
            // ignore
        }
    }

    @BeforeTest
    fun setup() {
        if (!done) {
            System.setProperty("test", "yes")
            loadGame(true)
            
            val p = newPlayer()
            player = p.first.self()
            
            done = true
            
            // Clear and initialize results file
            try {
                resultsFile.writeText("# Benchmark Stress Test Results\n\n")
            } catch (e: Exception) {
                // ignore
            }
            
            logResult("Warming up JIT compiler...")
            runBenchmarkTicks(120, 60)
            logResult("Warm-up complete.\n")
        }
    }

    private fun resetWorld(size: Int = 200) {
        val tiles = world.resize(size, size)
        world.beginMapLoad()
        for (x in 0 until tiles.width) {
            for (y in 0 until tiles.height) {
                tiles.set(x, y, Tile(x, y, Blocks.stone, Blocks.air, Blocks.air))
            }
        }
        world.endMapLoad()

        state.rules.unitDamageMultiplier = 0f
        state.rules.blockDamageMultiplier = 0f
        
        // Spawn cores
        world.tile(10, 10).setBlock(Blocks.coreShard, Team.sharded, 0)
        world.tile(size - 10, size - 10).setBlock(Blocks.coreShard, Team.crux, 0)

        // Keep only primary player and their unit, remove others
        val primaryPlayer = player
        val playerUnits = Groups.player.map { it.unit() }.toSet()
        Groups.unit.each { u ->
            if (u !in playerUnits) {
                u.remove()
            }
        }
        
        // Remove additional players
        val playersToLeave = Groups.player.filter { it.uuid() != primaryPlayer.uuid() }
        playersToLeave.forEach { p ->
            p.remove()
        }
        Groups.player.update()
    }

    private fun runBenchmarkTicks(ticks: Int = 60, targetRate: Int = 60): Double {
        val tickIntervalNs = 1_000_000_000L / targetRate
        val start = System.nanoTime()
        repeat(ticks) {
            val tickStart = System.nanoTime()
            logic.update()
            val elapsed = System.nanoTime() - tickStart
            val sleepNs = tickIntervalNs - elapsed
            if (sleepNs > 0) {
                val sleepMs = sleepNs / 1_000_000
                val sleepNsRemainder = sleepNs % 1_000_000
                try {
                    sleep(sleepMs, sleepNsRemainder.toInt())
                } catch (e: InterruptedException) {
                    // ignore
                }
            }
        }
        val totalDuration = System.nanoTime() - start
        val elapsedSeconds = totalDuration / 1_000_000_000.0
        return ticks.toDouble() / elapsedSeconds
    }

    @Test
    fun testCombinedLoadCapacity_1_3_2() {
        logResult("## Combined Load Capacity Test (Ratio 1:3:2)")
        resetWorld(200)

        var playerCount = 0
        var unitCount = 0
        var buildingCount = 0
        
        // Increments maintain ratio 1:3:2 (50 players, 150 units, 100 buildings)
        val playerIncrement = 50
        val unitIncrement = 150
        val buildingIncrement = 100
        val rand = Random(42)

        val tiles = mutableListOf<Tile>()
        for (x in 0 until world.width()) {
            for (y in 0 until world.height()) {
                tiles.add(world.tile(x, y))
            }
        }
        tiles.shuffle(rand)
        var tileIndex = 0

        while (true) {
            var failedToAdd = false
            repeat(playerIncrement) {
                try {
                    newPlayer()
                    playerCount++
                } catch (e: Exception) {
                    logResult("Failed to add player: ${e.message}")
                    failedToAdd = true
                }
            }
            if (failedToAdd) break

            // Spawn units
            val width = world.width() * tilesize
            val height = world.height() * tilesize
            val margin = 10f * tilesize
            repeat(unitIncrement) { i ->
                val rx = margin + rand.nextFloat() * (width - 2 * margin)
                val ry = margin + rand.nextFloat() * (height - 2 * margin)
                val team = if (i % 2 == 0) Team.sharded else Team.crux
                UnitTypes.dagger.spawn(team, rx, ry)
            }
            unitCount += unitIncrement

            // Place buildings
            if (tileIndex + buildingIncrement < tiles.size) {
                repeat(buildingIncrement) {
                    val tile = tiles[tileIndex++]
                    tile.setBlock(Blocks.router, Team.sharded, 0)
                }
                buildingCount += buildingIncrement
            } else {
                logResult("No more free tiles for buildings!")
            }

            val tps = runBenchmarkTicks(60, 60)
            logResult("- Players: $playerCount | Units: $unitCount | Buildings: $buildingCount | TPS: ${String.format("%.2f", tps)}")

            if (tps < 40.0) {
                logResult("\n> **Max Combined Capacity (1:3:2)**: $playerCount players, $unitCount units, $buildingCount buildings (TPS dropped to ${String.format("%.2f", tps)})\n")
                break
            }
        }
    }

    @Test
    fun testCombinedLoadCapacity_1_6_3() {
        logResult("## Combined Load Capacity Test (Ratio 1:6:3)")
        resetWorld(200)

        var playerCount = 0
        var unitCount = 0
        var buildingCount = 0
        
        // Increments maintain ratio 1:6:3 (50 players, 300 units, 150 buildings)
        val playerIncrement = 50
        val unitIncrement = 300
        val buildingIncrement = 150
        val rand = Random(42)

        val tiles = mutableListOf<Tile>()
        for (x in 0 until world.width()) {
            for (y in 0 until world.height()) {
                tiles.add(world.tile(x, y))
            }
        }
        tiles.shuffle(rand)
        var tileIndex = 0

        while (true) {
            var failedToAdd = false
            repeat(playerIncrement) {
                try {
                    newPlayer()
                    playerCount++
                } catch (e: Exception) {
                    logResult("Failed to add player: ${e.message}")
                    failedToAdd = true
                }
            }
            if (failedToAdd) break

            // Spawn units
            val width = world.width() * tilesize
            val height = world.height() * tilesize
            val margin = 10f * tilesize
            repeat(unitIncrement) { i ->
                val rx = margin + rand.nextFloat() * (width - 2 * margin)
                val ry = margin + rand.nextFloat() * (height - 2 * margin)
                val team = if (i % 2 == 0) Team.sharded else Team.crux
                UnitTypes.dagger.spawn(team, rx, ry)
            }
            unitCount += unitIncrement

            // Place buildings
            if (tileIndex + buildingIncrement < tiles.size) {
                repeat(buildingIncrement) {
                    val tile = tiles[tileIndex++]
                    tile.setBlock(Blocks.router, Team.sharded, 0)
                }
                buildingCount += buildingIncrement
            } else {
                logResult("No more free tiles for buildings!")
            }

            val tps = runBenchmarkTicks(60, 60)
            logResult("- Players: $playerCount | Units: $unitCount | Buildings: $buildingCount | TPS: ${String.format("%.2f", tps)}")

            if (tps < 40.0) {
                logResult("\n> **Max Combined Capacity (1:6:3)**: $playerCount players, $unitCount units, $buildingCount buildings (TPS dropped to ${String.format("%.2f", tps)})\n")
                break
            }
        }
    }

    @Test
    fun testCombinedLoadCapacity_0_4_1() {
        logResult("## Combined Load Capacity Test (Ratio 0:4:1)")
        resetWorld(200)

        val playerCount = 0
        var unitCount = 0
        var buildingCount = 0
        
        // Increments maintain ratio 0:4:1 (0 players, 400 units, 100 buildings)
        val unitIncrement = 400
        val buildingIncrement = 100
        val rand = Random(42)

        val tiles = mutableListOf<Tile>()
        for (x in 0 until world.width()) {
            for (y in 0 until world.height()) {
                tiles.add(world.tile(x, y))
            }
        }
        tiles.shuffle(rand)
        var tileIndex = 0

        while (true) {
            // Spawn units
            val width = world.width() * tilesize
            val height = world.height() * tilesize
            val margin = 10f * tilesize
            repeat(unitIncrement) { i ->
                val rx = margin + rand.nextFloat() * (width - 2 * margin)
                val ry = margin + rand.nextFloat() * (height - 2 * margin)
                val team = if (i % 2 == 0) Team.sharded else Team.crux
                UnitTypes.dagger.spawn(team, rx, ry)
            }
            unitCount += unitIncrement

            // Place buildings
            if (tileIndex + buildingIncrement < tiles.size) {
                repeat(buildingIncrement) {
                    val tile = tiles[tileIndex++]
                    tile.setBlock(Blocks.router, Team.sharded, 0)
                }
                buildingCount += buildingIncrement
            } else {
                logResult("No more free tiles for buildings!")
            }

            val tps = runBenchmarkTicks(60, 60)
            logResult("- Players: $playerCount | Units: $unitCount | Buildings: $buildingCount | TPS: ${String.format("%.2f", tps)}")

            if (tps < 40.0) {
                logResult("\n> **Max Combined Capacity (0:4:1)**: $playerCount players, $unitCount units, $buildingCount buildings (TPS dropped to ${String.format("%.2f", tps)})\n")
                break
            }
        }
    }
}
