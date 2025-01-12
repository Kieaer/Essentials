package essentials.database

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
    val blockPlaceCount: Int = 0,
    val blockBreakCount: Int = 0,
    val totalJoinCount: Int = 0,
    val totalKickCount: Int = 0,
    val level: Int = 0,
    val exp: Int = 0,
    @Serializable(with = LocalDateTimeSerializer::class)
    val firstPlayDate: LocalDateTime = LocalDateTime.now(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastLoginDate: LocalDateTime = LocalDateTime.now(),
    @Serializable(with = LocalDateTimeSerializer::class)
    val lastLeaveDate: LocalDateTime = LocalDateTime.now(),
    val totalPlayTime: Long = 0,
    val attackModeClear: Int = 0,
    val pvpVictories: Int = 0,
    val pvpDefeats: Int = 0,
    val pvpEliminationTeamCount: Int = 0,
    val hideRanking: Boolean = false,
    val joinStacks: Int = 0,
    val strict: Boolean = false,
    val isConnected: Boolean = false,

    val animatedName: Boolean = false,
    val permission: String,
    val mute: Boolean = false,
    val discord: String,
    val effectLevel: Int = 0,
    val effectColor: String,
    val freeze: Boolean = false,
    val hud: String,
    val tpp: Boolean = false,
    val log: Boolean = false,
    @Serializable(with = LocalDateTimeSerializer::class)
    val banTime: LocalDateTime?,
    val tracking: Boolean = false,
    val showLevelEffects: Boolean = false,
    val lastPlayedWorldName: List<String> = emptyList(),
    val lastPlayedWorldMode: List<String> = emptyList(),
    val mvpTime: Int = 0,

    var currentPlayTime: Int = 0,
    var afkTime: Int = 0,
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

    fun bundle() = Bundle(player.locale())
}

/** LocalDateTime 에 대한 Kotlin Serializer */
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.format(formatter))
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString(), formatter)
    }
}