package essential.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MotdLocaleTest {
    @Test
    fun normalizesSupportedLocaleTags() {
        assertEquals("ko-KR", normalizedMotdLocale("ko_KR"))
    }

    @Test
    fun rejectsLocaleTraversalInput() {
        assertNull(normalizedMotdLocale("../../config"))
    }
}
