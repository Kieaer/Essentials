package essential

import arc.Core
import essential.database.data.PlayerData
import essential.util.toHString
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** 현재 DB 버전 */
const val DATABASE_VERSION: UByte = 4u

/** 플러그인 데이터 폴더 경로 */
val rootPath = Core.settings.dataDirectory.child("mods/Essentials/")

/** Kotlin TimeSource */
private val timeSource = TimeSource.Monotonic

/** 서버 시작 시간 */
private val startupTime = timeSource.markNow()

/** 맵이 시작된 시간 */
private var mapStartTime = timeSource.markNow()

/** 서버가 켜져있는 시간 */
val uptime = (timeSource.markNow() - startupTime).toHString()

/** 현재 플레이 중인 맵 시간 */
val playTime = (timeSource.markNow() - mapStartTime).toHString()

/** 마지막으로 투표 한 시간 */
var lastVoted: TimeMark = timeSource.markNow()

/** 투표 가능 유무 */
var nextVoteAvailable = lastVoted.hasPassedNow()

/** 플레이어별 남은 투표 시간 (UUID, Time) */
var voterCooldown = mapOf<String, TimeMark>()

/** 현재 투표 진행 유무 */
var isVoting = false

/** 플러그인에 의한 치트 사용 유무 (맵 이동시 초기화) */
var isCheated = false

/** 항복 유무 (맵 이동시 초기화) */
var isSurrender = false

/** 플레이어 데이터 목록 */
val players = listOf<PlayerData>()

/** 플러그인 데이터 출력 */
fun getPluginDataInfo(): String {
    return """
        |rootPath: ${rootPath.absolutePath()}
        |startupTime: $startupTime
        |uptime: $uptime
        |playTime: $playTime
        |lastVoted: $lastVoted
        |nextVoteAvailable: $nextVoteAvailable
        |voterCooldown: $voterCooldown
        |isVoting: $isVoting
        |isCheated: $isCheated
        |isSurrender: $isSurrender
    """.trimMargin()
}