package essentials

import arc.ApplicationListener
import arc.Core
import arc.files.Fi
import arc.util.CommandHandler
import arc.util.Http
import arc.util.Log
import essentials.Permission.bundle
import mindustry.Vars
import mindustry.gen.Call
import mindustry.mod.Plugin
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.hjson.JsonValue
import java.io.*
import java.net.ServerSocket
import java.net.SocketException
import java.net.URL
import java.util.*
import java.util.concurrent.Executors

class Main: Plugin() {
    companion object {
        val database = DB()
        val root : Fi = Core.settings.dataDirectory.child("mods/Essentials/")
        val daemon = Executors.newCachedThreadPool()
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
                        for(a in 0 until Vars.mods.list().size) {
                            if(Vars.mods.list()[a].meta.name == "Essentials") {
                                PluginData.pluginVersion = Vars.mods.list()[a].meta.version
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
            for(a in 0 until Vars.mods.list().size) {
                if(Vars.mods.list()[a].meta.name == "Essentials") {
                    PluginData.pluginVersion = Vars.mods.list()[a].meta.version
                    break
                }
            }
        }

        Vars.netServer.admins.addActionFilter { e ->
            if(e.player == null) return@addActionFilter true
            val data = database.players.find { it.uuid == e.player.uuid() }
            val isHub = PluginData["hubMode"]
            for(a in PluginData.warpBlocks) {
                if(e.tile != null) {
                    if(a.mapName == Vars.state.map.name() && a.x.toShort() == e.tile.x && a.y.toShort() == e.tile.y && a.tileName == e.tile.block().name) {
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

        if (Config.blockfooclient) {
            val fooArray = arrayOf("fooCheck", "fooTransmission", "fooTransmissionEnabled")

            for(a in fooArray) {
                Vars.netServer.addPacketHandler(a) { b, _ ->
                    b.kick("Custom client detected")
                    Call.sendMessage("${b.name()}[white] kicked method 3")
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
            for(a in 0 until names.size) {
                if(!root.child("motd/${names[a]}.txt").exists()) {
                    root.child("motd/${names[a]}.txt").writeString(texts[a])
                }
            }
        }

        if(!root.child("messages").exists()) {
            root.child("messages").mkdirs()
            val names = arrayListOf("en", "ko")
            val texts = arrayListOf("To edit this message, open [green]config/mods/Essentials/messages[] folder and edit [green]en.txt[]", "이 메세지를 수정할려면 [green]config/mods/Essentials/messages[] 폴더에서 [green]ko.txt[] 파일을 수정하세요.")
            for(a in 0 until names.size) {
                if(!root.child("messages/${names[a]}.txt").exists()) {
                    root.child("messages/${names[a]}.txt").writeString(texts[a])
                }
            }
        }

        if(!root.child("chat_blacklist.txt").exists()) {
            root.child("chat_blacklist.txt").writeString("않")
        }
    }
}