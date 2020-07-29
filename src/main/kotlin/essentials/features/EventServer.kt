package essentials.features

import arc.ApplicationListener
import arc.Core
import arc.struct.Array
import essentials.Main
import essentials.Main.Companion.tool
import essentials.external.PingHost
import essentials.internal.CrashReport
import essentials.internal.Log
import mindustry.game.Gamemode
import mindustry.net.Host
import org.hjson.JsonValue
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Paths
import java.util.*
import java.util.function.Consumer

class EventServer {
    var servers = Array<Process>()
    fun create(roomname: String, map: String, gamemode: String?, port: Int): Boolean {
        return try {
            val json = JsonValue.readJSON(Main.tool.getWebContent("https://api.github.com/repos/anuken/Mindustry/releases/latest")).asObject()
            val url = json["assets"].asArray()[0].asObject()["browser_download_url"].asString()
            Main.pluginRoot.child("temp").child(roomname).mkdirs()
            tool.download(URL(url), Main.pluginRoot.child("temp/$roomname/server.jar").file())
            val service = EventService(roomname, map, Gamemode.valueOf(gamemode!!), port)
            service.start()
            Thread.sleep(5000)
            true
        } catch (e: Exception) {
            CrashReport(e)
            false
        }
    }

    class EventService internal constructor(var roomname: String, var map: String, var gamemode: Gamemode, var port: Int) : Thread() {
        var disablecount = 0
        override fun run() {
            try {
                val p: Process
                val pb = ProcessBuilder("java", "-jar", Paths.get("").toAbsolutePath().toString() + "/config/mods/Essentials/temp/" + roomname + "/server.jar", "config port $port,host $map $gamemode")
                pb.directory(File(Paths.get("").toAbsolutePath().toString() + "/config/mods/Essentials/temp/" + roomname))
                pb.inheritIO().redirectOutput(Core.settings.dataDirectory.child("test.txt").file())
                p = pb.start()
                sleep(2000)
                Main.eventServer.servers.add(p)
                if (p.isAlive) Log.info("$roomname Event serer online!")
                val t: TimerTask = object : TimerTask() {
                    override fun run() {
                        PingHost("127.0.0.1", port, Consumer { result: Host ->
                            if (disablecount > 300) {
                                try {
                                    val settings = JsonValue.readJSON(Main.pluginRoot.child("data/data.json").reader()).asObject()
                                    for (a in 0 until settings["servers"].asArray().size()) {
                                        if (settings["servers"].asArray()[a].asObject().getInt("port", 0) == port) {
                                            settings["servers"].asArray().remove(a)
                                            Main.pluginRoot.child("data/data.json").writeString(settings.toString())
                                            break
                                        }
                                    }
                                    p.destroy()
                                    Main.pluginData.process.remove(p)
                                    cancel()
                                } catch (e: IOException) {
                                    CrashReport(e)
                                }
                            } else if (result.players == 0) {
                                disablecount++
                            }
                        })
                    }
                }
                Main.timer.scheduleAtFixedRate(t, 1000, 1000)
                Core.app.addListener(object : ApplicationListener {
                    override fun dispose() {
                        Main.timer.cancel()
                    }
                })
            } catch (e: Exception) {
                CrashReport(e)
            }
        }

    }
}