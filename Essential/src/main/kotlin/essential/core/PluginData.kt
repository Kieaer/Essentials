package essential.core

import mindustry.Vars
import mindustry.world.Tile
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.IOException
import java.time.LocalTime
import java.util.*

object PluginData {
    var uptime = 0L
    var playtime = 0L
    var pluginVersion = ""
    var databaseVersion = 3

    var warpZones: ArrayList<WarpZone> = arrayListOf()
    var warpBlocks: ArrayList<WarpBlock> = arrayListOf()
    var warpCounts: ArrayList<WarpCount> = arrayListOf()
    var warpTotals: ArrayList<WarpTotal> = arrayListOf()
    var blacklist: ArrayList<String> = arrayListOf()
    var banned: ArrayList<Banned> = arrayListOf()
    var status: ArrayList<Pair<String, String>> = arrayListOf()

    var isRankingWorking = false
    var isSurrender = false
    var isCheated = false

    var entityOrder = 0
    var effectLocal = false
    var currentMap = ""
    var voting = false

    var voteCooltime: Int = 0
    var voterCooltime = HashMap<String, Int>()
    var lastVoted: LocalTime? = null

    data class WarpZone(
        val mapName: String,
        val start: Int,
        val finish: Int,
        val click: Boolean,
        val ip: String,
        val port: Int
    ) {
        val startTile: Tile get() = Vars.world.tile(start)
        val finishTile: Tile get() = Vars.world.tile(finish)
    }

    data class WarpBlock(
        val mapName: String,
        val x: Int,
        val y: Int,
        val tileName: String,
        val size: Int,
        val ip: String,
        val port: Int,
        val description: String
    ) {
        var online = false
    }

    data class WarpCount(
        val mapName: String,
        val pos: Int,
        val ip: String,
        val port: Int,
        var players: Int,
        var numbersize: Int
    ) {
        val tile: Tile get() = Vars.world.tile(pos)
    }

    data class WarpTotal(val mapName: String, val pos: Int, var totalplayers: Int, var numbersize: Int) {
        val tile: Tile get() = Vars.world.tile(pos)
    }

    data class Banned(val time: Long, val name: String, val uuid: String, val reason: String)

    fun save(first: Boolean) {
        val data = JsonObject()
        var buffer = JsonArray()

        warpZones.forEach {
            val obj = JsonObject()
            obj.add("mapName", it.mapName)
            obj.add("start", it.start)
            obj.add("finish", it.finish)
            obj.add("touch", it.click)
            obj.add("ip", it.ip)
            obj.add("port", it.port)
            buffer.add(obj)
        }
        data.add("warpZones", buffer)
        buffer = JsonArray()

        warpBlocks.forEach {
            val obj = JsonObject()
            obj.add("mapName", it.mapName)
            obj.add("x", it.x)
            obj.add("y", it.y)
            obj.add("tileName", it.tileName)
            obj.add("size", it.size)
            obj.add("ip", it.ip)
            obj.add("port", it.port)
            obj.add("description", it.description)
            buffer.add(obj)
        }
        data.add("warpBlocks", buffer)
        buffer = JsonArray()

        warpCounts.forEach {
            val obj = JsonObject()
            obj.add("mapName", it.mapName)
            obj.add("pos", it.pos)
            obj.add("ip", it.ip)
            obj.add("port", it.port)
            obj.add("players", it.players)
            obj.add("numbersize", it.numbersize)
            buffer.add(obj)
        }
        data.add("warpCounts", buffer)
        buffer = JsonArray()

        warpTotals.forEach {
            val obj = JsonObject()
            obj.add("mapName", it.mapName)
            obj.add("pos", it.pos)
            obj.add("totalplayers", it.totalplayers)
            obj.add("numbersize", it.numbersize)
            buffer.add(obj)
        }
        data.add("warpTotals", buffer)
        buffer = JsonArray()

        blacklist.forEach {
            buffer.add(it)
        }
        data.add("blacklist", buffer)
        buffer = JsonArray()

        banned.forEach {
            val obj = JsonObject()
            obj.add("time", it.time)
            obj.add("name", it.name)
            obj.add("uuid", it.uuid)
            obj.add("reason", it.reason)
            buffer.add(obj)
        }
        data.add("banned", buffer)

        val json = JsonObject()
        status.forEach {
            json.add(it.first, it.second)
        }
        data.add("status", json.toString())

        json.add("isDuplicateNameChecked", true)

        transaction {
            if (first) {
                DB.Data.insert {
                    it[DB.Data.data] = data.toString()
                }
            } else {
                DB.Data.update {
                    it[DB.Data.data] = data.toString()
                }
            }
        }
    }

    operator fun get(key: String): String? {
        return status.find { e -> e.first == key }?.second
    }

    fun load() {
        try {
            transaction {
                if (DB.Data.selectAll().firstOrNull() == null) {
                    save(true)
                } else {
                    warpZones = arrayListOf()
                    warpBlocks = arrayListOf()
                    warpCounts = arrayListOf()
                    warpTotals = arrayListOf()
                    blacklist = arrayListOf()
                    banned = arrayListOf()
                    status = arrayListOf()

                    DB.Data.selectAll().first().run {
                        // upgrade
                        var isOldVersion = false
                        val data = try {
                            JsonObject.readJSON(this[DB.Data.data]).asObject()
                        } catch (e: Exception) {
                            isOldVersion = true
                            JsonObject.readJSON(String(Base64.getDecoder().decode(this[DB.Data.data].toString()))).asObject()
                        }

                        data["warpZones"].asArray().forEach {
                            val obj = it.asObject()
                            warpZones.add(
                                WarpZone(
                                    obj["mapName"].asString(),
                                    obj["start"].asInt(),
                                    obj["finish"].asInt(),
                                    obj["touch"].asBoolean(),
                                    obj["ip"].asString(),
                                    obj["port"].asInt()
                                )
                            )
                        }

                        data["warpBlocks"].asArray().forEach {
                            val obj = it.asObject()
                            warpBlocks.add(
                                WarpBlock(
                                    obj["mapName"].asString(),
                                    obj["x"].asInt(),
                                    obj["y"].asInt(),
                                    obj["tileName"].asString(),
                                    obj["size"].asInt(),
                                    obj["ip"].asString(),
                                    obj["port"].asInt(),
                                    obj["description"].asString()
                                )
                            )
                        }

                        data["warpCounts"].asArray().forEach {
                            val obj = it.asObject()
                            warpCounts.add(
                                WarpCount(
                                    obj["mapName"].asString(),
                                    obj["pos"].asInt(),
                                    obj["ip"].asString(),
                                    obj["port"].asInt(),
                                    obj["players"].asInt(),
                                    obj["numbersize"].asInt()
                                )
                            )
                        }

                        data["warpTotals"].asArray().forEach {
                            val obj = it.asObject()
                            warpTotals.add(
                                WarpTotal(
                                    obj["mapName"].asString(),
                                    obj["pos"].asInt(),
                                    obj["totalplayers"].asInt(),
                                    obj["numbersize"].asInt()
                                )
                            )
                        }

                        data["blacklist"].asArray().forEach { blacklist.add(it.asString()) }

                        data["banned"].asArray().forEach {
                            val obj = it.asObject()
                            banned.add(
                                Banned(
                                    obj["time"].asLong(),
                                    obj["name"].asString(),
                                    obj["uuid"].asString(),
                                    obj["reason"].asString()
                                )
                            )
                        }

                        JsonArray.readJSON(data["status"].asString().replace("\\", "")).asObject().forEach {
                            status.add(Pair(it.name, it.value.asString()))
                        }

                        if (isOldVersion) {
                            save(false)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun toString(): String {
        return "PluginData(uptime=$uptime, playtime=$playtime, pluginVersion='$pluginVersion', databaseVersion=$databaseVersion, warpZones=$warpZones, warpBlocks=$warpBlocks, warpCounts=$warpCounts, warpTotals=$warpTotals, blacklist=$blacklist, banned=$banned, status=$status, isRankingWorking=$isRankingWorking, isSurrender=$isSurrender, isCheated=$isCheated, entityOrder=$entityOrder, effectLocal=$effectLocal, currentMap='$currentMap', voting=$voting, voteCooltime=$voteCooltime, voterCooltime=$voterCooltime, lastVoted=$lastVoted)"
    }


}