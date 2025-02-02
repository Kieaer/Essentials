package essentials.data

import essentials.bundle.Bundle
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import mindustry.gen.Playerc
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Serializable
data class PlayerData(
    val name: String,
    val uuid: String,
    val uuidList: List<String> = emptyList(),
    val id: String,
    val password: String,
    val blockPlaceCount: UInt = 0u,
    val blockBreakCount: UInt = 0u,
    val totalJoinCount: UInt = 0u,
    val totalKickCount: UInt = 0u,
    val level: UInt = 0u,
    val exp: UInt = 0u,
    @Serializable(with = LocalDateTimeSerializer::class) val firstPlayDate: LocalDateTime = LocalDateTime.now(),
    @Serializable(with = LocalDateTimeSerializer::class) val lastLoginDate: LocalDateTime = LocalDateTime.now(),
    @Serializable(with = LocalDateTimeSerializer::class) val lastLeaveDate: LocalDateTime = LocalDateTime.now(),
    val totalPlayTime: ULong = 0u,
    val attackModeClear: UInt = 0u,
    val pvpVictories: UInt = 0u,
    val pvpDefeats: UInt = 0u,
    val pvpEliminationTeamCount: UInt = 0u,
    val hideRanking: Boolean = false,
    val joinStacks: UInt = 0u,
    val strict: Boolean = false,
    val isConnected: Boolean = false,

    val animatedName: Boolean = false,
    val permission: String,
    val mute: Boolean = false,
    val discord: String,
    val effectLevel: UInt = 0u,
    val effectColor: String,
    val freeze: Boolean = false,
    val hud: String,
    val tpp: Boolean = false,
    val log: Boolean = false,
    @Serializable(with = LocalDateTimeSerializer::class) val banTime: LocalDateTime?,
    val tracking: Boolean = false,
    val showLevelEffects: Boolean = false,
    val lastPlayedWorldName: List<String> = emptyList(),
    val lastPlayedWorldMode: List<String> = emptyList(),
    val mvpTime: UInt = 0u,

    var currentPlayTime: UInt = 0u,
    var afkTime: UInt = 0u,
    var afk: Boolean = false,
    var player: Playerc,
    var entityId: Int = 0
) {
    /**
     * Bundle 파일에서 [message] 값을 경고 메세지로 플레이어에게 보냄
     */
    fun err(message: String, vararg parameters: Any) {
        val text = "[scarlet]" + Bundle(player.locale()).get(message, *parameters)
        player.sendMessage(text)
    }

    /**
     * Bundle 파일에서 [message] 값을 플레이어에게 보냄
     */
    fun send(message: String, vararg parameters: Any) {
        val text = bundle().get(message, *parameters)
        player.sendMessage(text)
    }

    /**
     * 외부 Bundle 파일에서 [message] 값을 플레이어에게 보냄
     */
    fun send(bundle: Bundle, message: String, vararg parameters: Any) {
        val text = bundle.get(message, *parameters)
        player.sendMessage(text)
    }

    private fun bundle() = Bundle(player.locale())

    /** LocalDateTime 에 대한 Kotlin Serializer */
    private class LocalDateTimeSerializer : KSerializer<LocalDateTime> {
        private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

        override fun serialize(encoder: Encoder, value: LocalDateTime) {
            encoder.encodeString(value.format(formatter))
        }

        override fun deserialize(decoder: Decoder): LocalDateTime {
            return LocalDateTime.parse(decoder.decodeString(), formatter)
        }
    }
}