package remake

import arc.Core
import arc.files.Fi
import arc.util.Log
import mindustry.gen.Groups
import org.hjson.ParseException
import java.io.IOException
import java.nio.file.*

object FileWatchService : Runnable {
    private val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
    private var watchService: WatchService = FileSystems.getDefault().newWatchService()

    override fun run() {
        var watchKey: WatchKey
        val path = Paths.get(root.absolutePath())
        path.register(
            watchService,
            StandardWatchEventKinds.ENTRY_CREATE,
            StandardWatchEventKinds.ENTRY_DELETE,
            StandardWatchEventKinds.ENTRY_MODIFY,
            StandardWatchEventKinds.OVERFLOW
        )

        while (!Thread.currentThread().isInterrupted) {
            try {
                watchKey = watchService.take()
                val events = watchKey.pollEvents()
                for (event in events) {
                    val kind = event.kind()
                    val paths = (event.context() as Path).fileName.toString()
                    if ((paths == "permission_user.txt" || paths == "permission.txt") && kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        try {
                            Permission.load()

                            for (c in Groups.player) {
                                c.name = Permission[c].name
                                c.admin = Permission[c].admin
                            }
                            Log.info("Permission file updated!")
                        } catch (e: ParseException){
                            Log.err(e)
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