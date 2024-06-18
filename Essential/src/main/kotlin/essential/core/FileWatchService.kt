package essential.core

import arc.Core
import arc.files.Fi
import arc.util.Log
import mindustry.Vars
import mindustry.net.Administration
import org.hjson.JsonArray
import org.hjson.ParseException
import java.io.IOException
import java.nio.file.*

object FileWatchService: Runnable {
    private val root : Fi = Core.settings.dataDirectory.child("mods/Essentials/")
    private var watchService : WatchService = FileSystems.getDefault().newWatchService()

    override fun run() {
        var watchKey : WatchKey
        val path = Paths.get(root.absolutePath())
        path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY)

        while (!Thread.currentThread().isInterrupted) {
            try {
                watchKey = watchService.take()
                Thread.sleep(100)
                val events = watchKey.pollEvents()
                events.forEach {
                    val kind = it.kind()
                    val paths = (it.context() as Path).fileName.toString()
                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        when (paths) {
                            "permission_user.txt", "permission.txt" -> {
                                try {
                                    Permission.load()
                                    Log.info(Bundle()["config.permission.updated"])
                                } catch (e : ParseException) {
                                    Log.err(e)
                                }
                            }
                            "config.txt" -> {
                                Config.load()
                                Log.info(Bundle()["config.reloaded"])
                            }
                            "ban.txt" -> {
                                Vars.netServer.admins.playerInfo.values().forEach(Consumer { info : Administration.PlayerInfo -> info.banned = false })
                                for (bans in JsonArray.readHjson(Fi(Config.banList).readString()).asArray()) {
                                    val data = bans.asObject()
                                    val id = data["id"].asString()
                                    val ips = data["ip"].asArray()
                                    Vars.netServer.admins.playerInfo.values().find { a -> a.id == id }?.banned = true
                                    for (ip in ips) {
                                        Vars.netServer.admins.playerInfo.values().find { a -> a.lastIP == ip.asString() }?.banned = true
                                    }
                                }

                                Vars.netServer.admins.save()
                            }
                        }
                    }
                }
                if (!watchKey.reset()) {
                    try {
                        watchService.close()
                        break
                    } catch (e : IOException) {
                        println(e)
                    }
                }
            } catch (e : InterruptedException) {
                watchService.close()
                Thread.currentThread().interrupt()
            }
        }
    }
}