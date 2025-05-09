package essential.collect

import arc.Events
import arc.files.Fi
import arc.util.Log
import arc.util.serialization.Json
import essential.core.Bundle
import essential.core.Main.Companion.root
import mindustry.Vars.state
import mindustry.content.Planets
import mindustry.game.EventType.*
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.mod.Plugin
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.Stringify

class Main : Plugin() {
    private var recordFile: Fi? = null
    private var currentStatus = CurrentStatus("sandbox", "andromeda")
    private var isFirstWrite = true

    private data class CurrentStatus(
        val mode: String,
        val planet: String,
    )

    override fun init() {
        bundle.prefix = "[EssentialCollect]"
        Log.debug(bundle["event.plugin.starting"])
        root.child("collect").mkdirs()
        setEvents()
        Log.debug(bundle["event.plugin.loaded"])
    }

    /**
     * Writes a JsonObject directly to the record file.
     * Creates the file if it doesn't exist, and appends to it if it does.
     */
    private fun writeToFile(jsonObject: JsonObject) {
        try {
            if (recordFile == null) {
                return
            }

            // For the first write, write the opening bracket of the JSON array
            if (isFirstWrite) {
                recordFile?.writeString("[", false)
                isFirstWrite = false
            } else {
                // For subsequent writes, add a comma before the new object
                recordFile?.writeString(",", false)
            }

            // Write the JSON object
            recordFile?.writeString(jsonObject.toString(Stringify.PLAIN), false)
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

    private fun getPlayerStatus(player: Player) : JsonObject {
        val json = JsonObject()
        if (player.unit() != null && !player.unit().dead) {
            json.add("unit_x", player.unit().x)
            json.add("unit_y", player.unit().y)
            json.add("aim_x", player.unit().aimX)
            json.add("aim_y", player.unit().aimY)
            val buildPlan = JsonArray()
            for (plan in player.unit().plans) {
                val data = JsonObject()
                data.add("block_name", plan.block.name)
                data.add("tile_x", plan.tile().x.toInt())
                data.add("tile_y", plan.tile().y.toInt())
                data.add("rotation", plan.rotation)
                buildPlan.add(data)
            }
            json.add("build_plan", buildPlan)
            json.add("team", player.unit().team.name)
            json.add("item_name", player.unit().stack.item.name)
            json.add("item_amount", player.unit().stack.amount)
            json.add("rotation", player.unit().rotation)
            json.add("health", player.unit().health())
            json.add("shield", player.unit().shield())
        }
        json.add("mouse_x", player.mouseX)
        json.add("mouse_y", player.mouseY)
        json.add("name", player.name)

        return json
    }

    /**
     * Sets up event handlers to record player activities and save them to a JSON file.
     */
    private fun setEvents() {
        Events.on(PlayEvent::class.java) { _ ->
            setCurrentStatus()
            // Create a new file for this game session
            recordFile = root.child("collect/${System.currentTimeMillis()}_${currentStatus.planet}_${currentStatus.mode}.json")
            isFirstWrite = true

            val mapLoad = JsonObject()
            mapLoad.add("type", "map_load")
            mapLoad.add("map", state.map.name())
            mapLoad.add("mode", currentStatus.mode)
            mapLoad.add("planet", currentStatus.planet)
            mapLoad.add("time", System.currentTimeMillis())
            writeToFile(mapLoad)
        }

        Events.on(PlayerJoin::class.java) { e ->
            val playerJoin = JsonObject()
            playerJoin.add("type", "player_join")
            playerJoin.add("player", getPlayerStatus(e.player))
            playerJoin.add("time", System.currentTimeMillis())
            writeToFile(playerJoin)
        }

        Events.on(PlayerLeave::class.java) { e ->
            val playerLeave = JsonObject()
            playerLeave.add("type", "player_leave")
            playerLeave.add("player", getPlayerStatus(e.player))
            playerLeave.add("time", System.currentTimeMillis())
            writeToFile(playerLeave)
        }

        Events.on(WithdrawEvent::class.java) { e ->
            val withdraw = JsonObject()
            withdraw.add("type", "withdraw")
            withdraw.add("player", getPlayerStatus(e.player))
            withdraw.add("item", e.item.name)
            withdraw.add("block_name", e.tile.block.name)
            withdraw.add("amount", e.amount)
            withdraw.add("time", System.currentTimeMillis())
            writeToFile(withdraw)
        }

        Events.on(DepositEvent::class.java) { e ->
            val deposit = JsonObject()
            deposit.add("type", "deposit")
            deposit.add("player", getPlayerStatus(e.player))
            deposit.add("item", e.item.name)
            deposit.add("amount", e.amount)
            deposit.add("block_name", e.tile.block.name)
            deposit.add("time", System.currentTimeMillis())
            writeToFile(deposit)
        }

        Events.on(UnitDestroyEvent::class.java) { e ->
            val unitDestroy = JsonObject()
            unitDestroy.add("type", "unit_destroy")
            unitDestroy.add("unit", e.unit.type.name)
            unitDestroy.add("team", e.unit.team.name)
            unitDestroy.add("time", System.currentTimeMillis())
            writeToFile(unitDestroy)
        }

        Events.on(ConfigEvent::class.java) { e ->
            val config = JsonObject()
            config.add("type", "config")
            if (e.player != null) {
                config.add("player", getPlayerStatus(e.player))
            }
            config.add("config", Json().toJson(e.value))
            config.add("block", e.tile.block.name)
            config.add("time", System.currentTimeMillis())
            writeToFile(config)
        }

        Events.on(GameOverEvent::class.java) { e ->
            try {
                // Write the closing bracket of the JSON array
                if (recordFile != null && !isFirstWrite) {
                    recordFile?.writeString("]", false)
                }
                // Reset for next game
                recordFile = null
            } catch (ex: Exception) {
                Log.err("Failed to close record file", ex)
            }
        }

        Events.on(TapEvent::class.java) { e ->
            val tap = JsonObject()
            tap.add("player", getPlayerStatus(e.player))
            tap.add("type", "tap")
            tap.add("tile_x", e.tile.x.toInt())
            tap.add("tile_y", e.tile.y.toInt())
            tap.add("time", System.currentTimeMillis())
            writeToFile(tap)
        }

        Events.on(PickupEvent::class.java) { e ->
            if (e.carrier != null && e.carrier.isPlayer) {
                val pickup = JsonObject()
                val p = e.carrier.player
                if (e.unit != null) {
                    pickup.add("unit_name", e.unit.type.name)
                }
                if (e.build != null) {
                    pickup.add("block_name", e.build.block.name)
                }

                pickup.add("type", "pickup")
                pickup.add("player", getPlayerStatus(p))
                pickup.add("time", System.currentTimeMillis())
                writeToFile(pickup)
            }
        }

        Events.on(UnitCreateEvent::class.java) { e ->
            val unitCreate = JsonObject()
            unitCreate.add("type", "unit_create")
            unitCreate.add("unit", e.unit.type.name)
            unitCreate.add("team", e.unit.team.toString())
            unitCreate.add("time", System.currentTimeMillis())
            writeToFile(unitCreate)
        }

        Events.on(UnitControlEvent::class.java) { e ->
            val unitControl = JsonObject()
            unitControl.add("type", "unit_control")
            unitControl.add("player", getPlayerStatus(e.player))
            if(e.unit.isCommandable) {
                unitControl.add("target_x", e.unit.command().targetPos.x)
                unitControl.add("target_y", e.unit.command().targetPos.y)
            }
            unitControl.add("unit", e.unit.type.name)
            unitControl.add("time", System.currentTimeMillis())
            writeToFile(unitControl)
        }

        Events.on(BlockBuildBeginEvent::class.java) { e ->
            val blockBuildBegin = JsonObject()
            blockBuildBegin.add("type", "block_build_begin")

            if (e!!.unit != null && e.unit.isPlayer) {
                blockBuildBegin.add("player", getPlayerStatus(e.unit.player))
            }

            blockBuildBegin.add("block", e.tile.block().name)
            blockBuildBegin.add("tile_x", e.tile.x.toInt())
            blockBuildBegin.add("tile_y", e.tile.y.toInt())
            blockBuildBegin.add("is_breaking", e.breaking)
            blockBuildBegin.add("time", System.currentTimeMillis())
            writeToFile(blockBuildBegin)
        }

        Events.on(BlockBuildEndEvent::class.java) { e: BlockBuildEndEvent? ->
            val blockBuildEnd = JsonObject()
            blockBuildEnd.add("type", "block_build_end")

            if (e!!.unit != null && e.unit.isPlayer) {
                blockBuildEnd.add("player", getPlayerStatus(e.unit.player))
            }

            blockBuildEnd.add("block", e.tile.block().name)
            blockBuildEnd.add("tile_x", e.tile.x.toInt())
            blockBuildEnd.add("tile_y", e.tile.y.toInt())
            blockBuildEnd.add("is_breaking", e.breaking)
            blockBuildEnd.add("time", System.currentTimeMillis())
            writeToFile(blockBuildEnd)
        }

        // if player or unit rotated the target block
        Events.on(BuildRotateEvent::class.java) { e ->
            val buildRotate = JsonObject()
            buildRotate.add("type", "build_rotate")
            if (e.unit != null && e.unit.isPlayer) {
                buildRotate.add("player", getPlayerStatus(e.unit.player))
                val buildPlan = JsonArray()
                for (plan in e.unit.plans) {
                    val data = JsonObject()
                    data.add("block_name", plan.block.name)
                    data.add("tile_x", plan.tile().x.toInt())
                    data.add("tile_y", plan.tile().y.toInt())
                    data.add("rotation", plan.rotation)
                    buildPlan.add(data)
                }
            }
            buildRotate.add("block", e.build.block.name)
            buildRotate.add("previous", e.previous)
            buildRotate.add("current", e.build.rotation)
            buildRotate.add("time", System.currentTimeMillis())
            writeToFile(buildRotate)
        }

        // if player or unit is block remove or selected
        Events.on(BuildSelectEvent::class.java) { e ->
            val buildSelect = JsonObject()
            buildSelect.add("type", "build_select")
            buildSelect.add("name", e.tile.block().name)
            buildSelect.add("is_breaking", e.breaking)
            buildSelect.add("tile_x", e.tile.x.toInt())
            buildSelect.add("tile_y", e.tile.y.toInt())
            if (e.builder.isPlayer) {
                val buildPlan = JsonArray()
                for (plan in e.builder.plans) {
                    val data = JsonObject()
                    data.add("block_name", plan.block.name)
                    data.add("tile_x", plan.tile().x.toInt())
                    data.add("tile_y", plan.tile().y.toInt())
                    data.add("rotation", plan.rotation)
                    buildPlan.add(data)
                }
            }
            buildSelect.add("time", System.currentTimeMillis())
            writeToFile(buildSelect)
        }

        // if power generator exploded by pressure
        Events.on(GeneratorPressureExplodeEvent::class.java) { e: GeneratorPressureExplodeEvent? -> }

        // if building destroyed by bullet
        Events.on(BuildingBulletDestroyEvent::class.java) { e ->
            val buildingDestroy = JsonObject()
            buildingDestroy.add("type", "bullet_destroy")
            buildingDestroy.add("build_name", e.build.block.name)
            buildingDestroy.add("health", e.build.health())
            buildingDestroy.add("destroyed", e.build.dead)
            buildingDestroy.add("team", e.build.team.name)
            buildingDestroy.add("bullet_team", e.bullet.team.name)
            buildingDestroy.add("bullet_unit", e.bullet.owner is mindustry.gen.Unit)
            buildingDestroy.add("bullet_build", e.bullet.owner is mindustry.world.Block)
            buildingDestroy.add("time", System.currentTimeMillis())
            writeToFile(buildingDestroy)
        }

        // if unit destroyed by bullet
        Events.on(UnitBulletDestroyEvent::class.java) { e ->
            val bulletDestroy = JsonObject()
            bulletDestroy.add("type", "bullet_destroy")
            bulletDestroy.add("unit_name", e.unit.type.name)
            bulletDestroy.add("health", e.unit.health())
            bulletDestroy.add("is_dead", e.unit.dead)
            bulletDestroy.add("team", e.unit.team.name)
            bulletDestroy.add("bullet_team", e.bullet.team.name)
            bulletDestroy.add("bullet_unit", e.bullet.owner is mindustry.gen.Unit)
            bulletDestroy.add("bullet_build", e.bullet.owner is mindustry.world.Block)
            bulletDestroy.add("time", System.currentTimeMillis())
            writeToFile(bulletDestroy)
        }

        // if unit taken damage
        Events.on(UnitDamageEvent::class.java) { e ->
            val damaged = JsonObject()
            damaged.add("type", "bullet_destroy")
            damaged.add("unit_name", e.unit.type.name)
            damaged.add("health", e.unit.health())
            damaged.add("is_dead", e.unit.dead)
            damaged.add("team", e.unit.team.name)
            damaged.add("bullet_team", e.bullet.team.name)
            damaged.add("bullet_unit", e.bullet.owner is mindustry.gen.Unit)
            damaged.add("bullet_build", e.bullet.owner is mindustry.world.Block)
            damaged.add("time", System.currentTimeMillis())
            writeToFile(damaged)
        }

        // if unit is drowned
        Events.on(UnitDrownEvent::class.java) { e: UnitDrownEvent? ->
            val drown = JsonObject()
            drown.add("type", "unit_drown")
            drown.add("unit_name", e!!.unit.type.name)
            drown.add("health", e.unit.health())
            drown.add("is_dead", e.unit.dead)
            drown.add("team", e.unit.team.name)
            drown.add("time", System.currentTimeMillis())
            writeToFile(drown)
        }

        var tick = 0
        Events.on(Trigger.update::class.java) {
            if (tick == 60) {
                val current = JsonObject()
                current.add("type", "current_status")
                current.add("time", System.currentTimeMillis())

                val list = JsonArray()

                Groups.unit.forEach {
                    val currentStatus = JsonObject()
                    currentStatus.add("unit_name", it.type.name)
                    currentStatus.add("team", it.team.name)
                    currentStatus.add("unit_x", it.x)
                    currentStatus.add("unit_y", it.y)
                    currentStatus.add("aim_x", it.aimX)
                    currentStatus.add("aim_y", it.aimY)
                    currentStatus.add("health", it.health())
                    currentStatus.add("shield", it.shield())
                    list.add(currentStatus)
                }

                current.add("units", list)

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
