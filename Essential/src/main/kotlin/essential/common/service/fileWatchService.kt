package essential.common.service

import arc.Events
import essential.common.event.CustomEvents
import essential.common.rootPath
import java.nio.file.*

fun fileWatchService() {
    val watchService: WatchService = FileSystems.getDefault().newWatchService()

    try {
        val configPath = Paths.get(rootPath.child("config/").absolutePath())
        Files.createDirectories(configPath)
        configPath.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)

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
    } catch (_: NoSuchFileException) {
        // The plugin can be disposed while the watcher is starting (notably in
        // headless smoke tests). There is no directory left to watch.
    } catch (_: ClosedWatchServiceException) {
        // Normal shutdown closes the service while the watcher is blocked.
    } finally {
        watchService.close()
    }
}
