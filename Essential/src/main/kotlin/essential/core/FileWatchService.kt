package essential.core

import arc.Core
import arc.Events
import arc.files.Fi
import java.io.IOException
import java.nio.file.*

class FileWatchService : Runnable {
    private val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
    private var watchService: WatchService = FileSystems.getDefault().newWatchService()

    override fun run() {
        var watchKey: WatchKey
        val path = Paths.get(root.absolutePath())
        path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)

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