package essential.common.service

import arc.Events
import essential.common.event.CustomEvents
import essential.common.rootPath
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import java.nio.file.*

suspend fun fileWatchService() {
    val watchService: WatchService = FileSystems.getDefault().newWatchService()

    Paths.get(rootPath.child("config/").absolutePath())
        .register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)

    try {
        while (currentCoroutineContext().isActive) {
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