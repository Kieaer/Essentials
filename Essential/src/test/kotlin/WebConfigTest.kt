import essential.core.service.web.WebConfig
import kotlin.test.Test
import kotlin.test.assertTrue

class WebConfigTest {
    @Test
    fun generatedSessionSecretMeetsMinimumLength() {
        assertTrue(WebConfig().sessionSecret.length >= 32)
    }
}
