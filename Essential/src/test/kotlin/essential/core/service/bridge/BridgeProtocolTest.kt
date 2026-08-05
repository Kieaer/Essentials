package essential.core.service.bridge

import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BridgeProtocolTest {
    private val secret = "12345678901234567890123456789012"

    @Test
    fun challengeResponseOnlyAcceptsMatchingSecret() {
        val challenge = createBridgeChallenge()
        val response = bridgeAuthenticationResponse(secret, challenge)

        assertTrue(isValidBridgeAuthentication(secret, challenge, response))
        assertFalse(isValidBridgeAuthentication("different-secret-material-32-bytes!", challenge, response))
    }

    @Test
    fun rejectsOverlongProtocolLines() {
        assertNull(readBridgeLine(StringReader("")))
        kotlin.test.assertFailsWith<java.io.IOException> {
            readBridgeLine(StringReader("12345\n"), maxLength = 4)
        }
    }
}
