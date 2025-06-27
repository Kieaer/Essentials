package essential.service

import arc.Events
import essential.event.CustomEvents
import essential.rootPath
import kotlinx.coroutines.isActive
import java.io.IOException
import java.nio.file.*
import kotlin.coroutines.coroutineContext

suspend fun fileWatchService() {
    val watchService: WatchService = FileSystems.getDefault().newWatchService()

    Paths.get(rootPath.child("config/").absolutePath())
        .register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)

    try {
        while (coroutineContext.isActive) {
            val watchKey = watchService.take()
            for (event in watchKey.pollEvents()) {
                val kind = event.kind()
                val paths = (event.context() as Path).fileName.toString()
                Events.fire(CustomEvents.ConfigFileModified(kind, paths))
            }

            if (!watchKey.reset()) {
                break
            }
        }
    } finally {
        watchService.close()
    }
}