package essential.core.service.web.achievement

import essential.common.database.data.getPlayerAchievements
import essential.common.database.data.getPlayerDataByName
import essential.common.players
import essential.common.util.toHString
import essential.core.service.achievements.Achievement
import essential.core.service.web.auth.UserSession
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import java.util.*
import kotlin.time.Duration.Companion.seconds

@Serializable
data class AchievementInfo(
    val name: String,
    val title: String,
    val description: String,
    val goal: String,
    val current: Int,
    val target: Int,
    val completed: Boolean,
    val hidden: Boolean
)

@Serializable
data class MyInfo(
    val name: String,
    val uuid: String,
    val firstPlayed: String,
    val lastLogin: String,
    val permission: String,
    val level: Int,
    val exp: Int,
    val expMax: Int,
    val blockPlaceCount: Int,
    val blockBreakCount: Int,
    val totalPlayed: String,
    val attendanceDays: Int,
    val pvpWinCount: Int,
    val pvpLoseCount: Int,
    val pvpWinRate: Int,
    val waveClear: Int,
    val attackClear: Int,
    val achievementsCompleted: Int,
    val achievementsTotal: Int,
    val achievements: List<AchievementInfo>
)

class AchievementController {
    suspend fun getMyInfo(call: ApplicationCall) {
        val session = call.sessions.get<UserSession>()
            ?: return call.respond(HttpStatusCode.Unauthorized)

        val dbData = getPlayerDataByName(session.username)
            ?: return call.respond(HttpStatusCode.NotFound, "Player not found")

        // Prefer live (connected) data so runtime-only achievement progress is accurate
        val data = players.find { it.uuid == dbData.uuid } ?: dbData

        val completed = getPlayerAchievements(dbData).map { it.achievementName.lowercase() }.toSet()

        // Load achievement names/descriptions in the account's language
        val bundle = try {
            ResourceBundle.getBundle(
                "bundles/achievements/bundle",
                Locale.forLanguageTag(dbData.languageTag.replace("_", "-"))
            )
        } catch (e: MissingResourceException) {
            ResourceBundle.getBundle("bundles/achievements/bundle", Locale.ENGLISH)
        }

        fun localized(prefix: String, key: String, fallback: String): String = try {
            bundle.getString("$prefix.$key")
        } catch (e: MissingResourceException) {
            fallback
        }

        val achievements = Achievement.entries.mapNotNull { ach ->
            val key = ach.name.lowercase()
            val isDone = completed.contains(key)
            // Hide secret achievements until unlocked
            if (ach.isHidden && !isDone) return@mapNotNull null

            val target = ach.value()
            val current = if (isDone) {
                target
            } else {
                try {
                    ach.current(data)
                } catch (e: Exception) {
                    0
                }
            }
            AchievementInfo(
                name = ach.name,
                title = localized("achievement", key, ach.name),
                description = localized("description", key, ""),
                goal = localized("target", key, "").replace("{0}", target.toString()),
                current = current.coerceIn(0, target),
                target = target,
                completed = isDone,
                hidden = ach.isHidden
            )
        }

        val info = MyInfo(
            name = dbData.name,
            uuid = dbData.uuid,
            firstPlayed = dbData.firstPlayed.toString(),
            lastLogin = dbData.lastLoginDate.toString(),
            permission = dbData.permission,
            level = dbData.level,
            exp = dbData.exp,
            expMax = essential.core.Commands.Exp.calculateFullTargetXp(dbData.level).toInt(),
            blockPlaceCount = dbData.blockPlaceCount,
            blockBreakCount = dbData.blockBreakCount,
            totalPlayed = dbData.totalPlayed.toLong().seconds.toHString(),
            attendanceDays = dbData.attendanceDays,
            pvpWinCount = dbData.pvpWinCount.toInt(),
            pvpLoseCount = dbData.pvpLoseCount.toInt(),
            pvpWinRate = run {
                val total = dbData.pvpWinCount + dbData.pvpLoseCount
                if (total > 0) dbData.pvpWinCount * 100 / total else 0
            },
            waveClear = dbData.waveClear,
            attackClear = dbData.attackClear,
            achievementsCompleted = achievements.count { it.completed },
            achievementsTotal = achievements.size,
            achievements = achievements
        )

        call.respond(info)
    }
}

fun Route.achievementRoutes(controller: AchievementController) {
    route("/api/me") {
        authenticate("auth-session") {
            get {
                controller.getMyInfo(call)
            }
        }
    }
}

/** Reflection entry point used by the Web module when achievements are packaged. */
object AchievementWebModule {
    @JvmStatic
    fun registerRoutes(route: Route) {
        route.achievementRoutes(AchievementController())
    }
}
