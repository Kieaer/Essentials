package essential

import arc.Core
import arc.files.Fi
import arc.func.Cons
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.ObjectMapper
import essential.bundle.Bundle
import essential.database.data.PlayerData
import essential.database.data.PluginData
import essential.util.toHString
import kotlinx.datetime.TimeZone
import java.util.HashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** 현재 DB 버전 */
const val DATABASE_VERSION: UByte = 4u

/** 플러그인 버전 */
val PLUGIN_VERSION: String get() {
    val file = PluginData::class.java.getResourceAsStream("/plugin.json")
    file.use {
        return ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .readTree(it)
            .get("version")
            .asText()
    }
}

/** 플러그인 메세지 데이터 */
val bundle = Bundle()

/** 플러그인 데이터 폴더 경로 */
val rootPath: Fi = Core.settings.dataDirectory.child("mods/Essentials/")

/** Kotlin TimeSource */
val timeSource = TimeSource.Monotonic

/** 서버 시작 시간 */
private val startupTime = timeSource.markNow()

/** 맵이 시작된 시간 */
var mapStartTime = timeSource.markNow()

/** 서버가 켜져있는 시간 */
val uptime : String get() = (timeSource.markNow() - startupTime).toHString()

/** 현재 플레이 중인 맵 시간 */
val playTime : String get() = (timeSource.markNow() - mapStartTime).toHString()

/** 투표 가능 유무 */
var nextVoteAvailable : TimeMark = timeSource.markNow()

/** 플레이어별 남은 투표 시간 (UUID, Time) */
var voterCooldown = mutableMapOf<String, TimeMark>()

/** 현재 투표 진행 유무 */
var isVoting = false

/** 플러그인에 의한 치트 사용 유무 (맵 이동시 초기화) */
var isCheated = false

/** 항복 유무 (맵 이동시 초기화) */
var isSurrender = false

/** 플레이어 데이터 목록 */
val players = CopyOnWriteArrayList<PlayerData>()

/** 시스템 Time zone */
val systemTimezone = TimeZone.currentSystemDefault()

/** 플레이어 번호 */
var playerNumber = 0

/** 플레이 도중 나간 플레이어 목록 */
var offlinePlayers = mutableListOf<PlayerData>()

/** 플러그인 데이터 */
lateinit var pluginData: PluginData

/** 플러그인이 등록한 Event listeners 목록 */
val eventListeners: HashMap<Class<*>, Cons<*>> = hashMapOf()

/** 플러그인 데이터 출력 */
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
