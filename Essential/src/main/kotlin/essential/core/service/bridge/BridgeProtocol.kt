package essential.core.service.bridge

import java.io.IOException
import java.io.Reader
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal const val MAX_BRIDGE_LINE_LENGTH = 8 * 1024
internal const val MAX_BRIDGE_CRASH_REPORT_LENGTH = 64 * 1024

internal fun hasValidBridgeSecret(secret: String): Boolean =
    secret.toByteArray(StandardCharsets.UTF_8).size >= 32

internal fun createBridgeChallenge(): String =
    ByteArray(32).also(SecureRandom()::nextBytes).let(Base64.getUrlEncoder().withoutPadding()::encodeToString)

internal fun bridgeAuthenticationResponse(secret: String, challenge: String): String {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(mac.doFinal(challenge.toByteArray(StandardCharsets.UTF_8)))
}

internal fun isValidBridgeAuthentication(secret: String, challenge: String, response: String): Boolean =
    MessageDigest.isEqual(
        bridgeAuthenticationResponse(secret, challenge).toByteArray(StandardCharsets.US_ASCII),
        response.toByteArray(StandardCharsets.US_ASCII)
    )

internal fun encodeBridgePayload(payload: String): String =
    Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray(StandardCharsets.UTF_8))

internal fun decodeBridgePayload(payload: String): String? = try {
    String(Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8)
} catch (_: IllegalArgumentException) {
    null
}

internal fun readBridgeLine(reader: Reader, maxLength: Int = MAX_BRIDGE_LINE_LENGTH): String? {
    val line = StringBuilder()
    while (true) {
        when (val character = reader.read()) {
            -1 -> return line.takeIf { it.isNotEmpty() }?.toString()
            '\n'.code -> return line.toString()
            '\r'.code -> Unit
            else -> {
                if (line.length >= maxLength) throw IOException("Bridge protocol line exceeds $maxLength bytes")
                line.append(character.toChar())
            }
        }
    }
}
