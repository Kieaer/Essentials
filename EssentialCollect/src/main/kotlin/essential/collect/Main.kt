package essential.collect

import arc.Events
import arc.files.Fi
import arc.util.Log
import arc.util.serialization.Json
import essential.bundle.Bundle
import essential.rootPath
import mindustry.Vars.state
import mindustry.content.Planets
import mindustry.game.EventType.*
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.mod.Plugin
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.ArrayNode
import java.io.BufferedWriter
import java.io.FileWriter

class Main : Plugin() {
    private var recordFile: Fi? = null
    private var jsonWriter: BufferedWriter? = null
    private var currentStatus = CurrentStatus("sandbox", "andromeda")
    private var isFirstWrite = true
    private val mapper = ObjectMapper()

    private data class CurrentStatus(
        val mode: String,
        val planet: String,
    )

    override fun init() {
        bundle.prefix = "[EssentialCollect]"
        Log.debug(bundle["event.plugin.starting"])
        rootPath.child("collect").mkdirs()
        setEvents()
        Log.debug(bundle["event.plugin.loaded"])
    }

    /**
     * Writes an ObjectNode directly to the record file using a BufferedWriter.
     * Creates the file if it doesn't exist, and appends to it if it does.
     * This approach is more memory-efficient for large datasets.
     */
    private fun writeToFile(jsonObject: ObjectNode) {
        try {
            if (recordFile == null) {
                return
            }

            // Ensure the writer is initialized
            if (jsonWriter == null) {
                jsonWriter = BufferedWriter(FileWriter(recordFile!!.file(), true))
            }

            // For the first write, write the opening bracket of the JSON array
            if (isFirstWrite) {
                jsonWriter?.write("[")
                isFirstWrite = false
            } else {
                // For subsequent writes, add a comma before the new object
                jsonWriter?.write(",")
            }

            // Write the JSON object and flush immediately to ensure data is written to disk
            jsonWriter?.write(jsonObject.toString())
            jsonWriter?.flush()
        } catch (ex: Exception) {
            Log.err("Failed to write to record file", ex)
        }
    }

    private fun setCurrentStatus() {
        val gameMode = when {
            state.rules.pvp -> "pvp"
            state.rules.attackMode -> "attack"
            state.rules.waves -> "wave"
            else -> "sandbox"
        }
        val gameType = when (state.rules.planet) {
            Planets.serpulo -> "serpulo"
            Planets.erekir -> "erekir"
            Planets.sun -> "sun"
            Planets.gier -> "gier"
            Planets.notva -> "notva"
            Planets.tantros -> "tantros"
            Planets.verilus -> "verilus"
            else -> "andromeda"
        }
        currentStatus = CurrentStatus(gameMode, gameType)
    }

    private fun getPlayerStatus(player: Player): ObjectNode {
        val json = mapper.createObjectNode()
        if (player.unit() != null && !player.unit().dead) {
            json.put("unit_x", player.unit().x)
            json.put("unit_y", player.unit().y)
            json.put("aim_x", player.unit().aimX)
            json.put("aim_y", player.unit().aimY)
            val buildPlan = mapper.createArrayNode()
            for (plan in player.unit().plans) {
                val data = mapper.createObjectNode()
                data.put("block_name", plan.block.name)
                data.put("tile_x", plan.tile().x.toInt())
                data.put("tile_y", plan.tile().y.toInt())
                data.put("rotation", plan.rotation)
                buildPlan.add(data)
            }
            json.set<ArrayNode>("build_plan", buildPlan)
            json.put("team", player.unit().team.name)
            json.put("item_name", player.unit().stack.item.name)
            json.put("item_amount", player.unit().stack.amount)
            json.put("rotation", player.unit().rotation)
            json.put("health", player.unit().health())
            json.put("shield", player.unit().shield())
        }
        json.put("mouse_x", player.mouseX)
        json.put("mouse_y", player.mouseY)
        json.put("name", player.name)

        return json
    }

    /**
     * Sets up event handlers to record player activities and save them to a JSON file.
     */
    private fun setEvents() {
        Events.on(PlayEvent::class.java) { _ ->
            setCurrentStatus()
            // Create a new file for this game session
            recordFile = rootPath.child("collect/${System.currentTimeMillis()}_${currentStatus.planet}_${currentStatus.mode}.json")
            // Close previous writer if it exists
            jsonWriter?.close()
            jsonWriter = null
            isFirstWrite = true

            val mapLoad = mapper.createObjectNode()
            mapLoad.put("type", "map_load")
            mapLoad.put("map", state.map.name())
            mapLoad.put("mode", currentStatus.mode)
            mapLoad.put("planet", currentStatus.planet)
            mapLoad.put("time", System.currentTimeMillis())
            writeToFile(mapLoad)
        }

        Events.on(PlayerJoin::class.java) { e ->
            val playerJoin = mapper.createObjectNode()
            playerJoin.put("type", "player_join")
            playerJoin.set<ObjectNode>("player", getPlayerStatus(e.player))
            playerJoin.put("time", System.currentTimeMillis())
            writeToFile(playerJoin)
        }

        Events.on(PlayerLeave::class.java) { e ->
            val playerLeave = mapper.createObjectNode()
            playerLeave.put("type", "player_leave")
            playerLeave.set<ObjectNode>("player", getPlayerStatus(e.player))
            playerLeave.put("time", System.currentTimeMillis())
            writeToFile(playerLeave)
        }

        Events.on(WithdrawEvent::class.java) { e ->
            val withdraw = mapper.createObjectNode()
            withdraw.put("type", "withdraw")
            withdraw.set<ObjectNode>("player", getPlayerStatus(e.player))
            withdraw.put("item", e.item.name)
            withdraw.put("block_name", e.tile.block.name)
            withdraw.put("amount", e.amount)
            withdraw.put("time", System.currentTimeMillis())
            writeToFile(withdraw)
        }

        Events.on(DepositEvent::class.java) { e ->
            val deposit = mapper.createObjectNode()
            deposit.put("type", "deposit")
            deposit.set<ObjectNode>("player", getPlayerStatus(e.player))
            deposit.put("item", e.item.name)
            deposit.put("amount", e.amount)
            deposit.put("block_name", e.tile.block.name)
            deposit.put("time", System.currentTimeMillis())
            writeToFile(deposit)
        }

        Events.on(UnitDestroyEvent::class.java) { e ->
            val unitDestroy = mapper.createObjectNode()
            unitDestroy.put("type", "unit_destroy")
            unitDestroy.put("unit", e.unit.type.name)
            unitDestroy.put("team", e.unit.team.name)
            unitDestroy.put("time", System.currentTimeMillis())
            writeToFile(unitDestroy)
        }

        Events.on(ConfigEvent::class.java) { e ->
            val config = mapper.createObjectNode()
            config.put("type", "config")
            if (e.player != null) {
                config.set<ObjectNode>("player", getPlayerStatus(e.player))
            }
            config.put("config", Json().toJson(e.value))
            config.put("block", e.tile.block.name)
            config.put("time", System.currentTimeMillis())
            writeToFile(config)
        }

        Events.on(GameOverEvent::class.java) { e ->
            try {
                // Write the closing bracket of the JSON array
                if (jsonWriter != null && !isFirstWrite) {
                    jsonWriter?.write("]")
                    jsonWriter?.flush()
                    jsonWriter?.close()
                    jsonWriter = null
                }
                // Reset for next game
                recordFile = null
            } catch (ex: Exception) {
                Log.err("Failed to close record file", ex)
            }
        }

        Events.on(TapEvent::class.java) { e ->
            val tap = mapper.createObjectNode()
            tap.set<ObjectNode>("player", getPlayerStatus(e.player))
            tap.put("type", "tap")
            tap.put("tile_x", e.tile.x.toInt())
            tap.put("tile_y", e.tile.y.toInt())
            tap.put("time", System.currentTimeMillis())
            writeToFile(tap)
        }

        Events.on(PickupEvent::class.java) { e ->
            if (e.carrier != null && e.carrier.isPlayer) {
                val pickup = mapper.createObjectNode()
                val p = e.carrier.player
                if (e.unit != null) {
                    pickup.put("unit_name", e.unit.type.name)
                }
                if (e.build != null) {
                    pickup.put("block_name", e.build.block.name)
                }

                pickup.put("type", "pickup")
                pickup.set<ObjectNode>("player", getPlayerStatus(p))
                pickup.put("time", System.currentTimeMillis())
                writeToFile(pickup)
            }
        }

        Events.on(UnitCreateEvent::class.java) { e ->
            val unitCreate = mapper.createObjectNode()
            unitCreate.put("type", "unit_create")
            unitCreate.put("unit", e.unit.type.name)
            unitCreate.put("team", e.unit.team.toString())
            unitCreate.put("time", System.currentTimeMillis())
            writeToFile(unitCreate)
        }

        Events.on(UnitControlEvent::class.java) { e ->
            val unitControl = mapper.createObjectNode()
            unitControl.put("type", "unit_control")
            unitControl.set<ObjectNode>("player", getPlayerStatus(e.player))
            if(e.unit.isCommandable) {
                unitControl.put("target_x", e.unit.command().targetPos.x)
                unitControl.put("target_y", e.unit.command().targetPos.y)
            }
            unitControl.put("unit", e.unit.type.name)
            unitControl.put("time", System.currentTimeMillis())
            writeToFile(unitControl)
        }

        Events.on(BlockBuildBeginEvent::class.java) { e ->
            val blockBuildBegin = mapper.createObjectNode()
            blockBuildBegin.put("type", "block_build_begin")

            if (e!!.unit != null && e.unit.isPlayer) {
                blockBuildBegin.set<ObjectNode>("player", getPlayerStatus(e.unit.player))
            }

            blockBuildBegin.put("block", e.tile.block().name)
            blockBuildBegin.put("tile_x", e.tile.x.toInt())
            blockBuildBegin.put("tile_y", e.tile.y.toInt())
            blockBuildBegin.put("is_breaking", e.breaking)
            blockBuildBegin.put("time", System.currentTimeMillis())
            writeToFile(blockBuildBegin)
        }

        Events.on(BlockBuildEndEvent::class.java) { e: BlockBuildEndEvent? ->
            val blockBuildEnd = mapper.createObjectNode()
            blockBuildEnd.put("type", "block_build_end")

            if (e!!.unit != null && e.unit.isPlayer) {
                blockBuildEnd.set<ObjectNode>("player", getPlayerStatus(e.unit.player))
            }

            blockBuildEnd.put("block", e.tile.block().name)
            blockBuildEnd.put("tile_x", e.tile.x.toInt())
            blockBuildEnd.put("tile_y", e.tile.y.toInt())
            blockBuildEnd.put("is_breaking", e.breaking)
            blockBuildEnd.put("time", System.currentTimeMillis())
            writeToFile(blockBuildEnd)
        }

        // if player or unit rotated the target block
        Events.on(BuildRotateEvent::class.java) { e ->
            val buildRotate = mapper.createObjectNode()
            buildRotate.put("type", "build_rotate")
            if (e.unit != null && e.unit.isPlayer) {
                buildRotate.set<ObjectNode>("player", getPlayerStatus(e.unit.player))
                val buildPlan = mapper.createArrayNode()
                for (plan in e.unit.plans) {
                    val data = mapper.createObjectNode()
                    data.put("block_name", plan.block.name)
                    data.put("tile_x", plan.tile().x.toInt())
                    data.put("tile_y", plan.tile().y.toInt())
                    data.put("rotation", plan.rotation)
                    buildPlan.add(data)
                }
            }
            buildRotate.put("block", e.build.block.name)
            buildRotate.put("previous", e.previous)
            buildRotate.put("current", e.build.rotation)
            buildRotate.put("time", System.currentTimeMillis())
            writeToFile(buildRotate)
        }

        // if player or unit is block remove or selected
        Events.on(BuildSelectEvent::class.java) { e ->
            val buildSelect = mapper.createObjectNode()
            buildSelect.put("type", "build_select")
            buildSelect.put("name", e.tile.block().name)
            buildSelect.put("is_breaking", e.breaking)
            buildSelect.put("tile_x", e.tile.x.toInt())
            buildSelect.put("tile_y", e.tile.y.toInt())
            if (e.builder.isPlayer) {
                val buildPlan = mapper.createArrayNode()
                for (plan in e.builder.plans) {
                    val data = mapper.createObjectNode()
                    data.put("block_name", plan.block.name)
                    data.put("tile_x", plan.tile().x.toInt())
                    data.put("tile_y", plan.tile().y.toInt())
                    data.put("rotation", plan.rotation)
                    buildPlan.add(data)
                }
            }
            buildSelect.put("time", System.currentTimeMillis())
            writeToFile(buildSelect)
        }

        // if power generator exploded by pressure
        Events.on(GeneratorPressureExplodeEvent::class.java) { e: GeneratorPressureExplodeEvent? -> }

        // if building destroyed by bullet
        Events.on(BuildingBulletDestroyEvent::class.java) { e ->
            val buildingDestroy = mapper.createObjectNode()
            buildingDestroy.put("type", "bullet_destroy")
            buildingDestroy.put("build_name", e.build.block.name)
            buildingDestroy.put("health", e.build.health())
            buildingDestroy.put("destroyed", e.build.dead)
            buildingDestroy.put("team", e.build.team.name)
            buildingDestroy.put("bullet_team", e.bullet.team.name)
            buildingDestroy.put("bullet_unit", e.bullet.owner is mindustry.gen.Unit)
            buildingDestroy.put("bullet_build", e.bullet.owner is mindustry.world.Block)
            buildingDestroy.put("time", System.currentTimeMillis())
            writeToFile(buildingDestroy)
        }

        // if unit destroyed by bullet
        Events.on(UnitBulletDestroyEvent::class.java) { e ->
            val bulletDestroy = mapper.createObjectNode()
            bulletDestroy.put("type", "bullet_destroy")
            bulletDestroy.put("unit_name", e.unit.type.name)
            bulletDestroy.put("health", e.unit.health())
            bulletDestroy.put("is_dead", e.unit.dead)
            bulletDestroy.put("team", e.unit.team.name)
            bulletDestroy.put("bullet_team", e.bullet.team.name)
            bulletDestroy.put("bullet_unit", e.bullet.owner is mindustry.gen.Unit)
            bulletDestroy.put("bullet_build", e.bullet.owner is mindustry.world.Block)
            bulletDestroy.put("time", System.currentTimeMillis())
            writeToFile(bulletDestroy)
        }

        // if unit taken damage
        Events.on(UnitDamageEvent::class.java) { e ->
            val damaged = mapper.createObjectNode()
            damaged.put("type", "bullet_destroy")
            damaged.put("unit_name", e.unit.type.name)
            damaged.put("health", e.unit.health())
            damaged.put("is_dead", e.unit.dead)
            damaged.put("team", e.unit.team.name)
            damaged.put("bullet_team", e.bullet.team.name)
            damaged.put("bullet_unit", e.bullet.owner is mindustry.gen.Unit)
            damaged.put("bullet_build", e.bullet.owner is mindustry.world.Block)
            damaged.put("time", System.currentTimeMillis())
            writeToFile(damaged)
        }

        // if unit is drowned
        Events.on(UnitDrownEvent::class.java) { e: UnitDrownEvent? ->
            val drown = mapper.createObjectNode()
            drown.put("type", "unit_drown")
            drown.put("unit_name", e!!.unit.type.name)
            drown.put("health", e.unit.health())
            drown.put("is_dead", e.unit.dead)
            drown.put("team", e.unit.team.name)
            drown.put("time", System.currentTimeMillis())
            writeToFile(drown)
        }

        var tick = 0
        Events.on(Trigger.update::class.java) {
            if (tick == 60) {
                val current = mapper.createObjectNode()
                current.put("type", "current_status")
                current.put("time", System.currentTimeMillis())

                val list = mapper.createArrayNode()

                Groups.unit.forEach {
                    val currentStatus = mapper.createObjectNode()
                    currentStatus.put("unit_name", it.type.name)
                    currentStatus.put("team", it.team.name)
                    currentStatus.put("unit_x", it.x)
                    currentStatus.put("unit_y", it.y)
                    currentStatus.put("aim_x", it.aimX)
                    currentStatus.put("aim_y", it.aimY)
                    currentStatus.put("health", it.health())
                    currentStatus.put("shield", it.shield())
                    list.add(currentStatus)
                }

                current.set<ArrayNode>("units", list)

                writeToFile(current)
                tick = 0
            } else {
                tick++
            }
        }
    }

    companion object {
        var bundle: Bundle = Bundle()
    }
}
