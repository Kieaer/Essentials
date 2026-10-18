package essential.common.bundle

import kotlin.test.Test
import kotlin.test.assertEquals

class BlockNameResolverTest {
    @Test
    fun resolvesEmptySpaceInPlayerLanguage() {
        assertEquals("Empty space", BlockNameResolver("en")["air"])
        assertEquals("빈 공간", BlockNameResolver("ko_KR")["air"])
    }

    @Test
    fun resolvesNamesFromMindustryBundle() {
        assertEquals("Copper Wall", BlockNameResolver("en")["copper-wall"])
        assertEquals("구리 벽", BlockNameResolver("ko_KR")["copper-wall"])
    }

    @Test
    fun keepsUnknownContentNameReadable() {
        assertEquals("custom-block", BlockNameResolver("en")["custom-block"])
    }
}
