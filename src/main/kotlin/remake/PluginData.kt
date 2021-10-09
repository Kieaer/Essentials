package remake

import arc.Core
import arc.struct.Seq
import arc.util.serialization.Json
import mindustry.Vars
import mindustry.world.Tile
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.Stringify
import java.io.IOException

object PluginData {
    var uptime = 0L
    var playtime = 0L
    var pluginVersion = 0L

    // 종료시 저장되는 플러그인 데이터
    var warpZones = Seq<WarpZone>()
    var warpBlocks = Seq<WarpBlock>()
    var warpCounts = Seq<WarpCount>()
    var warpTotals = Seq<WarpTotal>()
    var blacklist = Seq<String>()
    var banned = Seq<Banned>()

    private val root = Core.settings.dataDirectory.child("mods/Essentials/data/PluginData.object")

    data class WarpZone(val mapName: String, val start: Int, val finish: Int, val touch: Boolean, val ip: String, val port: Int) {
        val startTile: Tile get() = Vars.world.tile(start)
        val finishTile: Tile get() = Vars.world.tile(finish)
    }

    data class WarpBlock(val mapName: String, val pos: Int, val tileName: String, val size: Int, val ip: String, val port: Int, val description: String) {
        var online = false
    }

    data class WarpCount(val mapName: String, val pos: Int, val ip: String, val port: Int, var players: Int, var numbersize: Int) {
        val tile: Tile get() = Vars.world.tile(pos)
    }

    data class WarpTotal(val mapName: String, val pos: Int, var totalplayers: Int, var numbersize: Int) {
        val tile: Tile get() = Vars.world.tile(pos)
    }

    data class Banned(val time: Long, val name: String, val uuid: String, val reason: String)

    fun save(){
        val json = Json()
        val data = JsonObject()
        var buffer = JsonArray()

        warpZones.forEach { buffer.add(json.toJson(it)) }
        data.add("warpZones", buffer)
        buffer = JsonArray()

        warpBlocks.forEach { buffer.add(json.toJson(it)) }
        data.add("warpBlocks", buffer)
        buffer = JsonArray()

        warpCounts.forEach { buffer.add(json.toJson(it)) }
        data.add("warpCounts", buffer)
        buffer = JsonArray()

        warpTotals.forEach { buffer.add(json.toJson(it)) }
        data.add("warpTotals", buffer)
        buffer = JsonArray()

        blacklist.forEach { buffer.add(it) }
        data.add("blacklist", buffer)
        buffer = JsonArray()

        banned.forEach { buffer.add(json.toJson(it)) }
        data.add("banned", buffer)

        root.file().writeText(data.toString(Stringify.FORMATTED))
    }

    fun load(){
        val json = Json()

        try {
            if (!root.exists()) {
                save()
            } else {
                val data = JsonObject.readJSON(root.readString("utf-8")).asObject()
                data["warpZones"].asArray().forEach { warpZones.add(json.fromJson(WarpZone::class.java, it.toString())) }
                data["warpBlocks"].asArray().forEach { warpBlocks.add(json.fromJson(WarpBlock::class.java, it.toString())) }
                data["warpCounts"].asArray().forEach { warpCounts.add(json.fromJson(WarpCount::class.java, it.toString())) }
                data["warpTotals"].asArray().forEach { warpTotals.add(json.fromJson(WarpTotal::class.java, it.toString())) }
                data["blacklist"].asArray().forEach { blacklist.add(it.asString()) }
                data["banned"].asArray().forEach { banned.add(json.fromJson(Banned::class.java, it.toString())) }
            }
        } catch (e: IOException) {
            println(e)
        }
    }
}