package essentials

import arc.ApplicationListener
import arc.Core
import arc.files.Fi
import arc.util.CommandHandler
import essentials.command.ClientCommand
import essentials.command.ServerCommand
import essentials.data.Config
import essentials.data.DB
import essentials.data.PlayerCore
import essentials.event.Event
import essentials.event.feature.AutoRollback
import essentials.event.feature.Discord
import essentials.event.feature.Permissions
import essentials.event.feature.RainbowName
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.internal.PluginException
import essentials.internal.Tool
import essentials.network.Client
import essentials.network.Server
import essentials.thread.PermissionWatch
import essentials.thread.TriggerThread
import essentials.thread.WarpBorder
import mindustry.Vars.netServer
import mindustry.core.Version
import mindustry.mod.Plugin
import org.hjson.JsonValue
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.zip.ZipFile
import kotlin.system.exitProcess

class Main : Plugin() {
    companion object {
        val timer = Timer()
        val mainThread: ExecutorService = Executors.newCachedThreadPool()
        val pluginRoot: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
    }

    init {
        //checkServerVersion() // Temporary disabled
        fileExtract()

        // 서버 로비기능 설정
        if (!Core.settings.has("isLobby")) {
            Core.settings.put("isLobby", false)
            Core.settings.saveValues()
        } else if (Core.settings.getBool("isLobby")) {
            Log.info("system.lobby")
            Log.info("Lobby server can only be built by admins!") //TODO 언어별 추가
        }

        // 설정 불러오기
        Config.createFile()
        Log.info("config.language", Config.locale.displayLanguage)

        // 플러그인 데이터 불러오기
        PluginData.loadAll()

        // 플레이어 권한 목록 불러오기
        Permissions.reload(true)

        // 스레드 시작
        mainThread.submit(TriggerThread)
        mainThread.submit(Threads)
        mainThread.submit(RainbowName)
        timer.scheduleAtFixedRate(AutoRollback, Config.saveTime.toSecondOfDay().toLong(), Config.saveTime.toSecondOfDay().toLong())
        mainThread.submit(PermissionWatch)
        mainThread.submit(WarpBorder)

        // DB 연결
        DB.start()

        // 네트워크 연결
        if (Config.networkMode == Config.NetworkMode.Server) {
            mainThread.submit(Server)
        } else if (Config.networkMode == Config.NetworkMode.Client){
            mainThread.submit(Client)
            Client.wakeup()
        }

        // 기록 시작
        // if (Config.logging) ActivityLog()

        // 이벤트 시작
        Event.register()

        //WebServer.main()

        // 서버 종료 이벤트 설정
        Core.app.addListener(object : ApplicationListener {
            override fun dispose() {
                try {
                    Discord.shutdownNow() // Discord 서비스 종료
                    PlayerCore.saveAll() // 플레이어 데이터 저장
                    PluginData.saveAll() // 플러그인 데이터 저장
                    WarpBorder.interrupt() // 서버간 이동 영역표시 종료
                    mainThread.shutdownNow() // 스레드 종료
                    // config.singleService.shutdownNow(); // 로그 스레드 종료
                    timer.cancel() // 일정 시간마다 실행되는 스레드 종료
                    // 투표 종료
                    PluginData.votingClass?.interrupt()
                    DB.stop()
                    if (Config.networkMode == Config.NetworkMode.Server) {
                        val servers = Server.list.iterator()
                        while (servers.hasNext()) {
                            val ser = servers.next()
                            if (ser != null) {
                                ser.os.close()
                                ser.br.close()
                                ser.socket.close()
                            }
                        }
                        Server.shutdown()
                        Log.info("server-thread-disabled")
                    }

                    // 클라이언트 종료
                    if (Config.networkMode == Config.NetworkMode.Client && Client.activated) {
                        Client.request(Client.Request.Exit, null, null)
                        Log.info("client.shutdown")
                    }

                    if (Server.isSocketInitialized() || Client.socket.isClosed || WarpBorder.isInterrupted || !PlayerCore.conn.isClosed) {
                        Log.info("thread-disable-waiting")
                    } else {
                        Log.warn("thread-not-dead")
                    }
                } catch (e: Exception) {
                    CrashReport(e)
                    exitProcess(1) // 오류로 인한 강제 종료
                }
            }
        })

        PluginData.serverIP = Tool.hostIP()
    }

    override fun init() {
        Tool.ipre.IPDatabasePath = pluginRoot.child("data/IP2LOCATION-LITE-DB1.BIN").absolutePath()
        Tool.ipre.UseMemoryMappedFile = true

        // 채팅 포맷 변경
        netServer.admins.addChatFilter { _, _ -> null }

        // 비 로그인 유저 통제
        netServer.admins.addActionFilter { e ->
            if (e.player == null) return@addActionFilter true
            return@addActionFilter PluginData[e.player.uuid()] != null
        }
    }

    override fun registerServerCommands(handler: CommandHandler) {
        ServerCommand.register(handler)
    }

    override fun registerClientCommands(handler: CommandHandler) {
        ClientCommand.register(handler)
    }

    private fun checkServerVersion(){
        javaClass.getResourceAsStream("/plugin.json").use { reader ->
            BufferedReader(InputStreamReader(reader)).use { br ->
                val version = JsonValue.readJSON(br).asObject()["version"].asString()
                if (Version.build != PluginData.buildVersion && Version.revision >= PluginData.buildRevision) {
                    throw PluginException("Essentials " + version + " plugin only works with Build " + PluginData.buildVersion + "." + PluginData.buildRevision + " or higher.")
                }
                PluginData.pluginVersion = version
            }
        }
    }

    private fun fileExtract(){
        try {
            if(pluginRoot.child("data/IP2LOCATION-LITE-DB1.BIN.ZIP").exists()) {
                ZipFile(pluginRoot.child("data/IP2LOCATION-LITE-DB1.BIN.ZIP").absolutePath()).use { zip ->
                    zip.entries().asSequence().forEach { entry ->
                        if (entry.isDirectory) {
                            File(pluginRoot.child("data").absolutePath(), entry.name).mkdirs()
                        } else {
                            zip.getInputStream(entry).use { input ->
                                File(pluginRoot.child("data").absolutePath(), entry.name).outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
                }
                pluginRoot.child("data/IP2LOCATION-LITE-DB1.BIN.ZIP").delete()
                pluginRoot.child("data/LICENSE-CC-BY-SA-4.0.TXT").delete()
                pluginRoot.child("data/README_LITE.TXT").delete()
            }
        } catch (e: IOException) {
            throw PluginException(e)
        }
    }
}