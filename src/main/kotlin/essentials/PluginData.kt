package essentials

import arc.struct.Seq
import mindustry.Vars
import mindustry.world.Tile
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.io.IOException
import java.util.*

object PluginData {
    var uptime = 0L
    var playtime = 0L
    var pluginVersion = ""

    // 종료시 저장되는 플러그인 데이터
    var warpZones = Seq<WarpZone>()
    var warpBlocks = Seq<WarpBlock>()
    var warpCounts = Seq<WarpCount>()
    var warpTotals = Seq<WarpTotal>()
    var blacklist = Seq<String>()
    var banned = Seq<Banned>()
    var status = Seq<String>()

    var sudoPassword = ""

    data class WarpZone(val mapName: String, val start: Int, val finish: Int, val touch: Boolean, val ip: String, val port: Int) {
        val startTile: Tile get() = Vars.world.tile(start)
        val finishTile: Tile get() = Vars.world.tile(finish)
    }

    data class WarpBlock(val mapName: String, val x: Int, val y: Int, val tileName: String, val size: Int, val ip: String, val port: Int, val description: String) {
        var online = false
    }

    data class WarpCount(val mapName: String, val pos: Int, val ip: String, val port: Int, var players: Int, var numbersize: Int) {
        val tile: Tile get() = Vars.world.tile(pos)
    }

    data class WarpTotal(val mapName: String, val pos: Int, var totalplayers: Int, var numbersize: Int) {
        val tile: Tile get() = Vars.world.tile(pos)
    }

    data class Banned(val time: Long, val name: String, val uuid: String, val reason: String)

    fun save() {
        val data = JsonObject()
        var buffer = JsonArray()

        for (it in warpZones) {
            val obj = JsonObject()
            obj.add("mapName", it.mapName)
            obj.add("start", it.start)
            obj.add("finish", it.finish)
            obj.add("touch", it.touch)
            obj.add("ip", it.ip)
            obj.add("port", it.port)
            buffer.add(obj)
        }
        data.add("warpZones", buffer)
        buffer = JsonArray()

        for (it in warpBlocks) {
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

        for (it in warpCounts) {
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

        for (it in warpTotals) {
            val obj = JsonObject()
            obj.add("mapName", it.mapName)
            obj.add("pos", it.pos)
            obj.add("totalplayers", it.totalplayers)
            obj.add("numbersize", it.numbersize)
            buffer.add(obj)
        }
        data.add("warpTotals", buffer)
        buffer = JsonArray()

        for (it in blacklist) buffer.add(it)
        data.add("blacklist", buffer)
        buffer = JsonArray()

        for (it in banned) {
            val obj = JsonObject()
            obj.add("time", it.time)
            obj.add("name", it.name)
            obj.add("uuid", it.uuid)
            obj.add("reason", it.reason)
            buffer.add(obj)
        }
        data.add("banned", buffer)
        buffer = JsonArray()

        for (it in status) buffer.add(it)
        data.add("status", buffer)

        val encode = Base64.getEncoder()
        if (transaction { DB.Data.selectAll().firstOrNull() == null }) {
            transaction {
                DB.Data.insert {
                    it[this.data] = encode.encodeToString(data.toString().toByteArray())
                }
            }
        } else {
            transaction {
                DB.Data.update {
                    it[this.data] = encode.encodeToString(data.toString().toByteArray())
                }
            }
        }
    }

    fun load() {
        try {
            if (transaction { DB.Data.selectAll().firstOrNull() == null }) {
                save()
            } else {
                transaction {
                    DB.Data.selectAll().first().apply {
                        val data = JsonObject.readHjson(String(Base64.getDecoder().decode(this[DB.Data.data]))).asObject()

                        data["warpZones"].asArray().forEach {
                            val obj = it.asObject()
                            warpZones.add(WarpZone(
                                obj.get("mapName").asString(),
                                obj.get("start").asInt(),
                                obj.get("finish").asInt(),
                                obj.get("touch").asBoolean(),
                                obj.get("ip").asString(),
                                obj.get("port").asInt()
                            ))
                        }

                        data["warpBlocks"].asArray().forEach {
                            val obj = it.asObject()
                            warpBlocks.add(WarpBlock(
                                obj.get("mapName").asString(),
                                obj.get("x").asInt(),
                                obj.get("y").asInt(),
                                obj.get("tileName").asString(),
                                obj.get("size").asInt(),
                                obj.get("ip").asString(),
                                obj.get("port").asInt(),
                                obj.get("description").asString()
                            ))
                        }

                        data["warpCounts"].asArray().forEach {
                            val obj = it.asObject()
                            warpCounts.add(WarpCount(
                                obj.get("mapName").asString(),
                                obj.get("pos").asInt(),
                                obj.get("ip").asString(),
                                obj.get("port").asInt(),
                                obj.get("players").asInt(),
                                obj.get("numbersize").asInt()
                            ))
                        }

                        data["warpTotals"].asArray().forEach {
                            val obj = it.asObject()
                            warpTotals.add(WarpTotal(
                                obj.get("mapName").asString(),
                                obj.get("pos").asInt(),
                                obj.get("totalplayers").asInt(),
                                obj.get("numbersize").asInt()
                            ))
                        }

                        data["blacklist"].asArray().forEach { blacklist.add(it.asString()) }

                        data["banned"].asArray().forEach {
                            val obj = it.asObject()
                            banned.add(Banned(
                                obj.get("time").asLong(),
                                obj.get("name").asString(),
                                obj.get("uuid").asString(),
                                obj.get("reason").asString()
                            ))
                        }

                        if (data["status"] == null) {
                            status = Seq<String>()
                        } else {
                            data["status"].asArray().forEach { status.add(it.asString()) }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            println(e)
        } catch (e: Exception) {
            transaction {
                DB.Data.deleteAll()
            }
        }
    }
}