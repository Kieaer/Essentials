package essential.common

import arc.Core
import arc.files.Fi
import arc.func.Cons
import essential.common.bundle.Bundle
import essential.common.database.data.PlayerData
import essential.common.database.data.PluginData
import essential.common.util.toHString
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Current database version */
const val DATABASE_VERSION: UByte = 4u

/** Plugin version */
val PLUGIN_VERSION: String get() {
    val file = PluginData::class.java.getResourceAsStream("/plugin.json")
    file.use {
        val jsonString = it.bufferedReader().readText()
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val jsonObject = json.parseToJsonElement(jsonString).jsonObject
        return jsonObject["version"]?.jsonPrimitive?.content ?: "unknown"
    }
}

/** Plugin message bundle */
val bundle = Bundle()

/** Plugin data folder path */
val rootPath: Fi = Core.settings.dataDirectory.child("mods/Essentials/")

/** Kotlin TimeSource */
val timeSource = TimeSource.Monotonic

/** Server start time */
private val startupTime = timeSource.markNow()

/** Map start time */
var mapStartTime = timeSource.markNow()

/** Server uptime */
val uptime : String get() = (timeSource.markNow() - startupTime).toHString()

/** Elapsed time on current map */
val playTime : String get() = (timeSource.markNow() - mapStartTime).toHString()

/** Next vote availability time */
var nextVoteAvailable : TimeMark = timeSource.markNow()

/** Remaining vote cooldown per player (UUID -> TimeMark) */
var voterCooldown = mutableMapOf<String, TimeMark>()

/** Whether a vote is in progress */
var isVoting = false

/** Whether plugin-induced cheats are enabled (reset on map change) */
var isCheated = false

/** Whether surrender is active (reset on map change) */
var isSurrender = false

/** Player data list */
val players = CopyOnWriteArrayList<PlayerData>()

/** System time zone */
val systemTimezone = TimeZone.currentSystemDefault()

/** Player number sequence */
var playerNumber = 0

/** Players who left during a match */
var offlinePlayers = mutableListOf<PlayerData>()

/** Plugin data */
lateinit var pluginData: PluginData

/** Event listeners registered by the plugin */
val eventListeners: HashMap<Class<*>, Cons<*>> = hashMapOf()

/** Print plugin data summary */
fun getPluginDataInfo(): String {
    return """
        |rootPath: ${rootPath.absolutePath()}
        |startupTime: $startupTime
        |uptime: $uptime
        |playTime: $playTime
        |nextVoteAvailable: $nextVoteAvailable
        |voterCooldown: $voterCooldown
        |isVoting: $isVoting
        |isCheated: $isCheated
        |isSurrender: $isSurrender
        |offlinePlayers: ${offlinePlayers.size}
        |pluginData: $pluginData
    """.trimMargin()
}
