package remake

import arc.Core
import arc.files.Fi
import arc.util.Log
import arc.util.async.Threads.sleep
import java.io.IOException
import java.nio.file.*

object FileWatchService : Runnable {
    private val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
    private var watchService: WatchService = FileSystems.getDefault().newWatchService()

    override fun run() {
        var watchKey: WatchKey
        val path = Paths.get(root.absolutePath())
        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.OVERFLOW)

        while(!Thread.currentThread().isInterrupted) {
            try {
                watchKey = watchService.take()
                sleep(50)
                val events = watchKey.pollEvents()
                for(event in events) {
                    val kind = event.kind()
                    val paths = (event.context() as Path).fileName.toString()
                    if((paths == "permission_user.hjson" || paths == "permission.hjson") && kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        Permission.load()
                        Log.info("Permission file updated!")
                    }
                }
                if(!watchKey.reset()) {
                    try {
                        watchService.close()
                        break
                    } catch(e: IOException) {
                        println(e)
                    }
                }
            } catch(e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }
}