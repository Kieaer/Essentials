package essentials

import arc.Core
import arc.files.Fi
import arc.util.Log
import org.hjson.ParseException
import java.io.IOException
import java.lang.Thread.sleep
import java.nio.file.*

object FileWatchService : Runnable {
    private val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
    private var watchService: WatchService = FileSystems.getDefault().newWatchService()

    override fun run() {
        var watchKey: WatchKey
        val path = Paths.get(root.absolutePath())
        path.register(
            watchService, StandardWatchEventKinds.ENTRY_MODIFY
        )

        while (!Thread.currentThread().isInterrupted) {
            try {
                watchKey = watchService.take()
                val events = watchKey.pollEvents()
                for (event in events) {
                    val kind = event.kind()
                    val paths = (event.context() as Path).fileName.toString()
                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        if (paths == "permission_user.txt" || paths == "permission.txt") {
                            try {
                                sleep(100)
                                Permission.load()
                                Log.info(Bundle()["config.permission.updated"])
                            } catch (e: ParseException) {
                                Log.err(e)
                            }
                        } else if (paths == "config.txt") {
                            Config.load()
                            Log.info(Bundle()["config.reloaded"])
                        }
                    }
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