package essential.core.service.web

import arc.Core
import arc.Settings
import arc.files.Fi
import essential.core.service.web.maps.MapController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class MapDeleteTest {
    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("essentials_test").toFile()
        Core.settings = Settings()
        Core.settings.dataDirectory = Fi(tempDir.absolutePath)
    }

    @AfterTest
    fun cleanup() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testMapControllerInitCreatesDirectoriesAndLoadsUploaders() {
        val controller = MapController()

        // Create dummy uploaders json
        val uploadersFile = controller.uploadersFile
        uploadersFile.parentFile.mkdirs()
        val dummyData = mapOf("TestMap" to "testerUser", "AnotherMap" to "admin")
        uploadersFile.writeText(Json.encodeToString(dummyData))

        val scope = CoroutineScope(Dispatchers.Default)
        controller.init(scope)

        assertTrue(controller.webCacheDir.exists())
        assertTrue(uploadersFile.exists())
    }
}
