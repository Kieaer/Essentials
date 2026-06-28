package essential.core.service.contribution

import arc.util.Timer
import essential.common.database.data.PlayerData
import essential.common.database.data.insertContribution
import essential.common.offlinePlayers
import essential.common.players
import essential.common.util.findPlayerData
import essential.core.Main.Companion.scope
import essential.core.service.contribution.ContributionService.Companion.conf
import kotlinx.coroutines.launch
import ksp.event.Event
import mindustry.Vars
import mindustry.ai.types.CommandAI
import mindustry.content.Items
import mindustry.game.EventType.*
import mindustry.gen.Building
import mindustry.gen.Groups
import mindustry.gen.Unit
import mindustry.type.Item
import mindustry.world.blocks.production.Drill
import mindustry.world.blocks.production.GenericCrafter

// --- Per-game scratch state (cleared on WorldLoadEvent) ---

/** Tile position (Building.pos()) -> owner uuid. Who placed the building. */
private val tileOwner = mutableMapOf<Int, String>()

/** Unit id -> uuid of the player whose factory produced it. */
private val unitProducer = mutableMapOf<Int, String>()

/** Unit id -> uuid of the player currently/last controlling it (direct possession). */
private val unitController = mutableMapOf<Int, String>()

/** Building positions already scored for the one-time factory-build bonus. */
private val scoredFactories = mutableSetOf<Int>()

private var timerScheduled = false

private fun resetGameState() {
    tileOwner.clear()
    unitProducer.clear()
    unitController.clear()
    scoredFactories.clear()
    for (data in players) data.currentContribution = 0.0
    for (data in offlinePlayers) data.currentContribution = 0.0
}

/** Resolve a uuid to its (online or recently-offline) PlayerData. */
private fun ownerData(uuid: String?): PlayerData? {
    if (uuid == null) return null
    return findPlayerData(uuid) ?: offlinePlayers.find { it.uuid == uuid }
}

private fun addScore(uuid: String?, amount: Double) {
    if (amount == 0.0) return
    ownerData(uuid)?.let { it.currentContribution += amount }
}

/** Sum of a block's item build cost. */
private fun resourceCost(block: mindustry.world.Block): Int {
    var sum = 0
    block.requirements?.forEach { sum += it.amount }
    return sum
}

/** Sum of a unit type's build cost. */
private fun unitCost(type: mindustry.type.UnitType): Int {
    var sum = 0
    type.getTotalRequirements()?.forEach { sum += it.amount }
    return sum
}

private fun isPenaltyExempt(blockName: String): Boolean =
    conf.resourcePenaltyExempt.any { blockName.contains(it) }

/** Stored amount of [item] in the team's first core, 0 if none. */
private fun coreItem(building: Building, item: Item): Int {
    val core = building.team().data().core() ?: return 0
    return core.items?.get(item) ?: 0
}

/** Mining multiplier: 1.0 until the team core holds [coreThreshold] of both copper and lead, then reduced. */
private fun miningMultiplier(building: Building): Double {
    val reached = coreItem(building, Items.copper) >= conf.coreThreshold &&
            coreItem(building, Items.lead) >= conf.coreThreshold
    return if (reached) conf.postThresholdMultiplier else 1.0
}

// --- Event handlers ---

@Event
fun worldLoad(event: WorldLoadEvent) {
    if (!conf.enabled) return
    resetGameState()
}

@Event
fun blockBuildEnd(event: BlockBuildEndEvent) {
    if (!conf.enabled) return
    val unit = event.unit ?: return
    if (!unit.isPlayer) return
    val player = unit.player ?: return
    val tile = event.tile ?: return
    val block = tile.block() ?: return
    val pos = tile.build?.pos() ?: tile.pos()

    if (!event.breaking) {
        // Record ownership.
        tileOwner[pos] = player.uuid()

        // First-build factory bonus.
        val factoryScore = conf.factoryBuildScore[block.name]
        if (factoryScore != null && scoredFactories.add(pos)) {
            addScore(player.uuid(), factoryScore.toDouble())
        }

        // Build resource penalty (skip exempt blocks: conveyors, walls, turrets).
        if (!Vars.state.rules.infiniteResources && !isPenaltyExempt(block.name)) {
            addScore(player.uuid(), -resourceCost(block) * conf.buildPenaltyMultiplier)
        }
    } else {
        // Self-deconstruction of a resource producer: subtract its per-second output value.
        val owner = player.uuid()
        val build = tile.build
        if (build != null && !isPenaltyExempt(block.name)) {
            val perSec = estimateOutputPerSecond(build)
            if (perSec > 0.0) {
                addScore(owner, -perSec * miningMultiplier(build))
            }
        }
        tileOwner.remove(pos)
        scoredFactories.remove(pos)
    }
}

@Event
fun unitCreate(event: UnitCreateEvent) {
    if (!conf.enabled) return
    val spawner = event.spawner ?: return
    val producer = tileOwner[spawner.pos()] ?: return
    unitProducer[event.unit.id()] = producer
}

@Event
fun unitControl(event: UnitControlEvent) {
    if (!conf.enabled) return
    // Player took direct control of a unit; remember the controller.
    unitController[event.unit.id()] = event.player.uuid()
}

@Event
fun buildDamage(event: BuildDamageEvent) {
    if (!conf.enabled) return
    val bullet = event.source ?: return
    val owner = bullet.owner as? Unit ?: return
    val building = event.build ?: return

    // Only score damage dealt to enemy buildings.
    if (building.team() == owner.team()) return

    // Value of the damage: scaled by the building's resource cost relative to its max health.
    val maxHp = building.maxHealth()
    if (maxHp <= 0f) return
    val value = (event.source.damage() / maxHp) * resourceCost(building.block)
    if (value <= 0.0) return

    // Identify the controlling player.
    val attacker: String? = when {
        owner.isPlayer -> owner.player?.uuid()
        owner.controller() is CommandAI -> unitController[owner.id()] // best-effort: direct-control history
        else -> null
    }

    addScore(attacker, value.toDouble())

    // Reward the unit's producer with 50% (1.5x total when attacker == producer).
    val producer = unitProducer[owner.id()]
    addScore(producer, value.toDouble() * 0.5)
}

@Event
fun unitDestroy(event: UnitDestroyEvent) {
    if (!conf.enabled) return
    val id = event.unit.id()
    // A controlled unit died: penalize its controller by its production value (resource cost).
    val controller = unitController[id]
    if (controller != null) {
        addScore(controller, -unitCost(event.unit.type()).toDouble())
    }
    unitController.remove(id)
    unitProducer.remove(id)
}

@Event
fun gameOver(event: GameOverEvent) {
    if (!conf.enabled) return
    val mode = when {
        Vars.state.rules.pvp -> "pvp"
        Vars.state.rules.attackMode -> "attack"
        else -> "survival"
    }
    val mapName = Vars.state.map?.plainName()
    val snapshot = (players + offlinePlayers).distinctBy { it.uuid }
    for (data in snapshot) {
        val score = data.currentContribution
        scope.launch {
            try {
                insertContribution(data, mode, mapName, score)
            } catch (_: Throwable) {
            }
        }
    }
    resetGameState()
}

/**
 * No-arg registration hook (called once by the generated registrar). Schedules the
 * per-second polling loop for mining, item production and power generation.
 */
@Event
fun startSecondTimer() {
    if (timerScheduled) return
    timerScheduled = true
    Timer.schedule({
        if (!conf.enabled || !Vars.state.isPlaying) return@schedule
        pollProduction()
    }, 0f, 1f)
}

/** Estimate ores/items produced per second by a drill or crafter building. */
private fun estimateOutputPerSecond(build: Building): Double {
    when (build) {
        is Drill.DrillBuild -> {
            val drill = build.block as? Drill ?: return 0.0
            val item = build.dominantItem ?: return 0.0
            if (item == Items.coal) return 0.0
            val drillTime = drill.getDrillTime(item)
            if (drillTime <= 0f) return 0.0
            // dominantItems ore tiles produce that many items per drill cycle.
            return (build.dominantItems * 60.0 / drillTime) * build.warmup
        }
        is GenericCrafter.GenericCrafterBuild -> {
            val crafter = build.block as? GenericCrafter ?: return 0.0
            val outputs = crafter.outputItems ?: return 0.0
            if (crafter.craftTime <= 0f) return 0.0
            var total = 0.0
            for (stack in outputs) {
                total += stack.amount * 60.0 / crafter.craftTime
            }
            return total * build.warmup
        }
        else -> return 0.0
    }
}

private fun pollProduction() {
    Groups.build.forEach { build ->
        val owner = tileOwner[build.pos()]

        // Mining (drills).
        if (build is Drill.DrillBuild) {
            val item = build.dominantItem
            if (item != null && item != Items.coal) {
                val perSec = estimateOutputPerSecond(build)
                if (perSec > 0.0) {
                    addScore(owner, perSec * conf.miningPerOre * miningMultiplier(build))
                }
            }
        }

        // Item production (crafters).
        if (build is GenericCrafter.GenericCrafterBuild) {
            val crafter = build.block as? GenericCrafter
            val outputs = crafter?.outputItems
            if (crafter != null && outputs != null && crafter.craftTime > 0f) {
                for (stack in outputs) {
                    val perSec = stack.amount * 60.0 / crafter.craftTime * build.warmup
                    if (perSec <= 0.0) continue
                    val score = itemScore(stack.item, build) * perSec
                    addScore(owner, score)
                }
            }
        }

        // Power generation.
        if (build.block.outputsPower) {
            val prodPerTick = build.getPowerProduction()
            if (prodPerTick > 0f) {
                addScore(owner, prodPerTick * 60.0 * conf.powerScoreRatio)
            }
        }
    }
}

/** Score per produced item; titanium switches on the team core titanium threshold. */
private fun itemScore(item: Item, building: Building): Double {
    if (item == Items.titanium) {
        return if (coreItem(building, Items.titanium) >= conf.titaniumThreshold)
            conf.titaniumScoreAfterThreshold.toDouble()
        else
            conf.titaniumScoreBeforeThreshold.toDouble()
    }
    return (conf.itemProduceScore[item.name] ?: 0).toDouble()
}
