package essential.achievements

/*
public enum Achievement {
   Builder, Deconstructor,
   // Specific
   Creator, Eliminator, Defender, Aggressor, Serpulo, Erekir,
   // Time
   TurbidWater, BlackWater, Oil,
   // PvP
   Lord,
   */
/*Iron, Bronze, Silver, Gold, Platinum, Master, GrandMaster*/ /*

    // Wave
    // Attack
}*/

import arc.Events
import essential.core.Main.Companion.scope
import essential.database.data.PlayerDataEntity
import essential.database.data.hasAchievement
import essential.database.data.setAchievement
import kotlinx.coroutines.launch
import mindustry.Vars
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

enum class Achievement {
    // int 배열값은 현재 값과 목표 값
    CrawlerBlockDestroyer {
        override fun value(): Int{
            return 5
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.crawler.block.destroy", "0").toInt()
        }
    },

    SoloMapClear {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.map.clear.solo", "0").toInt()
        }
    },

    LongPlayNoAfk {
        override fun value(): Int{
            return 18000 // 5 hours in seconds
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.time.noafk", "0").toInt()
        }
    },

    NoMiningClear {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.map.clear.nomining", "0").toInt()
        }
    },

    SerpuloPvPWin {
        override fun value(): Int{
            return 5
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.pvp.win.serpulo", "0").toInt()
        }
    },

    ErekirPvPWin {
        override fun value(): Int{
            return 5
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.pvp.win.erekir", "0").toInt()
        }
    },

    BothPlanetsPvPWin {
        override fun value(): Int{
            return 10
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.pvp.win.both", "0").toInt()
        }
    },

    LeaveAndLosePvP {
        override fun value(): Int{
            return 10
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.pvp.leave.lose", "0").toInt()
        }
    },

    PvPWinMaster {
        override fun value(): Int{
            return 300
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.pvpWinCount.toInt()
        }
    },

    MapClearMaster {
        override fun value(): Int{
            return 10
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.map.clear.count", "0").toInt()
        }
    },

    TurretMultiKill {
        override fun value(): Int{
            return 5
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.turret.multikill", "0").toInt()
        }
    },

    QuillKiller {
        override fun value(): Int{
            return 5
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.turret.quill.kill", "0").toInt()
        }
    },

    ZenithKiller {
        override fun value(): Int{
            return 30
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.turret.zenith.kill", "0").toInt()
        }
    },

    OmuraHorizonKiller {
        override fun value(): Int{
            return 5
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.omura.horizon.kill", "0").toInt()
        }
    },

    PvPWinStreak {
        override fun value(): Int{
            return 5
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.pvp.win.streak", "0").toInt()
        }
    },

    PvPWinRate {
        override fun value(): Int{
            return 20
        }

        override fun current(data: PlayerDataEntity): Int{
            var result: Int
            try {
                val total: Int = data.pvpWinCount + data.pvpLoseCount
                if (total < 10) { // Require at least 10 games for win rate calculation
                    result = 0
                } else {
                    result = data.pvpWinCount * 100 / total
                }
            } catch (e: ArithmeticException) {
                result = 0
            }
            return result
        }
    },

    PvPUnderdog {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.pvp.underdog", "0").toInt()
        }
    },

    WarpServerDisconnect {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.warp.disconnect", "0").toInt()
        }
    },

    ExplosionKiller {
        override fun value(): Int{
            return 10
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.explosion.kill", "0").toInt()
        }
    },

    LowPowerClear {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.map.clear.lowpower", "0").toInt()
        }
    },

    PvPDefeatStreak {
        override fun value(): Int{
            return 5
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.pvp.defeat.streak", "0").toInt()
        }
    },

    NoPowerClear {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.map.clear.nopower", "0").toInt()
        }
    },

    NoTurretsClear {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.map.clear.noturrets", "0").toInt()
        }
    },

    DuoTurretSurvival {
        override fun value(): Int{
            return 100
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.wave.duo", "0").toInt()
        }
    },

    FlareOnlyClear {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.map.clear.flareonly", "0").toInt()
        }
    },

    VotingBan {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.voting.ban", "0").toInt()
        }
    },

    PvPContribution {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.pvp.contribution", "0").toInt()
        }
    },

    APM50 {
        override fun value(): Int{
            return 50
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.apm
        }
    },

    APM100 {
        override fun value(): Int{
            return 100
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.apm
        }
    },

    APM200 {
        override fun value(): Int{
            return 200
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.apm
        }
    },

    MapProvider {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.map.provider", "0").toInt()
        }
    },

    FeedbackProvider {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.feedback.provider", "0").toInt()
        }
    },

    Builder {
        override fun value(): Int{
            return 100000
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.blockPlaceCount
        }
    },
    Deconstructor {
        override fun value(): Int{
            return 100000
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.blockBreakCount
        }
    },

    Creator {
        override fun value(): Int{
            return 360000
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.time.sandbox", "0").toInt()
        }
    },
    Eliminator {
        override fun value(): Int{
            return 100
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.pvpWinCount.toInt()
        }
    },
    Defender {
        override fun value(): Int{
            return 10000
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.wave", "0").toInt()
        }
    },
    Aggressor {
        override fun value(): Int{
            return 50
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.attackClear
        }
    },
    Serpulo {
        override fun value(): Int{
            return 360000
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.time.serpulo", "0").toInt()
        }
    },
    Erekir {
        override fun value(): Int{
            return 360000
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.time.erekir", "0").toInt()
        }
    },

    TurbidWater {
        override fun value(): Int{
            return 360000
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.totalPlayed
        }
    },
    BlackWater {
        override fun value(): Int{
            return 720000
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.totalPlayed
        }
    },
    Oil {
        override fun value(): Int{
            return 1080000
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.totalPlayed
        }
    },

    SerpuloQuad {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.unit.serpulo.quad", "0").toInt()
        }
    },

    Lord {
        override fun value(): Int{
            return 70
        }

        override fun current(data: PlayerDataEntity): Int{
            var result: Int
            try {
                val total: Int = data.pvpWinCount + data.pvpLoseCount
                if (total < 50) {
                    result = 0
                } else {
                    result = data.pvpWinCount * 100 / total
                }
            } catch (e: ArithmeticException) {
                result = 0
            }
            return result
        }
    },

    Chatter {
        override fun value(): Int{
            return 10000
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.time.chat", "0").toInt()
        }
    },

    // ??
    MeetOwner {
        override fun value(): Int{
            return 1
        }

        override val isHidden = true

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.time.meetowner", "0").toInt()
        }
    },

    // Specific map clear achievements
    Asteroids {
        override fun value(): Int{
            return 1
        }

        override val isHidden = true

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.map.clear.asteroids", "0").toInt()
        }

        override fun success(data: PlayerDataEntity): Boolean {
            val mapHash = "7b032cc7815022be644d00a877ae0388"
            if (Achievement.Companion.mapHash == mapHash) {
                data.status.put("record.map.clear.asteroids", "1")
                return true
            } else {
                return false
            }
        }
    },

    Transcendence {
        override fun value(): Int{
            return 1
        }

        override val isHidden = true

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.map.clear.transcendence", "0").toInt()
        }

        override fun success(data: PlayerDataEntity): Boolean {
            val mapHash = "f355b3d91d5d8215e557ff045b3864ef"
            if (Achievement.Companion.mapHash == mapHash) {
                data.status.put("record.map.clear.transcendence", "1")
                return true
            } else {
                return false
            }
        }
    },

    NewYear {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.chat.newyear", "0").toInt()
        }
    },

    Loyal {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.login.loyal", "0").toInt()
        }
    },

    WaterExtractor {
        override fun value(): Int{
            return 10
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.build.waterextractor", "0").toInt()
        }
    },

    Attendance {
        override fun value(): Int{
            return 100
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.attendanceDays
        }
    },

    LoyalSixMonths {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.login.loyal.sixmonths", "0").toInt()
        }
    },

    LoyalOneYearSixMonths {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.login.loyal.oneyearsixmonths", "0").toInt()
        }
    },

    AllMaps {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return data.status.getOrDefault("record.map.clear.all", "0").toInt()
        }
    },

    DiscordAuth {
        override fun value(): Int{
            return 1
        }

        override fun current(data: PlayerDataEntity): Int{
            return if (data.discordID?.isNotEmpty() == true) 1 else 0
        }
    };

    abstract fun value(): Int
    open val isHidden: Boolean = false

    abstract fun current(data: PlayerDataEntity): Int
    open fun success(data: PlayerDataEntity): Boolean {
        // Prevent achievements from being cleared in sandbox mode
        if (mindustry.Vars.state.rules.infiniteResources) {
            return false
        }

        // First check if the achievement is already completed in status
        val achievementName = this.toString().lowercase(Locale.getDefault())
        val isCompletedInStatus = data.status.containsKey("achievement.$achievementName")

        // If it's already marked as completed in status, return true
        if (isCompletedInStatus) {
            return true
        }

        // Otherwise, check if the current value meets the target
        return current(data) >= value()
    }

    /**
     * Check if the achievement is completed in the database
     * This is a suspending function and should be called from a coroutine
     */
    suspend fun isCompletedInDatabase(data: PlayerDataEntity): Boolean {
        val achievementName = this.toString().lowercase(Locale.getDefault())
        return hasAchievement(data, achievementName)
    }

    fun set(data: PlayerDataEntity) {
        val achievementName = this.toString().lowercase(Locale.getDefault())

        // Store in status for temporary use during this session
        if (!data.status.containsKey("achievement.$achievementName")) {
            data.status.put(
                "achievement.$achievementName",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )

            // Also store in database for permanent storage
            scope.launch {
                setAchievement(data, achievementName)
            }

            Events.fire(CustomEvents.AchievementClear(this, data))
        }
    }

    companion object {
        private val mapHash: String?
            get() {
                try {
                    val data = Files.readAllBytes(Vars.state.map.file.file().toPath())
                    val hash = MessageDigest.getInstance("MD5").digest(data)
                    return BigInteger(1, hash).toString(16)
                } catch (e: NoSuchAlgorithmException) {
                    return ""
                } catch (e: IOException) {
                    return ""
                }
            }
    }
}
