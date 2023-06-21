package essentials

import arc.ApplicationListener
import arc.Core
import arc.files.Fi
import arc.util.CommandHandler
import arc.util.Http
import arc.util.Log
import essentials.Permission.bundle
import mindustry.Vars
import mindustry.mod.Plugin
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.hjson.JsonArray
import org.hjson.JsonValue
import org.hjson.Stringify
import java.io.*
import java.net.ServerSocket
import java.net.SocketException
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Main: Plugin() {
    companion object {
        val database = DB()
        val root : Fi = Core.settings.dataDirectory.child("mods/Essentials/")
        val daemon : ExecutorService = Executors.newCachedThreadPool()
        var connectType = false
    }

    var webServer = WebServer()

    init {
        Log.info("[Essentials] Loading")
        if((Core.settings.has("debugMode") && Core.settings.getBool("debugMode"))) {
            root.child("database.mv.db").delete()
        }

        createFile()
        if(!root.child("config.txt").exists()) Config.save()
        Config.load()
        Config.update()
        database.open()
        Permission.load()
        PluginData.load()

        if(Config.database != root.child("database").absolutePath()) {
            Log.info(Bundle()["event.database.remote"])
            root.child("database.mv.db").delete()
        }

        if(Config.blockIP) {
            val os = System.getProperty("os.name").lowercase(Locale.getDefault())
            if(os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                if(System.getenv("sudopassword") == null) {
                    Log.info(bundle["config.sudopassword"])
                    Log.info(bundle["config.sudopassword.repeat"])

                    val sc = Scanner(System.`in`)
                    val co = System.console()

                    // 시스템이 Console 를 지원 안할경우 (비밀번호 노출됨)
                    print(bundle["config.sudopassword.password"] + " ")
                    if(co == null) {
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

        if(Config.antiVPN) {
            if(!root.child("data/ipv4.txt").exists()) {
                root.child("data").mkdirs()
                var isUpdate = false

                if(PluginData["vpnListDate"] == null) {
                    PluginData.status.put("vpnListDate", System.currentTimeMillis().toString())
                    isUpdate = true
                } else if((PluginData["vpnListDate"]!!.toLong() + 8.64e+7) < System.currentTimeMillis()) {
                    PluginData.status.put("vpnListDate", System.currentTimeMillis().toString())
                    isUpdate = true
                }
                PluginData.save(false)
                PluginData.changed = true

                if(isUpdate) {
                    URL("https://github.com/X4BNet/lists_vpn/blob/main/output/datacenter/ipv4.txt").openStream().use { b ->
                        BufferedInputStream(b).use { bis ->
                            FileOutputStream(root.child("data/ipv4.txt").absolutePath()).use { fos ->
                                val data = ByteArray(1024)
                                var count : Int
                                while(bis.read(data, 0, 1024).also { count = it } != -1) {
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
        } catch(e : IOException) {
            false
        }

        daemon.submit(FileWatchService)
        daemon.submit(Trigger.Thread())
        daemon.submit(Trigger.UpdateThread)
        daemon.submit(if(isPortOpen) Trigger.Server else Trigger.Client)
        if(Config.webServer) webServer.start()
        connectType = isPortOpen
        if(Config.botToken.isNotEmpty() && Config.channelToken.isNotEmpty()) Commands.Discord.start()

        if(Config.update) {
            Http.get("https://api.github.com/repos/kieaer/Essentials/releases/latest").timeout(1000).error { _ -> Log.warn(bundle["event.plugin.update.check.failed"]) }.submit {
                if(it.status == Http.HttpStatus.OK) {
                    val json = JsonValue.readJSON(it.resultAsString).asObject()
                    Vars.mods.list().forEach { mod ->
                        if(mod.meta.name == "Essentials") {
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
            Vars.mods.list().forEach { mod ->
                if(mod.meta.name == "Essentials") {
                    PluginData.pluginVersion = mod.meta.version
                    return@forEach
                }
            }
        }

        Vars.netServer.admins.addActionFilter { e ->
            if(e.player == null) return@addActionFilter true
            val data = database.players.find { it.uuid == e.player.uuid() }
            val isHub = PluginData["hubMode"]
            PluginData.warpBlocks.forEach {
                if(e.tile != null) {
                    if(it.mapName == Vars.state.map.name() && it.x.toShort() == e.tile.x && it.y.toShort() == e.tile.y && it.tileName == e.tile.block().name) {
                        return@addActionFilter false
                    }
                }
            }

            if(data != null) {
                if(isHub != null && isHub == Vars.state.map.name()) {
                    return@addActionFilter Permission.check(e.player, "hub.build")
                } else {
                    return@addActionFilter true
                }
            }
            return@addActionFilter false
        }

        if(Config.blockfooclient) {
            val fooArray = arrayOf("fooCheck", "fooTransmission", "fooTransmissionEnabled")
            fooArray.forEach {
                Vars.netServer.addPacketHandler(it) { packet, _ ->
                    packet.kick(Bundle(packet.locale)["event.antigrief.foo"])
                    Log.info(Bundle()["event.antigrief.foo.log"])
                }
            }
        }

        Core.app.addListener(object: ApplicationListener {
            override fun dispose() {
                if(connectType) {
                    Trigger.clients.forEach {
                        val writer = BufferedWriter(OutputStreamWriter(it.getOutputStream()))
                        try {
                            writer.write("exit")
                            writer.newLine()
                            writer.flush()
                            it.close()
                        } catch(e : SocketException) {
                            it.close()
                            Trigger.clients.remove(it)
                        }
                    }
                    Trigger.Server.shutdown()
                } else {
                    Trigger.Client.send("exit")
                }
                daemon.shutdownNow()
                Commands.Discord.shutdownNow()
                Permission.sort()
                Config.save()
                database.close()
                webServer.stop()
                if(database.dbServer != null) database.dbServer!!.stop()
                if(Config.webServer) webServer.stop()
            }
        })

        if(!Config.ipBanList.exists()) {
            val data = JsonArray()
            Vars.netServer.admins.banned.forEach {
                it.ips.forEach { ip ->
                    data.add(ip)
                }
            }
            Config.ipBanList.writeString(data.toString(Stringify.HJSON))
        }

        if(!Config.idBanList.exists()) {
            val data = JsonArray()
            Vars.netServer.admins.banned.forEach {
                data.add(it.id)
            }
            Config.idBanList.writeString(data.toString(Stringify.HJSON))
        }

        Vars.netServer.admins.bannedIPs.clear()
        Vars.netServer.admins.banned.clear()

        Log.info(Bundle()["event.plugin.loaded"])
    }

    override fun registerClientCommands(handler : CommandHandler) {
        Commands(handler, true)
    }

    override fun registerServerCommands(handler : CommandHandler) {
        Commands(handler, false)
    }

    private fun createFile() {
        if(!root.child("motd").exists()) {
            root.child("motd").mkdirs()
            val names = arrayListOf("en", "ko")
            val texts = arrayListOf("To edit this message, open [green]config/mods/Essentials/motd[] folder and edit [green]en.txt[]", "이 메세지를 수정할려면 [green]config/mods/Essentials/motd[] 폴더에서 [green]ko.txt[] 파일을 수정하세요.")
            names.forEachIndexed { index, _ ->
                if(!root.child("motd/${names[index]}.txt").exists()) {
                    root.child("motd/${names[index]}.txt").writeString(texts[index])
                }
            }
        }

        if(!root.child("messages").exists()) {
            root.child("messages").mkdirs()
            val names = arrayListOf("en", "ko")
            val texts = arrayListOf("To edit this message, open [green]config/mods/Essentials/messages[] folder and edit [green]en.txt[]", "이 메세지를 수정할려면 [green]config/mods/Essentials/messages[] 폴더에서 [green]ko.txt[] 파일을 수정하세요.")
            names.forEachIndexed { index, _ ->
                if(!root.child("messages/${names[index]}.txt").exists()) {
                    root.child("messages/${names[index]}.txt").writeString(texts[index])
                }
            }
        }

        if(!root.child("chat_blacklist.txt").exists()) {
            root.child("chat_blacklist.txt").writeString("않")
        }
    }
}