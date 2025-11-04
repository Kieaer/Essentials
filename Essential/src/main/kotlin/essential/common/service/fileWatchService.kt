package essential.common.service

import arc.Events
import essential.common.event.CustomEvents
import essential.common.rootPath
import java.nio.file.*

fun fileWatchService() {
    val watchService: WatchService = FileSystems.getDefault().newWatchService()

    Paths.get(rootPath.child("config/").absolutePath())
        .register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)

    try {
        while (Thread.currentThread().isInterrupted.not()) {
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
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
    } finally {
        watchService.close()
    }
}