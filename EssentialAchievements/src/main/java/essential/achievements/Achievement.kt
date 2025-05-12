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

import essential.database.data.PlayerData
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
    Builder {
        override fun value(): UInt{
            return 100000u
        }

        override fun current(data: PlayerData): UInt{
            return data.blockPlaceCount
        }
    },
    Deconstructor {
        override fun value(): UInt{
            return 100000u
        }

        override fun current(data: PlayerData): UInt{
            return data.getBlockBreakCount()
        }
    },

    Creator {
        override fun value(): UInt{
            return 360000u
        }

        override fun current(data: PlayerData): UInt{
            return data.getStatus().getOrDefault("record.time.sandbox", "0").toInt()
        }
    },
    Eliminator {
        override fun value(): UInt{
            return 100u
        }

        override fun current(data: PlayerData): UInt{
            return data.getPvpVictoriesCount()
        }
    },
    Defender {
        override fun value(): UInt{
            return 10000u
        }

        override fun current(data: PlayerData): UInt{
            return data.getStatus().getOrDefault("record.wave", "0").toInt()
        }
    },
    Aggressor {
        override fun value(): UInt{
            return 50u
        }

        override fun current(data: PlayerData): UInt{
            return data.getAttackModeClear()
        }
    },
    Serpulo {
        override fun value(): UInt{
            return 360000u
        }

        override fun current(data: PlayerData): UInt{
            return data.getStatus().getOrDefault("record.time.serpulo", "0").toInt()
        }
    },
    Erekir {
        override fun value(): UInt{
            return 360000u
        }

        override fun current(data: PlayerData): UInt{
            return data.getStatus().getOrDefault("record.time.erekir", "0").toInt()
        }
    },

    TurbidWater {
        override fun value(): UInt{
            return 360000u
        }

        override fun current(data: PlayerData): UInt{
            return data.getTotalPlayTime() as Int
        }
    },
    BlackWater {
        override fun value(): UInt{
            return 720000u
        }

        override fun current(data: PlayerData): UInt{
            return data.getTotalPlayTime() as Int
        }
    },
    Oil {
        override fun value(): UInt{
            return 1080000u
        }

        override fun current(data: PlayerData): UInt{
            return data.getTotalPlayTime() as Int
        }
    },

    Lord {
        override fun value(): UInt{
            return 70u
        }

        override fun current(data: PlayerData): UInt{
            var result: Int
            try {
                val total: Int = data.getPvpVictoriesCount() + data.getPvpDefeatCount()
                if (total < 50) {
                    result = 0
                } else {
                    result = data.getPvpVictoriesCount() * 100 / total
                }
            } catch (e: ArithmeticException) {
                result = 0
            }
            return result
        }
    },

    Chatter {
        override fun value(): UInt{
            return 10000u
        }

        override fun current(data: PlayerData): UInt{
            return data.getStatus().getOrDefault("record.time.chat", "0").toInt()
        }
    },

    // ??
    MeetOwner {
        override fun value(): UInt{
            return 1u
        }

        override fun isHidden(): Boolean {
            return true
        }

        override fun current(data: PlayerData): UInt{
            return data.getStatus().getOrDefault("record.time.meetowner", "0").toInt()
        }
    },

    // Specific map clear achievements
    Asteroids {
        override fun value(): UInt{
            return 1u
        }

        override fun isHidden(): Boolean {
            return true
        }

        override fun current(data: PlayerData): UInt{
            return data.getStatus().getOrDefault("record.map.clear.asteroids", "0").toInt()
        }

        override fun success(data: PlayerData): Boolean {
            val mapHash = "7b032cc7815022be644d00a877ae0388"
            if (Achievement.Companion.mapHash == mapHash) {
                data.getStatus().put("record.map.clear.asteroids", "1")
                return true
            } else {
                return false
            }
        }
    },

    Transcendence {
        override fun value(): UInt{
            return 1u
        }

        override fun isHidden(): Boolean {
            return true
        }

        override fun current(data: PlayerData): UInt{
            return data.getStatus().getOrDefault("record.map.clear.transcendence", "0").toInt()
        }

        override fun success(data: PlayerData): Boolean {
            val mapHash = "f355b3d91d5d8215e557ff045b3864ef"
            if (Achievement.Companion.mapHash == mapHash) {
                data.getStatus().put("record.map.clear.transcendence", "1")
                return true
            } else {
                return false
            }
        }
    };

    abstract fun value(): UInt
    open val isHidden: Boolean
        get() = false

    abstract fun current(data: PlayerData): UInt
    open fun success(data: PlayerData): Boolean {
        return current(data) >= value()
    }

    fun set(data: PlayerData) {
        if (!data.getStatus().containsKey("achievement." + this.toString().lowercase(Locale.getDefault()))) {
            data.getStatus().put(
                "achievement." + this.toString().lowercase(Locale.getDefault()),
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )
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