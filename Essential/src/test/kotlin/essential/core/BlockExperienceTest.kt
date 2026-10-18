package essential.core

import PluginTest.Companion.leavePlayer
import PluginTest.Companion.loadGame
import PluginTest.Companion.newPlayer
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.content.UnitTypes
import mindustry.world.Build
import mindustry.world.blocks.ConstructBlock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BlockExperienceTest {
    @Test
    fun deconstructionDeductsOriginalBlockExperienceAcrossSizes() {
        verifyDeconstruction(infiniteResources = false)
    }

    @Test
    fun deconstructionWithInfiniteResourcesDoesNotDeductExperience() {
        verifyDeconstruction(infiniteResources = true)
    }

    private fun verifyDeconstruction(infiniteResources: Boolean) {
        loadGame(true)
        val (player, data) = newPlayer()
        val unit = UnitTypes.nova.spawn(player.team(), player.x, player.y).also {
            it.controller(player)
            player.unit(it)
        }
        val tile = Vars.world.tile(10, 10)
        val originalInfiniteResources = Vars.state.rules.infiniteResources
        val originalBlock = tile.block()
        val originalTeam = tile.team()
        val originalRotation = tile.build?.rotation ?: 0

        try {
            Vars.state.rules.infiniteResources = infiniteResources
            for (block in listOf(Blocks.duo, Blocks.mechanicalDrill, Blocks.thoriumReactor)) {
                val cost = block.requirements.sumOf { it.amount }
                assertTrue(cost > 0)
                data.exp = 0
                data.currentExp = 0
                val previousBreakCount = data.blockBreakCount

                tile.setBlock(block, player.team(), 0)
                Build.beginBreak(unit, player.team(), tile.x.toInt(), tile.y.toInt())
                val construction = assertIs<ConstructBlock.ConstructBuild>(tile.build)
                assertEquals(block, construction.current)
                assertEquals("build${block.size}", tile.block().name)

                ConstructBlock.deconstructFinish(tile, block, unit)

                val expectedChange = if (infiniteResources) 0 else -cost
                assertEquals(expectedChange, data.exp, "Total experience after deconstructing ${block.name}")
                assertEquals(expectedChange, data.currentExp, "Round experience after deconstructing ${block.name}")
                assertEquals(previousBreakCount + if (infiniteResources) 0 else 1, data.blockBreakCount)
                assertEquals(Blocks.air, tile.block())
            }
        } finally {
            Vars.state.rules.infiniteResources = originalInfiniteResources
            tile.setBlock(originalBlock, originalTeam, originalRotation)
            leavePlayer(player)
            unit.remove()
        }
    }
}
