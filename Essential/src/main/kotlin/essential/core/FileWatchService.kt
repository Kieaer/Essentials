package essential.core

import arc.Core
import arc.Events
import arc.files.Fi
import arc.func.Cons
import arc.util.Log
import com.charleskorn.kaml.Yaml
import org.hjson.ParseException
import java.io.IOException
import java.nio.file.*

object FileWatchService : Runnable {
    private val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
    private var watchService: WatchService = FileSystems.getDefault().newWatchService()

    fun registerEvent() {
        Events.on(CustomEvents.ConfigFileModified::class.java, Cons<CustomEvents.ConfigFileModified> {
            if (it.kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                when (it.paths) {
                    "permission_user.yaml", "permission.yaml" -> {
                        try {
                            Permission.load()
                            Log.info(Bundle()["config.permission.updated"])
                        } catch (e: ParseException) {
                            Log.err(e)
                        }
                    }

                    "config.yaml" -> {
                        Main.conf = Yaml.default.decodeFromString(
                            Config.serializer(),
                            Main.root.child(Main.CONFIG_PATH).readString()
                        )
                        Log.info(Bundle()["config.reloaded"])
                    }
                }
            }
        }.also { listener -> Event.eventListeners[CustomEvents.ConfigFileModified::class.java] = listener })
    }

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