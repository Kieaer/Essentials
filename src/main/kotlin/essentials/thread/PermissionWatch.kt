package essentials.internal.thread

import essentials.Main
import essentials.internal.CrashReport
import essentials.internal.Log
import java.io.IOException
import java.nio.file.*

class PermissionWatch : Runnable {
    lateinit var watchKey: WatchKey
    lateinit var path: Path

    var watchService: WatchService? = null
    var tried = false

    override fun run() {
        Thread.currentThread().name = "Essential Permission Watch thread"
        while (!Thread.currentThread().isInterrupted) {
            try {
                watchKey = watchService!!.take()
                Thread.sleep(50)
                val events = watchKey.pollEvents()
                for (event in events) {
                    val kind = event.kind()
                    val paths = (event.context() as Path).fileName.toString()
                    if (paths == "permission_user.hjson" || paths == "permission.hjson") {
                        if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                            Main.perm.reload(false)
                            Main.perm.update(false)
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
                Main.perm.update(true)
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