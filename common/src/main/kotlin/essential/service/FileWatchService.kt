package essential.service

import arc.Core
import arc.Events
import arc.files.Fi
import essential.event.CustomEvents
import essential.rootPath
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchKey
import java.nio.file.WatchService

class FileWatchService : Runnable {
    private var watchService: WatchService = FileSystems.getDefault().newWatchService()

    override fun run() {
        var watchKey: WatchKey
        Paths.get(rootPath.absolutePath()).register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)
        Paths.get(rootPath.child("config/").absolutePath()).register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)

        while (!Thread.currentThread().isInterrupted) {
            try {
                watchKey = watchService.take()
                Thread.sleep(100)
                val events = watchKey.pollEvents()
                events.forEach { watchEvent ->
                    val kind = watchEvent.kind()
                    val paths = (watchEvent.context() as Path).fileName.toString()
                    Events.fire(CustomEvents.ConfigFileModified(kind, paths))
                }
                if (!watchKey.reset()) {
                    try {
                        watchService.close()
                        break
                    } catch (e: IOException) {
                        println(e)
                    }
                }
            } catch (e: InterruptedException) {
                watchService.close()
                Thread.currentThread().interrupt()
            }
        }
    }
}