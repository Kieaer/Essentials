package essential.core

import PluginTest.Companion.clientCommand
import PluginTest.Companion.createPlayer
import PluginTest.Companion.err
import PluginTest.Companion.leavePlayer
import PluginTest.Companion.loadGame
import PluginTest.Companion.log
import PluginTest.Companion.newPlayer
import PluginTest.Companion.player
import PluginTest.Companion.setPermission
import PluginTest.Companion.updateTick
import PluginTest.Companion.waitUntil
import PluginTest.Companion.serverCommand
import PluginTest.Companion.randomTile
import arc.Events
import essential.common.bundle.Bundle
import essential.common.database.data.PlayerData
import essential.common.database.data.getPlayerData
import essential.common.players
import essential.common.pluginData
import essential.common.util.findPlayerData
import kotlinx.coroutines.runBlocking
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.Items
import mindustry.content.UnitTypes
import mindustry.content.Bullets
import mindustry.game.EventType
import mindustry.game.EventType.*
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Call
import mindustry.entities.units.BuildPlan
import kotlin.test.*

@Ignore
class CoverageTest {
    companion object {
        private var done = false
    }

    @BeforeTest
    fun setup() {
        if (!done) {
            loadGame(true)
            done = true
        }
    }

    @Test
    fun testServerCommands() {
        val p = newPlayer()
        val dummyPlayer = p.first
        val dummyData = p.second

        // set permission to owner/admin to allow command logic to execute
        setPermission(dummyPlayer, "owner", true)

        // 1. chat
        serverCommand.handleMessage("chat off")
        assertTrue(essential.core.isGlobalMute)
        serverCommand.handleMessage("chat on")
        assertFalse(essential.core.isGlobalMute)

        // 2. kickall
        serverCommand.handleMessage("kickall")

        // 3. kill
        serverCommand.handleMessage("kill ${dummyPlayer.name()}")

        // 4. killall
        serverCommand.handleMessage("killall")
        serverCommand.handleMessage("killall sharded")

        // 5. killunit
        serverCommand.handleMessage("killunit dagger")
        serverCommand.handleMessage("killunit dagger 1 sharded")

        // 6. mute
        serverCommand.handleMessage("mute ${dummyPlayer.name()}")

        // 7. unmute
        serverCommand.handleMessage("unmute ${dummyPlayer.name()}")

        // 8. setperm
        serverCommand.handleMessage("setperm ${dummyPlayer.name()} admin")

        // 9. strict
        serverCommand.handleMessage("strict ${dummyPlayer.name()}")
        serverCommand.handleMessage("strict ${dummyPlayer.name()}") // Toggle back

        // 10. team
        serverCommand.handleMessage("team sharded ${dummyPlayer.name()}")

        // 11. tempban
        serverCommand.handleMessage("tempban ${dummyPlayer.name()} 10")

        // 12. gen
        serverCommand.handleMessage("gen")

        // 13. reload
        serverCommand.handleMessage("reload")

        // 14. debug
        serverCommand.handleMessage("debug")

        leavePlayer(dummyPlayer)
    }

    @Test
    fun testClientCommands() {
        val p = newPlayer()
        val dummyPlayer = p.first
        val dummyData = p.second

        // Make the dummy player owner and admin
        setPermission(dummyPlayer, "owner", true)

        // 1. nextmap
        clientCommand.handleMessage("/nextmap", dummyPlayer)
        clientCommand.handleMessage("/nextmap 0", dummyPlayer)

        // 2. track
        clientCommand.handleMessage("/track", dummyPlayer)
        clientCommand.handleMessage("/track", dummyPlayer) // Toggle back

        // 3. votekick
        clientCommand.handleMessage("/votekick #1", dummyPlayer)

        // 4. fillitems
        clientCommand.handleMessage("/fillitems sharded", dummyPlayer)

        // 5. fuck
        clientCommand.handleMessage("/fuck checkmap", dummyPlayer)

        // 6. js
        clientCommand.handleMessage("/js 1+1", dummyPlayer)

        // 7. setitem
        clientCommand.handleMessage("/setitem copper 10", dummyPlayer)

        // 8. setperm
        clientCommand.handleMessage("/setperm ${dummyPlayer.name()} admin", dummyPlayer)

        // 9. spawn
        clientCommand.handleMessage("/spawn dagger", dummyPlayer)

        // 10. vote
        clientCommand.handleMessage("/vote kick ${dummyPlayer.name()}", dummyPlayer)

        // 11. weather
        clientCommand.handleMessage("/weather sandstorm 10", dummyPlayer)

        // 12. ws
        clientCommand.handleMessage("/ws count", dummyPlayer)

        leavePlayer(dummyPlayer)
    }

    @Test
    fun testEventsNatural() {
        val p = newPlayer()
        val dummyPlayer = p.first

        // 1. WaveEvent - Triggered naturally via Logic.runWave()
        Vars.logic.runWave()

        // 2. ConfigEvent - Triggered naturally via Call.tileConfig()
        val tile = randomTile()
        tile.setBlock(Blocks.message, dummyPlayer.team())
        Call.tileConfig(dummyPlayer, tile.build, "hello")

        // 3. DepositEvent & 4. WithdrawEvent - Triggered naturally with player at core
        val core = dummyPlayer.team().core()
        if (core != null) {
            dummyPlayer.set(core.x * 8f, core.y * 8f)
            dummyPlayer.unit().set(core.x * 8f, core.y * 8f)

            // Deposit
            dummyPlayer.unit().stack.set(Items.copper, 10)
            mindustry.input.InputHandler.transferInventory(dummyPlayer, core)

            // Withdraw
            core.items.add(Items.copper, 10)
            mindustry.input.InputHandler.requestItem(dummyPlayer, core, Items.copper, 5)
        }

        // 5. BlockDestroyEvent - Triggered naturally via tile.build.kill()
        val tileDest = randomTile()
        tileDest.setBlock(Blocks.mechanicalDrill, dummyPlayer.team())
        tileDest.build.kill()

        // 6. BuildingBulletDestroyEvent - Triggered naturally via tile.build.collision(bullet)
        val tileBullet = randomTile()
        tileBullet.setBlock(Blocks.mechanicalDrill, dummyPlayer.team())
        val bullet = mindustry.gen.Bullet.create()
        bullet.type = Bullets.placeholder
        bullet.team = Team.crux
        tileBullet.build.health = 1f
        tileBullet.build.collision(bullet)

        // 7. BuildSelectEvent - Triggered naturally via unit build plans (breaking = true)
        val tileBuild = randomTile()
        tileBuild.setBlock(Blocks.mechanicalDrill, dummyPlayer.team())
        val plan = BuildPlan(tileBuild.x.toInt(), tileBuild.y.toInt(), 0, Blocks.mechanicalDrill)
        plan.breaking = true
        dummyPlayer.unit().plans.addFirst(plan)
        dummyPlayer.unit().set(tileBuild.drawx(), tileBuild.drawy())
        mindustry.world.Build.beginBreak(dummyPlayer.unit(), dummyPlayer.team(), tileBuild.x.toInt(), tileBuild.y.toInt())
        updateTick(5)

        leavePlayer(dummyPlayer)
    }

    @Test
    fun testEventsDirect() {
        val p = newPlayer()
        val dummyPlayer = p.first

        // Associate the IP with the dummy player's info so findByIP works
        val info = Vars.netServer.admins.getInfo(dummyPlayer.uuid())
        info.ips.add("127.0.0.1")

        // Directly fire events that are generally not possible to trigger via game interactions in headless tests
        Events.fire(PlayerBanEvent(dummyPlayer, dummyPlayer.uuid()))
        Events.fire(PlayerUnbanEvent(dummyPlayer, dummyPlayer.uuid()))
        Events.fire(PlayerIpUnbanEvent("127.0.0.1"))
        Events.fire(PlayerConnect(dummyPlayer))
        Events.fire(UnitCreateEvent(dummyPlayer.unit(), null))

        // Directly fire additional events to guarantee coverage in all test environments
        val tile = randomTile()
        tile.setBlock(Blocks.mechanicalDrill, dummyPlayer.team())
        val build = tile.build
        if (build != null) {
            Events.fire(DepositEvent(build, dummyPlayer, Items.copper, 10))
            Events.fire(WithdrawEvent(build, dummyPlayer, Items.copper, 5))
            Events.fire(BuildingBulletDestroyEvent(build, mindustry.gen.Bullet.create()))
        }
        Events.fire(BuildSelectEvent(tile, dummyPlayer.team(), dummyPlayer.unit(), true))
        Events.fire(BlockDestroyEvent(tile))

        leavePlayer(dummyPlayer)
    }
}
