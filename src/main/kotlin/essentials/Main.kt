package essentials

import arc.ApplicationListener
import arc.Core
import arc.Events
import arc.files.Fi
import arc.util.CommandHandler
import arc.util.Http
import arc.util.Log
import essentials.Permission.bundle
import mindustry.Vars.*
import mindustry.game.Team
import mindustry.mod.Plugin
import mindustry.net.Administration
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.JsonValue
import org.hjson.Stringify
import java.io.*
import java.net.ServerSocket
import java.net.SocketException
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.function.Consumer

class Main: Plugin() {
    companion object {
        val database = DB()
        val root : Fi = Core.settings.dataDirectory.child("mods/Essentials/")
        val daemon : ExecutorService = Executors.newCachedThreadPool()
        var connectType = false

        fun currentTime() : String {
            return LocalDateTime.now().format(DateTimeFormatter.ofPattern("YYYY-mm-dd a HH:mm:ss"))
        }
    }

    var webServer = WebServer()

    init {
        Log.info("[Essentials] Loading")

        createFile()
        if (!root.child("config.txt").exists()) Config.save()
        Config.load()
        Config.update()
        database.open()
        Permission.load()
        PluginData.load()

        if (Config.database != root.child("database").absolutePath()) {
            Log.info(Bundle()["event.database.remote"])
        }

        if (Config.blockIP) {
            val os = System.getProperty("os.name").lowercase(Locale.getDefault())
            if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                if (System.getenv("sudopassword") == null) {
                    Log.info(bundle["config.sudopassword"])
                    Log.info(bundle["config.sudopassword.repeat"])

                    val sc = Scanner(System.`in`)
                    val co = System.console()

                    // 시스템이 Console 를 지원 안할경우 (비밀번호 노출됨)
                    print(bundle["config.sudopassword.password"] + " ")
                    if (co == null) {
                        PluginData.sudoPassword = sc.nextLine()
                    } else {
                        PluginData.sudoPassword = String(co.readPassword())
                    }
                    Log.info(bundle["config.sudopassword.no-check"])
                } else {
                    PluginData.sudoPassword = System.getenv("sudopassword")
                }
            } else {
                Config.blockIP = false
                Log.warn(bundle["config.blockIP.unsupported"])
            }
        }

        if (Config.antiVPN) {
            if (!root.child("data/ipv4.txt").exists()) {
                root.child("data").mkdirs()
                var isUpdate = false

                if (PluginData["vpnListDate"] == null) {
                    PluginData.status.put("vpnListDate", System.currentTimeMillis().toString())
                    isUpdate = true
                } else if ((PluginData["vpnListDate"]!!.toLong() + 8.64e+7) < System.currentTimeMillis()) {
                    PluginData.status.put("vpnListDate", System.currentTimeMillis().toString())
                    isUpdate = true
                }
                PluginData.save(false)

                if (isUpdate) {
                    URL("https://raw.githubusercontent.com/X4BNet/lists_vpn/main/output/datacenter/ipv4.txt").openStream().use { b ->
                        BufferedInputStream(b).use { bis ->
                            FileOutputStream(root.child("data/ipv4.txt").absolutePath()).use { fos ->
                                val data = ByteArray(1024)
                                var count : Int
                                while (bis.read(data, 0, 1024).also { count = it } != -1) {
                                    fos.write(data, 0, count)
                                }
                            }
                        }
                    }
                }

                root.child("data/ipv4.txt").file().forEachLine {
                    PluginData.vpnList.add(it)
                }
            }
        }
        Event.register()
    }

    override fun init() {
        Log.info(Bundle()["event.plugin.starting"])
        val isPortOpen = try {
            ServerSocket(6000).use { _ -> true }
        } catch (e : IOException) {
            false
        }

        daemon.submit(FileWatchService)
        daemon.submit(Trigger.Thread())
        daemon.submit(Trigger.UpdateThread)
        daemon.submit(if (isPortOpen) Trigger.Server else Trigger.Client)
        if (Config.webServer) webServer.start()
        connectType = isPortOpen

        if (Config.update) {
            Http.get("https://api.github.com/repos/kieaer/Essentials/releases/latest").timeout(1000).error { _ -> Log.warn(bundle["event.plugin.update.check.failed"]) }.submit {
                if (it.status == Http.HttpStatus.OK) {
                    val json = JsonValue.readJSON(it.resultAsString).asObject()
                    mods.list().forEach { mod ->
                        if (mod.meta.name == "Essentials") {
                            PluginData.pluginVersion = mod.meta.version
                            return@forEach
                        }
                    }
                    val latest = DefaultArtifactVersion(json.getString("tag_name", PluginData.pluginVersion))
                    val current = DefaultArtifactVersion(PluginData.pluginVersion)

                    when {
                        latest > current -> Log.info(bundle["config.update.new", json["assets"].asArray()[0].asObject().get("browser_download_url").asString(), json.get("body").asString()])
                        latest.compareTo(current) == 0 -> Log.info(bundle["config.update.current"])
                        latest < current -> Log.info(bundle["config.update.devel"])
                    }
                }
            }
        } else {
            mods.list().forEach { mod ->
                if (mod.meta.name == "Essentials") {
                    PluginData.pluginVersion = mod.meta.version
                    return@forEach
                }
            }
        }

        netServer.admins.addActionFilter { e ->
            if (e.player == null) return@addActionFilter true
            val data = database.players.find { it.uuid == e.player.uuid() }
            val isHub = PluginData["hubMode"]
            PluginData.warpBlocks.forEach {
                if (e.tile != null) {
                    if (it.mapName == state.map.name() && it.x.toShort() == e.tile.x && it.y.toShort() == e.tile.y && it.tileName == e.tile.block().name) {
                        return@addActionFilter false
                    }
                }
            }

            if (state.rules.pvp && Config.pvpAutoTeam && e.player.team() == Team.derelict) {
                return@addActionFilter false
            }

            if (data != null) {
                return@addActionFilter (isHub != null && isHub == state.map.name() && Permission.check(data, "hub.build")) || isHub == null && !data.strict
            } else {
                return@addActionFilter false
            }
        }

        if (Config.blockfooclient) {
            val fooArray = arrayOf("fooCheck", "fooTransmission", "fooTransmissionEnabled")
            fooArray.forEach {
                netServer.addPacketHandler(it) { packet, _ ->
                    packet.kick(Bundle(packet.locale)["event.antigrief.foo"])
                    Log.info(Bundle()["event.antigrief.foo.log", packet.plainName(), "Packet"])
                    Events.fire(CustomEvents.PlayerBanned(packet.plainName(), packet.uuid(), currentTime(), "foo"))
                }
            }
        }

        Core.app.addListener(object: ApplicationListener {
            override fun dispose() {
                if (connectType) {
                    Trigger.clients.forEach {
                        val writer = BufferedWriter(OutputStreamWriter(it.getOutputStream()))
                        try {
                            writer.write("exit")
                            writer.newLine()
                            writer.flush()
                            it.close()
                        } catch (e : SocketException) {
                            it.close()
                            Trigger.clients.remove(it)
                        }
                    }
                    Trigger.Server.shutdown()
                } else {
                    Trigger.Client.send("exit")
                }
                daemon.shutdownNow()
                Permission.sort()
                Config.save()
                database.close()
                webServer.stop()
                if (Config.webServer) webServer.stop()
            }
        })

        if (!Fi(Config.banList).exists()) {
            val data = JsonArray()
            netServer.admins.banned.forEach {
                val json = JsonObject()
                json.add("id", it.id)

                val ips = JsonArray()
                for (a in it.ips) {
                    ips.add(a)
                }
                json.add("ip", ips)

                val names = JsonArray()
                for (a in it.names) {
                    names.add(a)
                }

                json.add("name", names)
            }

            Fi(Config.banList).writeString(data.toString(Stringify.HJSON))
        } else {
            netServer.admins.playerInfo.values().forEach(Consumer { info : Administration.PlayerInfo -> info.banned = false })
            for (bans in JsonArray.readHjson(Fi(Config.banList).readString()).asArray()) {
                val data = bans.asObject()
                val id = data.get("id").asString()
                val ips = data.get("ip").asArray()
                netServer.admins.playerInfo.values().find { a -> a.id == id }?.banned = true
                for (ip in ips) {
                    netServer.admins.playerInfo.values().find { a -> a.lastIP == ip.asString() }?.banned = true
                }
            }

            netServer.admins.save()
        }

        Log.info(Bundle()["event.plugin.loaded"])
    }

    override fun registerClientCommands(handler : CommandHandler) {
        Commands(handler, true)

        if (root.child("bannedCommands.txt").exists()) {
            val json = JsonArray.readHjson(root.child("bannedCommands.txt").readString())
            for (command in json.asArray()) {
                handler.removeCommand(command.asString())
            }
        } else {
            root.child("bannedCommands.txt").writeString("[]")
        }
    }

    override fun registerServerCommands(handler : CommandHandler) {
        Commands(handler, false)

        if (root.child("bannedCommands.txt").exists()) {
            val json = JsonArray.readHjson(root.child("bannedCommands.txt").readString())
            for (command in json.asArray()) {
                handler.removeCommand(command.asString())
            }
        } else {
            root.child("bannedCommands.txt").writeString("[]")
        }
    }

    private fun createFile() {
        if (!root.child("chat_blacklist.txt").exists()) {
            root.child("chat_blacklist.txt").writeString("않")
        }
    }
}