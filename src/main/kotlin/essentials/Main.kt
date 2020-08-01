package essentials

import arc.ApplicationListener
import arc.Core
import arc.files.Fi
import arc.math.Mathf
import arc.util.CommandHandler
import arc.util.Strings
import arc.util.Time
import essentials.external.StringUtils
import essentials.features.*
import essentials.internal.*
import essentials.network.Client
import essentials.network.Server
import essentials.thread.*
import mindustry.Vars
import mindustry.Vars.world
import mindustry.content.Blocks
import mindustry.content.Mechs
import mindustry.core.Version
import mindustry.entities.type.BaseUnit
import mindustry.entities.type.Player
import mindustry.game.Difficulty
import mindustry.game.Gamemode
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.io.SaveIO
import mindustry.maps.Map
import mindustry.net.Packets
import mindustry.plugin.Plugin
import mindustry.world.Tile
import org.hjson.JsonObject
import org.hjson.JsonValue
import org.mindrot.jbcrypt.BCrypt
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.sql.SQLException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.jar.JarFile
import kotlin.math.roundToLong
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
            Core.settings.putSave("isLobby", false)
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

        // Client 연결
        if (configs.clientEnable) mainThread.submit(client)

        // Server 시작
        if (configs.serverEnable) mainThread.submit(server)

        // 기록 시작
        // if (configs.logging) ActivityLog()

        // 이벤트 시작
        Event()

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
                            servers.remove()
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

    }

    override fun registerServerCommands(handler: CommandHandler) {
        handler.register("lobby", "Toggle lobby server features") {
            Core.settings.putSave("isLobby", !Core.settings.getBool("isLobby"))
            Log.info("success")
        }
        handler.register("edit", "<uuid> <name> [value]", "Edit PlayerData directly") { arg: Array<String> ->
            val sql = "UPDATE players SET " + arg[1] + "=? WHERE uuid=?"
            try {
                playerCore.conn.prepareStatement(sql).use { pstmt ->
                    pstmt.setString(1, arg[2])
                    pstmt.setString(2, arg[0])
                    val playerData = playerCore[arg[0]]
                    val player = Vars.playerGroup.find { p: Player -> p.uuid == arg[0] }
                    if (!playerData.error) {
                        playerCore.save(playerData)
                        playerData.toData(playerData.toMap().set(arg[1], arg[2]))
                        perm.user[playerData.uuid].asObject()[arg[1]] = arg[2]
                        perm.saveAll()
                    }
                    val count = pstmt.executeUpdate()
                    if (count < 1 && !playerData.error) {
                        Log.info("success")
                        vars.removePlayerData(playerData)
                        vars.players.remove(player)
                        playerCore.playerLoad(player, null)
                        player.sendMessage(Bundle(playerData.locale)["player.reloaded"])
                    } else {
                        Log.info("failed")
                    }
                }
            } catch (e: SQLException) {
                CrashReport(e)
            }
        }
        handler.register("saveall", "desc") { pluginData.saveAll() }
        handler.register("gendocs", "Generate Essentials README.md") {
            val servercommands = arrayOf(
                    "help", "version", "exit", "stop", "host", "maps", "reloadmaps", "status",
                    "mods", "mod", "js", "say", "difficulty", "rules", "fillitems", "playerlimit",
                    "config", "subnet-ban", "whitelisted", "whitelist-add", "whitelist-remove",
                    "shuffle", "nextmap", "kick", "ban", "bans", "unban", "admin", "unadmin",
                    "admins", "runwave", "load", "save", "saves", "gameover", "info", "search", "gc",
                    "pardon", "players", "gendocs", "cha"
            )
            val clientcommands = arrayOf(
                    "help", "t", "sync", "pardon", "players", "votekick"
            )
            val serverdoc = "## Server commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
            val clientdoc = "## Client commands\n\n| Command | Parameter | Description |\n|:---|:---|:--- |\n"
            val gentime = """
                
                README.md Generated time: ${DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now())}
                """.trimIndent()
            Log.info("readme-generating")
            val header = """
                [![SonarCloud Coverage](https://sonarcloud.io/api/project_badges/measure?project=Kieaer_Essentials&metric=coverage)](https://sonarcloud.io/component_measures/metric/coverage/list?id=Kieaer_Essentials) [![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Kieaer_Essentials&metric=alert_status)](https://sonarcloud.io/dashboard?id=Kieaer_Essentials)
                # Essentials
                Add more commands to the server.
                
                I'm getting a lot of suggestions.<br>
                Please submit your idea to this repository issues or Mindustry official discord!
                
                ## Essentials 11 Plans
                - [ ] Fix bugs
                - [ ] Voting not working
                  - [ ] Sometimes an account system not working
                - [ ] Fix many typos
                - [ ] Features separation
                  - [ ] Features separation
                    - [x] Rest API [Plugin Link](https://github.com/Kieaer/Essential-REST_API) 
                        - [x] Information 
                        - [x] Add players detail information 
                        - [x] Add a gamemode 
                        - [x] Add other team core resource status 
                        - [x] Add a server map list
                    - [ ] Communication
                      - [ ] Communicate a chat message to server
                        - [ ] Send
                        - [ ] Receive
                  - [ ] Web server
                    - [ ] Fix a sometimes ranking site not loaded
                  - [x] Auto Rollback (Not remove) [Plugin Link](https://github.com/Kieaer/AutoRollback)
                - [ ] New features
                  - [ ] Web console
                    - [ ] Control plugin database
                    - [ ] Check world status
                      - [ ] Dynmap (idea from Minecraft)
                      - [ ] Rest API
                - [ ] Remove external API services
                  - [x] IP API (Due to traffic excess)
                  - [ ] Translate (Due to paid service)
                - [ ] Security patches
                - [ ] All code clean
                
                ### Recommend
                CPU: Ryzen 3 2200G or Intel i3 8100<br>
                RAM: 50MB<br>
                Disk: HDD capable of more than 5MB/s random read/write.
                
                ## Installation
                
                Put this plugin in the ``<server folder location>/config/mods`` folder.
                
                
                """.trimIndent()
            var tempbuild = StringBuilder()
            for (a in 0 until Vars.netServer.clientCommands.commandList.size) {
                val command = Vars.netServer.clientCommands.commandList[a]
                var dup = false
                for (`as` in clientcommands) {
                    if (command.text == `as`) {
                        dup = true
                        break
                    }
                }
                if (!dup) {
                    val temp = """| ${command.text} | ${StringUtils.encodeHtml(command.paramText)} | ${command.description} |
"""
                    tempbuild.append(temp)
                }
            }
            val tmp = """
                $header$clientdoc$tempbuild
                
                """.trimIndent()
            tempbuild = StringBuilder()
            for (command in handler.commandList) {
                var dup = false
                for (`as` in servercommands) {
                    if (command.text == `as`) {
                        dup = true
                        break
                    }
                }
                if (!dup) {
                    val temp = """| ${command.text} | ${StringUtils.encodeHtml(command.paramText)} | ${command.description} |
"""
                    tempbuild.append(temp)
                }
            }
            pluginRoot.child("README.md").writeString(tmp + serverdoc + tempbuild.toString() + gentime)
            Log.info("success")
        }
        handler.register("admin", "<name>", "Set admin status to player.") { arg: Array<String> ->
            if (arg.isNotEmpty()) {
                val player = Vars.playerGroup.find { p: Player -> p.name == arg[0] }
                if (player == null) {
                    Log.warn("player.not-found")
                } else {
                    for (data in perm.perm) {
                        if (data.name == "newadmin") {
                            val p = playerCore[player.uuid]
                            p.permission = "newadmin"
                            player.isAdmin = perm.isAdmin(p)
                            Log.info("success")
                            break
                        }
                    }
                    //Log.warn("use-setperm");
                }
            } else {
                Log.warn("no-parameter")
            }
        }
        handler.register("bansync", "Synchronize ban list with server") {
            if (client.activated) {
                client.request(Client.Request.BanSync, null, null)
            } else {
                Log.client("client.disabled")
            }
        }

        handler.register<Any>("info", "<player/uuid>", "Show player information", object : CommandHandler.CommandRunner<Any?> {
            fun execute(uuid: String) {
                try {
                    playerCore.conn.prepareStatement("SELECT * from players WHERE uuid=?").use { pstmt ->
                        pstmt.setString(1, uuid)
                        pstmt.executeQuery().use { rs ->
                            if (rs.next()) {
                                var datatext = "${rs.getString("name")} Player information\n" +
                                        "=====================================\n" +
                                        "name: ${rs.getString("name")}\n" +
                                        "uuid: ${rs.getString("uuid")}\n" +
                                        "country: ${rs.getString("country")}\n" +
                                        "country_code: ${rs.getString("country_code")}\n" +
                                        "language: ${rs.getString("language")}\n" +
                                        "isAdmin: ${rs.getBoolean("isAdmin")}\n" +
                                        "placecount: ${rs.getInt("placecount")}\n" +
                                        "breakcount: ${rs.getInt("breakcount")}\n" +
                                        "killcount: ${rs.getInt("killcount")}\n" +
                                        "deathcount: ${rs.getInt("deathcount")}\n" +
                                        "joincount: ${rs.getInt("joincount")}\n" +
                                        "kickcount: ${rs.getInt("kickcount")}\n" +
                                        "level: ${rs.getInt("level")}\n" +
                                        "exp: ${rs.getInt("exp")}\n" +
                                        "reqexp: ${rs.getInt("reqexp")}\n" +
                                        "firstdate: ${tool.longToDateTime(rs.getLong("firstdate")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}\n" +
                                        "lastdate: ${tool.longToDateTime(rs.getLong("lastDate")).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}\n" +
                                        "lastplacename: ${rs.getString("lastplacename")}\n" +
                                        "lastbreakname: ${rs.getString("lastbreakname")}\n" +
                                        "lastchat: ${rs.getString("lastchat")}\n" +
                                        "playtime: ${tool.longToTime(rs.getLong("playtime"))}\n" +
                                        "attackclear: ${rs.getInt("attackclear")}\n" +
                                        "pvpwincount: ${rs.getInt("pvpwincount")}\n" +
                                        "pvplosecount: ${rs.getInt("pvplosecount")}\n" +
                                        "pvpbreakout: ${rs.getInt("pvpbreakout")}\n" +
                                        "reactorcount: ${rs.getInt("reactorcount")}\n" +
                                        "bantime: ${rs.getString("bantime")}\n" +
                                        "translate: ${rs.getBoolean("translate")}\n" +
                                        "crosschat: ${rs.getBoolean("crosschat")}\n" +
                                        "colornick: ${rs.getBoolean("colornick")}\n" +
                                        "connected: ${rs.getBoolean("connected")}\n" +
                                        "connserver: ${rs.getString("connserver")}\n" +
                                        "permission: ${rs.getString("permission")}\n" +
                                        "mute: ${rs.getBoolean("mute")}\n" +
                                        "alert: ${rs.getBoolean("alert")}\n" +
                                        "udid: ${rs.getLong("udid")}\n" +
                                        "accountid: ${rs.getString("accountid")}"

                                val current = playerCore[uuid]
                                if (!current.error) {
                                    datatext = "$datatext\n" +
                                            "== ${current.name} Player internal data ==\n" + " +" +
                                            "isLogin: ${current.login}\n" +
                                            "afk: ${tool.longToTime(current.afk)}\n" +
                                            "afk_x: ${current.x}\n" + " +" +
                                            "afk_y: ${current.y}"
                                }
                                Log.info(datatext)
                            } else {
                                Log.info("Player not found!")
                            }
                        }
                    }
                } catch (e: SQLException) {
                    CrashReport(e)
                }
            }

            override fun accept(strings: Array<String>, o: Any?) {
                val players = Vars.netServer.admins.findByName(strings[0])
                if (players.size != 0) {
                    for (p in players) {
                        execute(p.id)
                    }
                } else {
                    execute(strings[0])
                }
            }
        })
        // TODO 모든 권한 그룹 변경 만들기
        handler.register("setperm", "<player_name/uuid> <group>", "Set player permission") { arg: Array<String> ->
            val target = Vars.playerGroup.find { p: Player -> p.name == arg[0] }
            val bundle = Bundle()
            val playerData: PlayerData
            if (target == null) {
                Log.warn(bundle["player.not-found"])
                return@register
            }
            for (p in perm.perm) {
                if (p.name == arg[1]) {
                    playerData = playerCore[target.uuid]
                    playerData.permission = arg[1]
                    perm.user[playerData.uuid].asObject()["group"] = arg[1]
                    perm.update(true)
                    perm.reload(false)
                    target.isAdmin = perm.isAdmin(playerData)
                    Log.info(bundle["success"])
                    target.sendMessage(Bundle(playerCore[target.uuid].locale).prefix("perm-changed"))
                    return@register
                }
            }
            Log.warn(bundle["perm-group-not-found"])
        }
        handler.register("reload", "Reload Essential plugin data") {
            perm.reload(false)
            perm.update(false)
            Log.info("plugin-reloaded")
        }
    }

    override fun registerClientCommands(handler: CommandHandler) {
        handler.removeCommand("votekick")
        //handler.removeCommand("t");
        handler.register("alert", "Turn on/off alerts") { _: Array<String?>?, player: Player ->
            if (!perm.check(player, "alert")) return@register
            val playerData = playerCore[player.uuid]
            if (playerData.alert) {
                playerData.alert = false
                player.sendMessage(Bundle(playerData.locale).prefix("anti-grief.alert.disable"))
            } else {
                playerData.alert = true
                player.sendMessage(Bundle(playerData.locale).prefix("anti-grief.alert.enable"))
            }
        }
        handler.register("ch", "Send chat to another server.") { _: Array<String?>?, player: Player ->
            if (!perm.check(player, "ch")) return@register
            val playerData = playerCore[player.uuid]
            playerData.crosschat = !playerData.crosschat
            player.sendMessage(Bundle(playerData.locale).prefix(if (playerData.crosschat) "player.crosschat.disable" else "player.crosschat.enabled"))
        }
        handler.register("changepw", "<new_password> <new_password_repeat>", "Change account password") { arg: Array<String>, player: Player ->
            if (!perm.check(player, "changepw")) return@register
            val playerData = playerCore[player.uuid]
            val bundle = Bundle(playerData.locale)
            if (!tool.checkPassword(player, playerData.accountid, arg[0], arg[1])) {
                player.sendMessage(bundle.prefix("system.account.need-new-password"))
                return@register
            }
            try {
                Class.forName("org.mindrot.jbcrypt.BCrypt")
                playerData.accountpw = BCrypt.hashpw(arg[0], BCrypt.gensalt(12))
                player.sendMessage(bundle.prefix("success"))
            } catch (e: ClassNotFoundException) {
                CrashReport(e)
            }
        }
        handler.register("chars", "<Text...>", "Make pixel texts") { arg: Array<String>, player: Player ->
            if (!perm.check(player, "chars")) return@register
            if (world != null) tool.setTileText(world.tile(player.tileX(), player.tileY()), Blocks.copperWall, arg[0])
        }
        handler.register("color", "Enable color nickname") { _: Array<String?>?, player: Player ->
            if (!perm.check(player, "color")) return@register
            val playerData = playerCore[player.uuid]
            playerData.colornick = !playerData.colornick
            if (playerData.colornick) colorNickname.targets.add(player)
            player.sendMessage(Bundle(playerData.locale).prefix(if (playerData.colornick) "feature.colornick.enable" else "feature.colornick.disable"))
        }
        handler.register("difficulty", "<difficulty>", "Set server difficulty") { arg: Array<String?>, player: Player ->
            if (!perm.check(player, "difficulty")) return@register
            val playerData = playerCore[player.uuid]
            try {
                Vars.state.rules.waveSpacing = Difficulty.valueOf(arg[0]!!).waveTime * 60 * 60 * 2
                Call.onSetRules(Vars.state.rules)
                player.sendMessage(Bundle(playerData.locale).prefix("system.difficulty.set", arg[0]))
            } catch (e: IllegalArgumentException) {
                player.sendMessage(Bundle(playerData.locale).prefix("system.difficulty.not-found", arg[0]))
            }
        }
        handler.register("killall", "Kill all enemy units") { _: Array<String?>?, player: Player ->
            if (!perm.check(player, "killall")) return@register
            for (a in Team.all().indices) Vars.unitGroup.all().each { obj: BaseUnit -> obj.kill() }
            player.sendMessage(Bundle(playerCore[player.uuid].locale).prefix("success"))
        }
        handler.register("help", "[page]", "Show command lists") { arg: Array<String?>, player: Player ->
            if (arg.isNotEmpty() && !Strings.canParseInt(arg[0])) {
                player.sendMessage(Bundle(playerCore[player.uuid].locale).prefix("page-number"))
                return@register
            }
            val temp = arc.struct.Array<String>()
            for (a in 0 until Vars.netServer.clientCommands.commandList.size) {
                val command = Vars.netServer.clientCommands.commandList[a]
                if (perm.check(player, command.text) || command.text == "t" || command.text == "sync") {
                    temp.add("[orange] /${command.text} [white]${command.paramText} [lightgray]- ${command.description}")
                }
            }
            val result = StringBuilder()
            val perpage = 8
            var page = if (arg.isNotEmpty()) Strings.parseInt(arg[0]) else 1
            val pages = Mathf.ceil(temp.size.toFloat() / perpage)
            page--
            if (page > pages || page < 0) {
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] $pages[scarlet].")
                return@register
            }
            result.append(Strings.format("[orange]-- Commands Page[lightgray] {0}[gray]/[lightgray]{1}[orange] --\n", page + 1, pages))
            for (a in perpage * page until (perpage * (page + 1)).coerceAtMost(temp.size)) {
                result.append(temp[a])
            }
            player.sendMessage(result.toString().substring(0, result.length - 1))
        }
        handler.register("info", "Show your information") { _: Array<String?>?, player: Player ->
            if (!perm.check(player, "info")) return@register
            val playerData = playerCore[player.uuid]
            val bundle = Bundle(playerData.locale)
            val datatext = """
                [#DEA82A]${Bundle(playerData.locale)["player.info"]}[]
                [#2B60DE]====================================[]
                [green]${bundle["player.name"]}[] : ${player.name}[white]
                [green]${bundle["player.uuid"]}[] : ${playerData.uuid}[white]
                [green]${bundle["player.country"]}[] : ${playerData.locale.getDisplayCountry(playerData.locale)}
                [green]${bundle["player.placecount"]}[] : ${playerData.placecount}
                [green]${bundle["player.breakcount"]}[] : ${playerData.breakcount}
                [green]${bundle["player.killcount"]}[] : ${playerData.killcount}
                [green]${bundle["player.deathcount"]}[] : ${playerData.deathcount}
                [green]${bundle["player.joincount"]}[] : ${playerData.joincount}
                [green]${bundle["player.kickcount"]}[] : ${playerData.kickcount}
                [green]${bundle["player.level"]}[] : ${playerData.level}
                [green]${bundle["player.reqtotalexp"]}[] : ${playerData.reqtotalexp}
                [green]${bundle["player.firstdate"]}[] : ${tool.longToDateTime(playerData.firstdate).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}
                [green]${bundle["player.lastdate"]}[] : ${tool.longToDateTime(playerData.lastdate).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))}
                [green]${bundle["player.playtime"]}[] : ${tool.longToTime(playerData.playtime)}
                [green]${bundle["player.attackclear"]}[] : ${playerData.attackclear}
                [green]${bundle["player.pvpwincount"]}[] : ${playerData.pvpwincount}
                [green]${bundle["player.pvplosecount"]}[] : ${playerData.pvplosecount}
                [green]${bundle["player.pvpbreakout"]}[] : ${playerData.pvpbreakout}
                """.trimIndent()
            Call.onInfoMessage(player.con, datatext)
        }
        handler.register("warp", "<zone/block/count/total> [ip] [parameters...]", "Create a server-to-server warp zone.") { arg: Array<String>, player: Player ->
            if (!perm.check(player, "warp")) return@register
            val playerData = playerCore[player.uuid]
            val bundle = Bundle(playerData.locale)
            val types = arrayOf("zone", "block", "count", "total")
            if (!listOf(*types).contains(arg[0])) {
                player.sendMessage(bundle["system.warp.info"])
            } else {
                val type = arg[0]
                val x = player.tileX()
                val y = player.tileY()
                val name = world.map.name()
                val size: Int
                val clickable: Boolean
                var ip = ""
                var port = 6567
                if (arg.size > 1) {
                    if (arg[1].contains(":")) {
                        val address = arg[1].split(":").toTypedArray()
                        ip = address[0]
                        port = address[1].toInt()
                    } else {
                        ip = arg[1]
                    }
                }
                val parameters: Array<String> = if (arg.size == 3) {
                    arg[2].split(" ").toTypedArray()
                } else {
                    arrayOf()
                }
                when (type) {
                    "zone" ->                         //ip size clickable
                        if (parameters.size <= 1) {
                            player.sendMessage(bundle.prefix("system.warp.incorrect"))
                        } else {
                            try {
                                size = parameters[0].toInt()
                                clickable = java.lang.Boolean.parseBoolean(parameters[1])
                            } catch (ignored: NumberFormatException) {
                                player.sendMessage(bundle.prefix("system.warp.not-int"))
                                return@register
                            }
                            pluginData.warpzones.add(PluginData.WarpZone(name, world.tile(x, y).pos(), world.tile(x + size, y + size).pos(), clickable, ip, port))
                            warpBorder.thread.clear()
                            warpBorder.start()
                            player.sendMessage(bundle.prefix("system.warp.added"))
                        }
                    "block" -> if (parameters.isEmpty()) {
                        player.sendMessage(bundle.prefix("system.warp.incorrect"))
                    } else {
                        val t: Tile = world.tile(x, y).link()
                        pluginData.warpblocks.add(PluginData.WarpBlock(name, t.pos(), t.block().name, t.block().size, ip, port, arg[2]))
                        player.sendMessage(bundle.prefix("system.warp.added"))
                    }
                    "count" -> {
                        pluginData.warpcounts.add(PluginData.WarpCount(name, world.tile(x, y).pos(), ip, port, 0, 0))
                        player.sendMessage(bundle.prefix("system.warp.added"))
                    }
                    "total" -> {
                        pluginData.warptotals.add(PluginData.WarpTotal(name, world.tile(x, y).pos(), 0, 0))
                        player.sendMessage(bundle.prefix("system.warp.added"))
                    }
                    else -> player.sendMessage(bundle.prefix("command.invalid"))
                }
            }
        }
        handler.register("kickall", "Kick all players") { _: Array<String?>?, player: Player ->
            if (!perm.check(player, "kickall")) return@register
            for (p in Vars.playerGroup.all()) {
                if (player !== p) Call.onKick(p.con, Packets.KickReason.kick)
            }
        }
        handler.register("kill", "[player]", "Kill player.") { arg: Array<String?>, player: Player ->
            if (!perm.check(player, "kill")) return@register
            if (arg.isEmpty()) {
                player.kill()
            } else {
                val other = Vars.playerGroup.find { p: Player -> p.name.equals(arg[0], ignoreCase = true) }
                if (other == null) {
                    player.sendMessage(Bundle(playerCore[player.uuid].locale).prefix("player.not-found"))
                } else {
                    other.kill()
                }
            }
        }
        handler.register("login", "<id> <password>", "Access your account") { arg: Array<String>, player: Player ->
            val playerData = playerCore[player.uuid]
            if (configs.loginEnable) {
                if (playerData.error) {
                    if (playerCore.login(arg[0], arg[1])) {
                        if (playerCore.playerLoad(player, arg[0])) {
                            player.sendMessage(Bundle(playerData.locale).prefix("system.login.success"))
                        }
                    } else {
                        player.sendMessage("[green][EssentialPlayer] [scarlet]Login failed/로그인 실패!!")
                    }
                } else {
                    if (configs.passwordMethod == "mixed") {
                        if (playerCore.login(arg[0], arg[1])) Call.onConnect(player.con, vars.serverIP, 7060)
                    } else {
                        player.sendMessage("[green][EssentialPlayer] [scarlet]You're already logged./이미 로그인한 상태입니다.")
                    }
                }
            } else {
                player.sendMessage(Bundle(playerData.locale).prefix("system.login.disabled"))
            }
        }
        handler.register("logout", "Log-out of your account.") { _: Array<String?>?, player: Player ->
            if (!perm.check(player, "logout")) return@register
            val playerData = playerCore[player.uuid]
            val bundle = Bundle(playerData.locale)
            if (configs.loginEnable && !playerData.error) {
                playerData.connected = false
                playerData.connserver = "none"
                playerData.uuid = "Logout"
                Call.onKick(player.con, Bundle(playerData.locale)["system.logout"])
            } else {
                player.sendMessage(bundle.prefix("system.login.disabled"))
            }
        }
        handler.register("maps", "[page]", "Show server maps") { arg: Array<String?>, player: Player ->
            if (!perm.check(player, "maps")) return@register
            val maplist = Vars.maps.all()
            val build = StringBuilder()
            var page = if (arg.isNotEmpty()) Strings.parseInt(arg[0]) else 1
            val pages = Mathf.ceil(maplist.size.toFloat() / 6)
            page--
            if (page > pages || page < 0) {
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] $pages[scarlet].")
                return@register
            }
            build.append("[green]==[white] Server maps page ").append(page).append("/").append(pages).append(" [green]==[white]\n")
            for (a in 6 * page until (6 * (page + 1)).coerceAtMost(maplist.size)) {
                build.append("[gray]").append(a).append("[] ").append(maplist[a].name()).append("\n")
            }
            player.sendMessage(build.toString())
        }
        handler.register("me", "<text...>", "broadcast * message") { arg: Array<String>, player: Player ->
            if (!perm.check(player, "me")) return@register
            Call.sendMessage("[orange]*[] " + player.name + "[white] : " + arg[0])
        }
        handler.register("motd", "Show server motd.") { _: Array<String?>?, player: Player ->
            if (!perm.check(player, "motd")) return@register
            val motd = tool.getMotd(playerCore[player.uuid].locale)
            val count = motd.split("\r\n|\r|\n").toTypedArray().size
            if (count > 10) {
                Call.onInfoMessage(player.con, motd)
            } else {
                player.sendMessage(motd)
            }
        }
        handler.register("players", "Show players list") { arg: Array<String?>, player: Player ->
            if (!perm.check(player, "players")) return@register
            val build = StringBuilder()
            var page = if (arg.isNotEmpty()) Strings.parseInt(arg[0]) else 1
            val pages = Mathf.ceil(Vars.playerGroup.size().toFloat() / 6)
            page--
            if (page > pages || page < 0) {
                player.sendMessage("[scarlet]'page' must be a number between[orange] 1[] and[orange] $pages[scarlet].")
                return@register
            }
            build.append("[green]==[white] Players list page ").append(page).append("/").append(pages).append(" [green]==[white]\n")
            for (a in 6 * page until (6 * (page + 1)).coerceAtMost(Vars.playerGroup.size())) {
                build.append("[gray]").append(Vars.playerGroup.all()[a].id).append("[] ").append(Vars.playerGroup.all()[a].name).append("\n")
            }
            player.sendMessage(build.toString())
        }
        handler.register("save", "Auto rollback map early save") { _: Array<String?>?, player: Player ->
            if (!perm.check(player, "save")) return@register
            val file = Vars.saveDirectory.child(configs.slotNumber.toString() + "." + Vars.saveExtension)
            SaveIO.save(file)
            player.sendMessage(Bundle(playerCore[player.uuid].locale).prefix("system.map-saved"))
        }
        handler.register("r", "<player> [message]", "Send Direct message to target player") { arg: Array<String>, player: Player ->
            if (!perm.check(player, "r")) return@register
            val playerData = playerCore[player.uuid]
            val bundle = Bundle(playerData.locale)
            val target = Vars.playerGroup.all().find { p: Player -> p.name.contains(arg[0]) }
            if (target != null) {
                target.sendMessage("[orange]DM [sky]" + playerData.name + " [green]>> [white]" + arg[1])
                player.sendMessage("[cyan]DM [sky]" + target.name + " [green]>> [white]" + arg[1])
            } else {
                player.sendMessage(bundle["player.not-found"])
            }
        }
        handler.register("reset", "<zone/count/total/block> [ip]", "Remove a server-to-server warp zone data.") { arg: Array<String>, player: Player ->
            if (!perm.check(player, "reset")) return@register
            val playerData = playerCore[player.uuid]
            val bundle = Bundle(playerData.locale)
            when (arg[0]) {
                "zone" -> {
                    var a = 0
                    while (a < pluginData.warpzones.size) {
                        if (arg.size != 2) {
                            player.sendMessage(bundle.prefix("no-parameter"))
                            return@register
                        }
                        if (arg[1] == pluginData.warpzones[a].ip) {
                            pluginData.warpzones.remove(a)
                            for (value in warpBorder.thread) {
                                value.interrupt()
                            }
                            warpBorder.thread.clear()
                            warpBorder.start()
                            player.sendMessage(bundle.prefix("success"))
                            break
                        }
                        a++
                    }
                }
                "count" -> {
                    pluginData.warpcounts.clear()
                    player.sendMessage(bundle.prefix("system.warp.reset", "count"))
                }
                "total" -> {
                    pluginData.warptotals.clear()
                    player.sendMessage(bundle.prefix("system.warp.reset", "total"))
                }
                "block" -> {
                    pluginData.warpblocks.clear()
                    player.sendMessage(bundle.prefix("system.warp.reset", "block"))
                }
                else -> player.sendMessage(bundle.prefix("command.invalid"))
            }
        }
        handler.register("router", "Router") { _: Array<String?>?, player: Player ->
            if (!perm.check(player, "router")) return@register
            Thread {
                val zero = arrayOf("""
    [stat][#404040][]
    [stat][#404040][]
    [stat][#404040]
    [stat][#404040][]
    [#404040][stat]
    [stat][#404040][]
    [stat][#404040][]
    [stat][#404040][][#404040]
    """.trimIndent(),
                        """
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][][#404040][]
                            """.trimIndent(),
                        """
                            [stat][#404040][][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat][][stat]
                            """.trimIndent(),
                        """
                            [stat][#404040][][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            """.trimIndent(),
                        """
                            [#404040][stat][][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            [stat][#404040]
                            [stat][#404040][]
                            [#404040][stat]
                            [stat][#404040][]
                            [stat][#404040][]
                            
                            """.trimIndent())
                val loop = arrayOf("""
    [#6B6B6B][stat][#6B6B6B]
    [stat][#404040][]
    [stat][#404040]
    [stat][#404040][]
    [#404040][]
    [stat][#404040][]
    [stat][#404040][]
    [#6B6B6B][stat][#404040][][#6B6B6B]
    
    """.trimIndent(),
                        """
                            [#6B6B6B][stat][#6B6B6B]
                            [#6B6B6B][stat][#404040][][#6B6B6B]
                            [stat][#404040][]
                            [#404040][]
                            [stat][#404040][]
                            [stat][#404040][]
                            [#6B6B6B][stat][#404040][][#6B6B6B]
                            [#6B6B6B][stat][#6B6B6B]
                            """.trimIndent(),
                        """
                            [#6B6B6B][#585858][stat][][#6B6B6B]
                            [#6B6B6B][#828282][stat][#404040][][][#6B6B6B]
                            [#585858][stat][#404040][][#585858]
                            [stat][#404040][]
                            [stat][#404040][]
                            [#585858][stat][#404040][][#585858]
                            [#6B6B6B][stat][#404040][][#828282][#6B6B6B]
                            [#6B6B6B][#585858][stat][][#6B6B6B]
                            """.trimIndent(),
                        """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][stat][][#6B6B6B]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#585858][stat][#404040][][#585858]
                            [#585858][stat][#404040][][#585858]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#6B6B6B][stat][][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """.trimIndent(),
                        """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#585858][#6B6B6B][stat][][#828282][#585858]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#585858][#6B6B6B][stat][#404040][][#828282][#585858]
                            [#585858][#6B6B6B][stat][][#828282][#585858]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """.trimIndent(),
                        """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][stat][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][stat][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """.trimIndent(),
                        """
                            [#6B6B6B][#585858][#6B6B6B]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#6B6B6B][#828282][#585858]
                            [#585858][#6B6B6B][#828282][#585858]
                            [#6B6B6B][#828282][#6B6B6B]
                            [#6B6B6B][#585858][#6B6B6B]
                            """.trimIndent())
                try {
                    while (player.isValid) {
                        for (d in loop) {
                            player.name = d
                            Thread.sleep(500)
                        }
                        Thread.sleep(5000)
                        for (i in loop.indices.reversed()) {
                            player.name = loop[i]
                            Thread.sleep(500)
                        }
                        for (d in zero) {
                            player.name = d
                            Thread.sleep(500)
                        }
                    }
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }.start()
        }
        handler.register("register", if (configs.passwordMethod.equals("password", ignoreCase = true)) "<accountid> <password>" else "", "Register account") { arg: Array<String>, player: Player ->
            if (configs.loginEnable) {
                when (configs.passwordMethod) {
                    "discord" -> {
                        player.sendMessage("""Join discord and use !register command!${configs.discordLink}""".trimIndent())
                        if (!discord.pins.containsKey(player.name)) discord.queue(player)
                    }
                    "password" -> {
                        val lc = tool.getGeo(player)
                        val hash = BCrypt.hashpw(arg[1], BCrypt.gensalt(12))
                        val register = playerCore.register(player.name, player.uuid, lc.displayCountry, lc.toString(), lc.displayLanguage, true, pluginVars.serverIP, "default", 0L, arg[0], hash, false)
                        if (register) {
                            playerCore.playerLoad(player, null)
                            player.sendMessage(Bundle(playerCore[player.uuid].locale).prefix("register-success"))
                        } else {
                            player.sendMessage("[green][Essentials] [scarlet]Register failed/계정 등록 실패!")
                        }
                    }
                    else -> {
                        val lc = tool.getGeo(player)
                        val hash = BCrypt.hashpw(arg[1], BCrypt.gensalt(12))
                        val register = playerCore.register(player.name, player.uuid, lc.displayCountry, lc.toString(), lc.displayLanguage, true, pluginVars.serverIP, "default", 0L, arg[0], hash, false)
                        if (register) {
                            playerCore.playerLoad(player, null)
                            player.sendMessage(Bundle(playerCore[player.uuid].locale).prefix("register-success"))
                        } else {
                            player.sendMessage("[green][Essentials] [scarlet]Register failed/계정 등록 실패!")
                        }
                    }
                }
            } else {
                player.sendMessage(Bundle(configs.locale).prefix("system.login.disabled"))
            }
        }
        handler.register("spawn", "<mob_name> <count> [team] [playerName]", "Spawn mob in player position") { arg: Array<String>, player: Player ->
            if (!perm.check(player, "spawn")) return@register
            val playerData = playerCore[player.uuid]
            val bundle = Bundle(playerData.locale)
            val targetUnit = tool.getUnitByName(arg[0])
            if (targetUnit == null) {
                player.sendMessage(bundle.prefix("system.mob.not-found"))
                return@register
            }
            val count: Int
            count = try {
                arg[1].toInt()
            } catch (e: NumberFormatException) {
                player.sendMessage(bundle.prefix("syttem.mob.not-number"))
                return@register
            }
            if (configs.spawnLimit == count) {
                player.sendMessage(bundle.prefix("spawn-limit"))
                return@register
            }
            var targetPlayer = if (arg.size > 3) tool.findPlayer(arg[3]) else player
            if (targetPlayer == null) {
                player.sendMessage(bundle.prefix("player.not-found"))
                targetPlayer = player
            }
            var targetTeam = if (arg.size > 2) tool.getTeamByName(arg[2]) else targetPlayer.team
            if (targetTeam == null) {
                player.sendMessage(bundle.prefix("team-not-found"))
                targetTeam = targetPlayer.team
            }
            var i = 0
            while (count > i) {
                val baseUnit = targetUnit.create(targetTeam)
                baseUnit[targetPlayer.getX()] = targetPlayer.getY()
                baseUnit.add()
                i++
            }
        }
        handler.register("setperm", "<player_name> <group>", "Set player permission") { arg: Array<String>, player: Player ->
            if (!perm.check(player, "setperm")) return@register
            val playerData = playerCore[player.uuid]
            val bundle = Bundle(playerData.locale)
            val target = Vars.playerGroup.find { p: Player -> p.name == arg[0] }
            if (target == null) {
                player.sendMessage(bundle.prefix("player.not-found"))
                return@register
            }
            for (permission in perm.perm) {
                if (permission.name == arg[1]) {
                    val data = playerCore[target.uuid]
                    data.permission = arg[1]
                    perm.user[data.uuid].asObject()["group"] = arg[1]
                    perm.update(true)
                    player.sendMessage(bundle.prefix("success"))
                    target.sendMessage(Bundle(data.locale).prefix("perm-changed"))
                    return@register
                }
            }
            player.sendMessage(Bundle(playerData.locale).prefix("perm-group-not-found"))
        }
        handler.register("spawn-core", "<smail/normal/big>", "Make new core") { arg: Array<String?>, player: Player ->
            if (!perm.check(player, "spawn-core")) return@register
            var core = Blocks.coreShard
            when (arg[0]) {
                "normal" -> core = Blocks.coreFoundation
                "big" -> core = Blocks.coreNucleus
            }
            Call.onConstructFinish(world.tile(player.tileX(), player.tileY()), core, 0, 0.toByte(), player.team, false)
        }
        handler.register("setmech", "<Mech> [player]", "Set player mech") { arg: Array<String>, player: Player ->
            if (!perm.check(player, "setmech")) return@register
            val playerData = playerCore[player.uuid]
            val bundle = Bundle(playerData.locale)
            var mech = Mechs.starter
            when (arg[0]) {
                "alpha" -> mech = Mechs.alpha
                "dart" -> mech = Mechs.dart
                "glaive" -> mech = Mechs.glaive
                "delta" -> mech = Mechs.delta
                "javelin" -> mech = Mechs.javelin
                "omega" -> mech = Mechs.omega
                "tau" -> mech = Mechs.tau
                "trident" -> mech = Mechs.trident
            }
            if (arg.size == 1) {
                for (p in Vars.playerGroup.all()) {
                    p.mech = mech
                }
            } else {
                val target = Vars.playerGroup.find { p: Player -> p.name == arg[1] }
                if (target == null) {
                    player.sendMessage(bundle.prefix("player.not-found"))
                    return@register
                }
                target.mech = mech
            }
            player.sendMessage(bundle.prefix("success"))
        }
        handler.register("status", "Show server status") { _: Array<String?>?, player: Player ->
            if (!perm.check(player, "status")) return@register
            val playerData = playerCore[player.uuid]
            val bundle = Bundle(playerData.locale)
            player.sendMessage(bundle.prefix("server.status"))
            player.sendMessage("[#2B60DE]========================================[]")
            val fps = (60f.toInt() / Time.delta()).roundToLong()
            val bans = Vars.netServer.admins.banned.size
            val ipbans = Vars.netServer.admins.bannedIPs.size
            val bancount = bans + ipbans
            val playtime = tool.longToTime(vars.playtime)
            val uptime = tool.longToTime(vars.uptime)
            player.sendMessage(bundle["server.status.result", fps, Vars.playerGroup.size(), bancount, bans, ipbans, playtime, uptime, vars.pluginVersion])
            val result = JsonObject()
            for (p in vars.playerData) {
                if (result[p.locale.getDisplayCountry(playerData.locale)] == null) {
                    result.add(p.locale.getDisplayCountry(playerData.locale), 1)
                } else {
                    result[p.locale.getDisplayCountry(playerData.locale)] = result[p.locale.getDisplayCountry(playerData.locale)].asInt() + 1
                }
            }
            val s = StringBuilder()
            for (m in result) {
                val d = """
                    ${m.name}: ${m.value}
                    
                    """.trimIndent()
                s.append(d)
            }
            player.sendMessage(s.substring(0, s.length - 1))
        }
        handler.register("suicide", "Kill yourself.") { _: Array<String?>?, player: Player ->
            if (!perm.check(player, "suicide")) return@register
            player.kill()
            if (Vars.playerGroup != null && Vars.playerGroup.size() > 0) {
                tool.sendMessageAll("suicide", player.name)
            }
        }
        handler.register("team", "<team_name>", "Change team") { arg: Array<String?>, player: Player ->
            if (!perm.check(player, "team")) return@register
            val playerData = playerCore[player.uuid]
            when (arg[0]) {
                "derelict" -> player.team = Team.derelict
                "sharded" -> player.team = Team.sharded
                "crux" -> player.team = Team.crux
                "green" -> player.team = Team.green
                "purple" -> player.team = Team.purple
                "blue" -> player.team = Team.blue
                else -> player.sendMessage(Bundle(playerData.locale).prefix("command.team"))
            }
        }
        handler.register("tempban", "<player> <time> <reason>", "Temporarily ban player. time unit: 1 minute") { arg: Array<String>, player: Player ->
            if (!perm.check(player, "tempban")) return@register
            val playerData = playerCore[player.uuid]
            var other: Player? = null
            for (p in Vars.playerGroup.all()) {
                val result = p.name.contains(arg[0])
                if (result) {
                    other = p
                }
            }
            if (other != null) {
                val bantime = System.currentTimeMillis() + 1000 * 60 * (arg[1].toInt())
                playerCore.ban(other, bantime, arg[2])
                other.con.kick("Temp kicked")
                for (a in 0 until Vars.playerGroup.size()) {
                    val current = Vars.playerGroup.all()[a]
                    val target = playerCore[current.uuid]
                    current.sendMessage(Bundle(target.locale).prefix("account.ban.temp", other.name, player.name))
                }
            } else {
                player.sendMessage(Bundle(playerData.locale).prefix("player.not-found"))
            }
        }
        handler.register("time", "Show server time") { _: Array<String?>?, player: Player ->
            if (!perm.check(player, "time")) return@register
            val playerData = playerCore[player.uuid]
            val now = LocalDateTime.now()
            val dateTimeFormatter = DateTimeFormatter.ofPattern("yy-MM-dd HH:mm:ss")
            val nowString = now.format(dateTimeFormatter)
            player.sendMessage(Bundle(playerData.locale).prefix("servertime", nowString))
        }
        handler.register("tp", "<player>", "Teleport to other players") { arg: Array<String?>, player: Player ->
            if (!perm.check(player, "tp")) return@register
            val playerData = playerCore[player.uuid]
            val bundle = Bundle(playerData.locale)
            if (player.isMobile) {
                player.sendMessage(bundle.prefix("tp-not-support"))
                return@register
            }
            var other: Player? = null
            for (p in Vars.playerGroup.all()) {
                val result = p.name.contains(arg[0]!!)
                if (result) {
                    other = p
                }
            }
            if (other == null) {
                player.sendMessage(bundle.prefix("player.not-found"))
                return@register
            }
            player.setNet(other!!.getX(), other!!.getY())
        }
        handler.register("tpp", "<source> <target>", "Teleport to other players") { arg: Array<String?>, player: Player ->
            if (!perm.check(player, "tpp")) return@register
            val playerData = playerCore[player.uuid]
            var other1: Player? = null
            var other2: Player? = null
            for (p in Vars.playerGroup.all()) {
                val result1 = p.name.contains(arg[0]!!)
                if (result1) {
                    other1 = p
                }
                val result2 = p.name.contains(arg[1]!!)
                if (result2) {
                    other2 = p
                }
            }
            if (other1 == null || other2 == null) {
                player.sendMessage(Bundle(playerData.locale).prefix("player.not-found"))
                return@register
            }
            if (!other1.isMobile || !other2.isMobile) {
                other1.setNet(other2.x, other2.y)
            } else {
                player.sendMessage(Bundle(playerData.locale).prefix("tp-ismobile"))
            }
        }
        handler.register("tppos", "<x> <y>", "Teleport to coordinates") { arg: Array<String>, player: Player ->
            if (!perm.check(player, "tppos")) return@register
            val playerData = playerCore[player.uuid]
            val x: Int
            val y: Int
            try {
                x = arg[0].toInt()
                y = arg[1].toInt()
            } catch (ignored: Exception) {
                player.sendMessage(Bundle(playerData.locale).prefix("tp-not-int"))
                return@register
            }
            player.setNet(x.toFloat(), y.toFloat())
        }
        /*handler.<Player>register("tr", "Enable/disable Translate all chat", (arg, player) -> {
            if (!perm.check(player, "tr")) return;
            PlayerData playerData = playerCore.get(player.uuid);
            playerCore.get(player.uuid).translate(!playerData.translate());
            player.sendMessage(new Bundle(playerData.locale).prefix(playerData.translate() ? "translate" : "translate-disable", player.name));
        });*/if (configs.vote) {
            handler.register("vote", "<mode> [parameter...]", "Voting system (Use /vote to check detail commands)") { arg: Array<String>, player: Player ->
                if (!perm.check(player, "vote") || Core.settings.getBool("isLobby")) return@register
                val playerData = playerCore[player.uuid]
                val bundle = Bundle(playerData.locale)
                if (vote.service.process) {
                    player.sendMessage(bundle.prefix("vote.in-processing"))
                    return@register
                }
                vote.player = player
                when (arg[0]) {
                    "kick" -> {
                        // vote kick <player name>
                        if (arg.size < 2) {
                            player.sendMessage(bundle["no-parameter"])
                            return@register
                        }
                        var target = Vars.playerGroup.find { p: Player -> p.name.equals(arg[1], ignoreCase = true) }
                        try {
                            if (target == null) target = Vars.playerGroup.find { p: Player -> p.id == arg[1].toInt() }
                        } catch (e: NumberFormatException) {
                            player.sendMessage(bundle.prefix("player.not-found"))
                            return@register
                        }
                        when {
                            target == null -> {
                                player.sendMessage(bundle.prefix("player.not-found"))
                                return@register
                            }
                            target.isAdmin -> {
                                player.sendMessage(bundle.prefix("vote.target-admin"))
                                return@register
                            }
                            target === player -> {
                                player.sendMessage(bundle.prefix("vote.target-own"))
                                return@register
                            }


                            // 강퇴 투표
                            else -> {
                                vote.type = Vote.VoteType.kick
                                vote.parameters = arrayOf(target, arg[1])
                            }
                        }

                    }
                    "map" -> {
                        // vote map <map name>
                        if (arg.size < 2) {
                            player.sendMessage(bundle["no-parameter"])
                            return@register
                        }

                        // 맵 투표
                        var world = Vars.maps.all().find { map: Map -> map.name().equals(arg[1].replace('_', ' '), ignoreCase = true) || map.name().equals(arg[1], ignoreCase = true) }
                        if (world == null) {
                            try {
                                world = Vars.maps.all()[arg[1].toInt()]
                                if (world != null) {
                                    vote.type = Vote.VoteType.map
                                    vote.parameters = arrayOf(world)
                                } else {
                                    player.sendMessage(bundle.prefix("vote.map.not-found"))
                                }
                            } catch (ignored: NumberFormatException) {
                                player.sendMessage(bundle.prefix("vote.map.not-found"))
                            }
                        } else {
                            vote.type = Vote.VoteType.map
                            vote.parameters = arrayOf(world)
                        }
                    }
                    "gameover" -> {
                        // vote gameover
                        vote.type = Vote.VoteType.gameover
                        vote.parameters = arrayOf()
                    }
                    "rollback" ->                         // vote rollback
                        if (configs.rollback) {
                            vote.type = Vote.VoteType.rollback
                            vote.parameters = arrayOf()
                        } else {
                            player.sendMessage(bundle["vote.rollback.disabled"])
                        }
                    "gamemode" -> {
                        // vote gamemode <gamemode>
                        if (arg.size < 2) {
                            player.sendMessage(bundle["no-parameter"])
                            return@register
                        }
                        try {
                            vote.type = Vote.VoteType.gamemode
                            vote.parameters = arrayOf(Gamemode.valueOf(arg[1]))
                        } catch (e: IllegalArgumentException) {
                            player.sendMessage(bundle.prefix("vote.wrong-gamemode"))
                        }
                    }
                    "skipwave" -> {
                        // vote skipwave <wave>
                        if (arg.size != 2) {
                            player.sendMessage(bundle["no-parameter"])
                            return@register
                        }
                        vote.type = Vote.VoteType.skipwave
                        vote.parameters = arrayOf(arg[1])
                    }
                    else -> {
                        when (arg[0]) {
                            "gamemode" -> player.sendMessage(bundle.prefix("vote.list.gamemode"))
                            "map" -> player.sendMessage(bundle.prefix("vote.map.not-found"))
                            "kick" -> player.sendMessage(bundle.prefix("vote.kick.parameter"))
                            else -> player.sendMessage(bundle.prefix("vote.list"))
                        }
                        return@register
                    }
                }
                vote.pause = false
            }
        }
        handler.register("weather", "<day/eday/night/enight>", "Change map light") { arg: Array<String?>, player: Player ->
            if (!perm.check(player, "weather")) return@register
            // Command idea from Minecraft EssentialsX and Quezler's plugin!
            // Useful with the Quezler's plugin.
            Vars.state.rules.lighting = true
            when (arg[0]) {
                "day" -> Vars.state.rules.ambientLight.a = 0f
                "eday" -> Vars.state.rules.ambientLight.a = 0.3f
                "night" -> Vars.state.rules.ambientLight.a = 0.7f
                "enight" -> Vars.state.rules.ambientLight.a = 0.85f
                else -> return@register
            }
            Call.onSetRules(Vars.state.rules)
            player.sendMessage(Bundle(playerCore[player.uuid].locale).prefix("success"))
        }
        handler.register("mute", "<Player_name>", "Mute/unmute player") { arg: Array<String?>, player: Player ->
            if (!perm.check(player, "mute")) return@register
            val other = Vars.playerGroup.find { p: Player -> p.name.equals(arg[0], ignoreCase = true) }
            val playerData = playerCore[player.uuid]
            if (other == null) {
                player.sendMessage(Bundle(playerData.locale).prefix("player.not-found"))
            } else {
                val target = playerCore[other.uuid]
                target.mute = !target.mute
                player.sendMessage(Bundle(target.locale).prefix(if (target.mute) "player.muted" else "player.unmute", target.name))
            }
        }
    }
}