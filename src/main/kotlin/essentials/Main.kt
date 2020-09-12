package essentials

import arc.ApplicationListener
import arc.Core
import arc.files.Fi
import arc.util.CommandHandler
import arc.util.async.Threads.sleep
import essentials.command.ClientCommander
import essentials.command.ServerCommander
import essentials.features.*
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.internal.PluginException
import essentials.internal.Tool
import essentials.network.Client
import essentials.network.Server
import essentials.thread.*
import mindustry.Vars.netServer
import mindustry.core.Version
import mindustry.mod.Plugin
import org.hjson.JsonValue
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.sql.SQLException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import kotlin.system.exitProcess

class Main : Plugin() {
    companion object {
        var companion: Companion = Companion
        val timer = Timer()
        val pluginVars = PluginVars()
        val playerCore = PlayerCore()
        val pluginData = PluginData()
        val server = Server()
        val client = Client()
        val discord = Discord()
        val rollback = AutoRollback()
        val warpBorder = WarpBorder()
        val vars = PluginVars()
        val vote = Vote()

        val tool = Tool()
        val configs = Config()
        val colorNickname = ColorNickname()
        val perm = Permissions()

        val mainThread: ExecutorService = ThreadPoolExecutor(0, 10, 10L, TimeUnit.SECONDS, SynchronousQueue())
        val pluginRoot: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
        var listener: ApplicationListener? = null
    }

    init {
        // 서버 버전 확인
        javaClass.getResourceAsStream("/plugin.json").use { reader ->
            BufferedReader(InputStreamReader(reader)).use { br ->
                val version = JsonValue.readJSON(br).asObject()["version"].asString()
                if (Version.build != pluginVars.buildVersion && Version.revision >= pluginVars.buildRevision) {
                    throw PluginException("Essentials " + version + " plugin only works with Build " + pluginVars.buildVersion + "." + pluginVars.buildRevision + " or higher.")
                }
                pluginVars.pluginVersion = version
            }
        }

        // 파일 압축해제
        try {
            JarFile(File(Core.settings.dataDirectory.child("mods/Essentials.jar").absolutePath())).use { jar ->
                val enumEntries = jar.entries()
                while (enumEntries.hasMoreElements()) {
                    val file = enumEntries.nextElement()
                    val renamed = file.name.replace("config_folder/", "")
                    if (file.name.startsWith("config_folder") && !pluginRoot.child(renamed).exists()) {
                        if (file.isDirectory) {
                            pluginRoot.child(renamed).file().mkdir()
                            continue
                        }
                        jar.getInputStream(file).use { i -> pluginRoot.child(renamed).write(i, false) }
                    }
                }
            }
        } catch (e: IOException) {
            throw PluginException(e)
        }

        // 서버 로비기능 설정
        if (!Core.settings.has("isLobby")) {
            Core.settings.put("isLobby", false)
            Core.settings.saveValues()
        } else if (Core.settings.getBool("isLobby")) {
            Log.info("system.lobby")
            Log.info("Lobby server can only be built by admins!") //TODO 언어별 추가
        }

        // 설정 불러오기
        configs.init()
        Log.info("config.language", configs.language.displayLanguage)

        // 플러그인 데이터 불러오기
        pluginData.loadAll()

        // 플레이어 권한 목록 불러오기
        perm.reload(true)

        // 스레드 시작
        TickTrigger()
        mainThread.submit(Threads())
        mainThread.submit(colorNickname)
        if (configs.rollback) timer.scheduleAtFixedRate(rollback, configs.saveTime.toSecondOfDay().toLong(), configs.saveTime.toSecondOfDay().toLong())
        mainThread.submit(PermissionWatch())
        mainThread.submit(warpBorder)
        mainThread.submit(vote)

        // DB 연결
        try {
            playerCore.connect(configs.dbServer)
            playerCore.create()
            playerCore.update()
        } catch (e: SQLException) {
            CrashReport(e)
        }

        // Server 시작
        if (configs.serverEnable) mainThread.submit(server)

        // Client 연결
        if (configs.clientEnable) {
            if(configs.serverEnable) sleep(1000)
            mainThread.submit(client)
            client.wakeup()
        }

        // 기록 시작
        // if (configs.logging) ActivityLog()

        // 이벤트 시작
        Event.register()

        // 서버 종료 이벤트 설정
        listener = object : ApplicationListener {
            override fun dispose() {
                try {
                    discord.shutdownNow() // Discord 서비스 종료
                    playerCore.saveAll() // 플레이어 데이터 저장
                    pluginData.saveAll() // 플러그인 데이터 저장
                    warpBorder.interrupt() // 서버간 이동 영역표시 종료
                    mainThread.shutdownNow() // 스레드 종료
                    // config.singleService.shutdownNow(); // 로그 스레드 종료
                    timer.cancel() // 일정 시간마다 실행되는 스레드 종료
                    // 투표 종료
                    vote.interrupt()
                    playerCore.dispose() // DB 연결 종료
                    if (configs.serverEnable) {
                        val servers = server.list.iterator()
                        while (servers.hasNext()) {
                            val ser = servers.next()
                            if (ser != null) {
                                ser.os.close()
                                ser.br.close()
                                ser.socket.close()
                            }
                        }
                        server.shutdown()
                        Log.info("server-thread-disabled")
                    }

                    // 클라이언트 종료
                    if (configs.clientEnable && client.activated) {
                        client.request(Client.Request.Exit, null, null)
                        Log.info("client.shutdown")
                    }

                    if (server.serverSocket.isClosed || client.socket.isClosed || warpBorder.isInterrupted || !playerCore.conn.isClosed) {
                        Log.info("thread-disable-waiting")
                    } else {
                        Log.warn("thread-not-dead")
                    }
                } catch (e: Exception) {
                    CrashReport(e)
                    exitProcess(1) // 오류로 인한 강제 종료
                }
            }
        }
        Core.app.addListener(listener)

        pluginVars.serverIP = tool.hostIP()
    }

    override fun init() {
        tool.ipre.IPDatabasePath = pluginRoot.child("data/IP2LOCATION-LITE-DB1.BIN").absolutePath()
        tool.ipre.UseMemoryMappedFile = true

        // 채팅 포맷 변경
        netServer.admins.addChatFilter { _, _ -> null }

        // 비 로그인 유저 통제
        netServer.admins.addActionFilter { e ->
            if (e.player == null) return@addActionFilter true
            return@addActionFilter playerCore[e.player.uuid()].login
        }
    }

    override fun registerServerCommands(handler: CommandHandler) {
        ServerCommander.register(handler)
    }

    override fun registerClientCommands(handler: CommandHandler) {
        ClientCommander.register(handler)
    }
}