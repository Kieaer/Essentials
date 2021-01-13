package essentials.thread

import arc.util.async.Threads.sleep
import essentials.Main
import essentials.event.feature.Permissions
import essentials.internal.CrashReport
import essentials.internal.Log
import java.io.IOException
import java.nio.file.*

object PermissionWatch : Runnable {
    private lateinit var watchKey: WatchKey
    private lateinit var path: Path

    private var watchService: WatchService? = null
    private var tried = false

    override fun run() {
        Thread.currentThread().name = "Essential Permission Watch thread"
        while (!Thread.currentThread().isInterrupted) {
            try {
                watchKey = watchService!!.take()
                sleep(50)
                val events = watchKey.pollEvents()
                for (event in events) {
                    val kind = event.kind()
                    val paths = (event.context() as Path).fileName.toString()
                    if (paths == "permission_user.hjson" || paths == "permission.hjson") {
                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            Permissions.reload(false)
                            Permissions.update(false)
                            tried = !tried
                            Log.info("system.perm.updated")
                        }
                    }
                    /*if(kind.equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                        System.out.println("created something in directory");
                    }else if(kind.equals(StandardWatchEventKinds.ENTRY_DELETE)) {
                        System.out.println("delete something in directory");
                    }else if(kind.equals(StandardWatchEventKinds.ENTRY_MODIFY)) {
                        System.out.println("modified something in directory");
                    }else if(kind.equals(StandardWatchEventKinds.OVERFLOW)) {
                        System.out.println("overflow");
                    }else {
                        System.out.println("hello world");
                    }*/
                }
                if (!watchKey.reset()) {
                    try {
                        watchService!!.close()
                        break
                    } catch (e: IOException) {
                        CrashReport(e)
                    }
                }
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (ignored: Exception) {
            }
        }
    }

    init {
        Main.mainThread.submit {
            if (tried) {
                Permissions.update(true)
                tried = false
            }
        }
        try {
            watchService = FileSystems.getDefault().newWatchService()
            path = Paths.get(Main.pluginRoot.absolutePath())
            path.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.OVERFLOW)
        } catch (e: Exception) {
            CrashReport(e)
        }
    }
}