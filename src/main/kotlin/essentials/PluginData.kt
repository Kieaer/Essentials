package essentials

import arc.struct.Seq
import arc.util.serialization.Json
import essentials.Main.Companion.pluginRoot
import essentials.event.feature.Vote
import essentials.event.feature.VoteType
import essentials.internal.Log
import mindustry.Vars.world
import mindustry.gen.Nulls
import mindustry.gen.Playerc
import mindustry.world.Tile
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.JsonValue
import java.io.FileNotFoundException
import java.lang.Thread.sleep

object PluginData {
    const val buildVersion = 127
    const val buildRevision = 0

    var serverIP: String = "127.0.0.1"
    var pluginVersion: String? = null
    var uptime = 0L
    var playtime = 0L
    val playerData = Seq<PlayerData>()
    var players = Seq<Playerc>()

    var votingClass: Vote? = null
    var isVoting: Boolean = false
    var votingType: VoteType = VoteType.None
    var votingPlayer: Playerc? = Nulls.unit.player

    var expData: JsonObject = JsonObject()

    private val json = Json()

    // 종료시 저장되는 플러그인 데이터
    var warpzones = Seq<WarpZone>()
    var warpblocks = Seq<WarpBlock>()
    var warpcounts = Seq<WarpCount>()
    var warptotals = Seq<WarpTotal>()
    var blacklist = Seq<String>()
    var banned = Seq<Banned>()

    operator fun get(uuid: String): PlayerData? {
        for(p in playerData) {
            if(p.uuid == uuid) return p
        }
        return null
    }

    fun removePlayerData(d: PlayerData) {
        playerData.remove(d)
    }

    fun saveAll() {
        val data = JsonObject()
        var buffer = JsonArray()
        for(a in 0 until warpzones.size) {
            buffer.add(JsonValue.readHjson(json.prettyPrint(warpzones[a])))
        }
        data.add("warpzones", buffer)

        buffer = JsonArray()
        for(a in 0 until warpblocks.size) {
            buffer.add(JsonValue.readHjson(json.prettyPrint(warpblocks[a])))
        }
        data.add("warpblocks", buffer)

        buffer = JsonArray()
        for(a in 0 until warpcounts.size) {
            buffer.add(JsonValue.readHjson(json.prettyPrint(warpcounts[a])))
        }
        data.add("warpcounts", buffer)

        buffer = JsonArray()
        for(a in 0 until warptotals.size) {
            buffer.add(JsonValue.readHjson(json.prettyPrint(warptotals[a])))
        }
        data.add("warptotals", buffer)

        buffer = JsonArray()
        for(a in 0 until blacklist.size) {
            buffer.add(blacklist[a])
        }
        data.add("blacklist", buffer)

        buffer = JsonArray()
        for(a in 0 until banned.size) {
            buffer.add(JsonValue.readHjson(json.prettyPrint(banned[a])))
        }
        data.add("banned", buffer)

        val file = pluginRoot.child("data/PluginData.object").file()
        try {
            file.writeText(data.toString())
        } catch(e: FileNotFoundException) {
            Thread {
                sleep(32)
                file.writeText(data.toString())
            }.start()
        }
    }

    fun loadAll() {
        try {
            if(!pluginRoot.child("data/PluginData.object").exists()) {
                saveAll()
            } else {
                val data = JsonValue.readJSON(pluginRoot.child("data/PluginData.object").readString()).asObject()
                for(a in 0 until data["warpzones"].asArray().size()) {
                    val buffer = data["warpzones"].asArray()[a].asObject()
                    warpzones.add(WarpZone(buffer["mapName"].asString(), buffer["start"].asInt(), buffer["finish"].asInt(), buffer["touch"].asBoolean(), buffer["ip"].asString(), buffer["port"].asInt()))
                }
                for(a in 0 until data["warpblocks"].asArray().size()) {
                    val buffer = data["warpblocks"].asArray()[a].asObject()
                    warpblocks.add(WarpBlock(buffer["mapName"].asString(), buffer["pos"].asInt(), buffer["tileName"].asString(), buffer["size"].asInt(), buffer["ip"].asString(), buffer["port"].asInt(), buffer["description"].asString()))
                }
                for(a in 0 until data["warpcounts"].asArray().size()) {
                    val buffer = data["warpcounts"].asArray()[a].asObject()
                    warpcounts.add(WarpCount(buffer["mapName"].asString(), buffer["pos"].asInt(), buffer["ip"].asString(), buffer["port"].asInt(), buffer["players"].asInt(), buffer["numbersize"].asInt()))
                }
                for(a in 0 until data["warptotals"].asArray().size()) {
                    val buffer = data["warptotals"].asArray()[a].asObject()
                    warptotals.add(WarpTotal(buffer["mapName"].asString(), buffer["tilePos"].asInt(), buffer["totalplayers"].asInt(), buffer["numbersize"].asInt()))
                }
                for(a in 0 until data["blacklist"].asArray().size()) {
                    val buffer = data["blacklist"].asArray()[a].asArray()
                    blacklist.add(buffer[a].asString())
                }
                for(a in 0 until data["banned"].asArray().size()) {
                    val buffer = data["banned"].asArray()[a].asObject()
                    banned.add(Banned(buffer["time"].asLong(), buffer["name"].asString(), buffer["uuid"].asString(), buffer["reason"].asString()))
                }
                Log.info("plugindata-loaded")
            }
        } catch(i: Exception) {
            pluginRoot.child("data/PluginData.object").delete()
            saveAll()
        }
    }

    class WarpZone(val mapName: String, val start: Int, val finish: Int, val touch: Boolean, val ip: String, val port: Int) {
        val startTile: Tile get() = world.tile(start)
        val finishTile: Tile get() = world.tile(finish)
    }

    class WarpBlock(val mapName: String, val pos: Int, val tileName: String, val size: Int, val ip: String, val port: Int, val description: String) {
        var online = false
    }

    class WarpCount(val mapName: String, val pos: Int, val ip: String, val port: Int, var players: Int, var numbersize: Int) {
        val tile: Tile get() = world.tile(pos)
    }

    class WarpTotal(val mapName: String, val pos: Int, var totalplayers: Int, var numbersize: Int) {
        val tile: Tile get() = world.tile(pos)
    }

    class Banned(val time: Long, val name: String, val uuid: String, val reason: String)
}