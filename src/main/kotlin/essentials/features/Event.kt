package essentials.features

import arc.Core
import arc.Events
import arc.func.Boolf
import arc.struct.ArrayMap
import essentials.Main.Companion.client
import essentials.Main.Companion.configs
import essentials.Main.Companion.discord
import essentials.Main.Companion.mainThread
import essentials.Main.Companion.perm
import essentials.Main.Companion.playerCore
import essentials.Main.Companion.pluginData
import essentials.Main.Companion.pluginRoot
import essentials.Main.Companion.pluginVars
import essentials.Main.Companion.server
import essentials.Main.Companion.timer
import essentials.Main.Companion.tool
import essentials.Main.Companion.vote
import essentials.PlayerData
import essentials.PluginData
import essentials.external.IpAddressMatcher
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.internal.Log.LogType
import essentials.network.Client
import mindustry.Vars
import mindustry.Vars.world
import mindustry.content.Blocks
import mindustry.core.NetClient
import mindustry.entities.type.Player
import mindustry.game.Difficulty
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.net.Packets
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.hjson.JsonValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern
import kotlin.math.abs

class Event {
    var log: Logger = LoggerFactory.getLogger(Event::class.java)

    init {
        Events.on(TapConfigEvent::class.java) { e: TapConfigEvent ->
            if (e.tile.entity != null && e.tile.entity.block != null && e.player != null && configs.alertAction) {
                for (p in Vars.playerGroup.all()) {
                    val playerData = playerCore[p.uuid]
                    if (playerData.alert) {
                        p.sendMessage(Bundle(playerData.locale)["tap-config", e.player.name, e.tile.entity.block.name])
                    }
                }
                if (configs.debug) Log.info("anti-grief.build.config", e.player.name, e.tile.block().name, e.tile.pos())
                if (configs.logging) Log.write(LogType.tap, "log.tap-config", e.player.name, e.tile.block().name)

                // Source by BasedUser(router)
                val entity = e.tile.entity
                val value = e.value
                val other = world.tile(value)
                val valid = other?.entity != null && other.entity.power != null
                if (valid) {
                    val oldGraph = entity.power.graph
                    val newgraph = other!!.entity.power.graph
                    val oldGraphCount = oldGraph.toString().substring(oldGraph.toString().indexOf("all={"), oldGraph.toString().indexOf("}, l")).replaceFirst("all=\\{".toRegex(), "").split(",").toTypedArray().size
                    val newGraphCount = newgraph.toString().substring(newgraph.toString().indexOf("all={"), newgraph.toString().indexOf("}, l")).replaceFirst("all=\\{".toRegex(), "").split(",").toTypedArray().size
                    if (abs(oldGraphCount - newGraphCount) > 10) {
                        //tool.sendMessageAll("anti-grief.powernode", e.player.name, "[green] " + Math.max(oldGraphCount, newGraphCount) + " [cyan]->[scarlet] " + Math.min(oldGraphCount, newGraphCount) + " [white](" + e.tile.x + ", " + e.tile.y + ")");
                        Call.sendMessage(e.player.name + " [white]player has [scarlet]unlinked[] the [yellow]power node[]. Number of connected buildings: [green] " + oldGraphCount.coerceAtLeast(newGraphCount) + " [cyan]->[scarlet] " + oldGraphCount.coerceAtMost(newGraphCount) + " [white](" + e.tile.x + ", " + e.tile.y + ")")
                    }
                }
            }
        }
        Events.on(TapEvent::class.java) { e: TapEvent ->
            if (configs.logging) Log.write(LogType.tap, "log.tap", e.player.name, e.tile.block().name)
            val playerData = playerCore[e.player.uuid]
            if (!playerData.error) {
                for (data in pluginData.warpblocks) {
                    if (e.tile.x >= world.tile(data.pos).link().x && e.tile.x <= world.tile(data.pos).link().x) {
                        if (e.tile.y >= world.tile(data.pos).link().y && e.tile.y <= world.tile(data.pos).link().y) {
                            if (data.online) {
                                Log.info("player.warped", e.player.name, data.ip + ":" + data.port)
                                playerData.connected = false
                                playerData.connserver = "none"
                                Call.onConnect(e.player.con, data.ip, data.port)
                            }
                            break
                        }
                    }
                }

                /*for (PluginData.warpzone data : pluginData.warpzones) {
                    if (e.tile.x > data.getStartTile().x && e.tile.x < data.getFinishTile().x) {
                        if (e.tile.y > data.getStartTile().y && e.tile.y < data.getFinishTile().y) {
                            Log.info("player.warped", e.player.name, data.ip + ":" + data.port);
                            playerData.connected(false);
                            playerData.connserver("none");
                            Call.onConnect(e.player.con, data.ip, data.port);
                            break;
                        }
                    }
                }*/
            }
        }
        Events.on(WithdrawEvent::class.java) { e: WithdrawEvent ->
            if (e.tile.entity != null && e.player.item().item != null && e.player.name != null && configs.antiGrief) {
                for (p in Vars.playerGroup.all()) {
                    val playerData = playerCore[p.uuid]
                    if (playerData.alert) {
                        p.sendMessage(Bundle(playerData.locale)["log.withdraw", e.player.name, e.player.item().item.name, e.amount, e.tile.block().name])
                    }
                }
                if (configs.debug) Log.info("log.withdraw", e.player.name, e.player.item().item.name, e.amount, e.tile.block().name)
                if (configs.logging) Log.write(LogType.withdraw, "log.withdraw", e.player.name, e.player.item().item.name, e.amount, e.tile.block().name)
                if (Vars.state.rules.pvp) {
                    if (e.item.flammability > 0.001f) {
                        e.player.sendMessage(Bundle(playerCore[e.player.uuid].locale)["system.flammable.disabled"])
                        e.player.clearItem()
                    }
                }
            }
        }

        // 게임오버가 되었을 때
        Events.on(GameOverEvent::class.java) { e: GameOverEvent ->
            if (Vars.state.rules.pvp) {
                var index = 5
                for (a in 0..4) {
                    if (Vars.state.teams[Team.all()[index]].cores.isEmpty) {
                        index--
                    }
                }
                if (index == 1) {
                    for (i in 0 until Vars.playerGroup.size()) {
                        val player = Vars.playerGroup.all()[i]
                        val target = playerCore[player.uuid]
                        if (target.login) {
                            if (player.team.name == e.winner.name) {
                                target.pvpwincount = target.pvpwincount + 1
                            } else if (player.team.name != e.winner.name) {
                                target.pvplosecount = target.pvplosecount + 1
                            }
                        }
                    }
                }
            } else if (Vars.state.rules.attackMode) {
                for (i in 0 until Vars.playerGroup.size()) {
                    val player = Vars.playerGroup.all()[i]
                    val target = playerCore[player.uuid]
                    if (target.login) {
                        target.attackclear = target.attackclear + 1
                    }
                }
            }
        }

        // 맵이 불러와졌을 때
        Events.on(WorldLoadEvent::class.java) {
            pluginVars.playtime = 0L

            // 전력 노드 정보 초기화
            pluginData.powerblocks.clear()
        }
        Events.on(PlayerConnect::class.java) { e: PlayerConnect ->
            if (configs.logging) Log.write(LogType.player, "log.player.connect", e.player.name, e.player.uuid, e.player.con.address)

            // 닉네임이 블랙리스트에 등록되어 있는지 확인
            for (s in pluginData.blacklist) {
                if (e.player.name.matches(Regex(s))) {
                    try {
                        val locale = tool.getGeo(e.player)
                        Call.onKick(e.player.con, Bundle(locale)["system.nickname.blacklisted.kick"])
                        Log.info("system.nickname.blacklisted", e.player.name)
                    } catch (ex: Exception) {
                        CrashReport(ex)
                    }
                }
            }
            if (configs.strictName) {
                if (e.player.name.length > 32) Call.onKick(e.player.con, "Nickname too long!")
                //if (e.player.name.matches(".*\\[.*].*"))
                //    Call.onKick(e.player.con, "Color tags can't be used for nicknames on this server.");
                if (e.player.name.contains("　")) Call.onKick(e.player.con, "Don't use blank speical charactor nickname!")
                if (e.player.name.contains(" ")) Call.onKick(e.player.con, "Nicknames can't be used on this server!")
                if (Pattern.matches(".*\\[.*.].*", e.player.name)) Call.onKick(e.player.con, "Can't use only color tags nickname in this server.")
            }
        }

        // 플레이어가 아이템을 특정 블록에다 직접 가져다 놓았을 때
        Events.on(DepositEvent::class.java) { e: DepositEvent ->
            if (e.player.item().amount > e.player.mech.itemCapacity) {
                Vars.player.con.kick("Invalid request!")
                return@on
            }
            for (p in Vars.playerGroup.all()) {
                val playerData = playerCore[p.uuid]
                if (playerData.alert) {
                    p.sendMessage(Bundle(playerData.locale)["anti-grief.deposit", e.player.name, e.player.item().item.name, e.tile.block().name])
                }
            }
            if (configs.logging) Log.write(LogType.deposit, "log.deposit", e.player.name, e.player.item().item.name, e.tile.block().name)
        }

        // 플레이어가 서버에 들어왔을 때
        Events.on(PlayerJoin::class.java) { e: PlayerJoin ->
            if (configs.logging) Log.write(LogType.player, "log.player.join", e.player.name, e.player.uuid, e.player.con.address)
            pluginVars.players.add(e.player)
            e.player.isAdmin = false
            val t = Thread {
                Thread.currentThread().name = e.player.name + " Player Join thread"
                val playerData = playerCore.load(e.player.uuid, null)
                val bundle = Bundle(playerData.locale)
                if (configs.loginEnable) {
                    if (configs.passwordMethod == "mixed") {
                        if (!playerData.error && configs.autoLogin) {
                            if (playerData.udid != 0L) {
                                Thread { Call.onConnect(e.player.con, pluginVars.serverIP, 7060) }.start()
                            } else {
                                e.player.sendMessage(bundle["account.autologin"])
                                playerCore.playerLoad(e.player, null)
                            }
                        } else {
                            val lc = tool.getGeo(e.player)
                            if (playerCore.register(e.player.name, e.player.uuid, lc.displayCountry, lc.toString(), lc.displayLanguage, true, pluginVars.serverIP, "default", 0L, e.player.name, "none", false)) {
                                playerCore.playerLoad(e.player, null)
                            } else {
                                Call.onKick(e.player.con, Bundle()["plugin-error-kick"])
                            }
                        }
                    } else if (configs.passwordMethod == "discord") {
                        if (!playerData.error && configs.autoLogin) {
                            e.player.sendMessage(bundle["account.autologin"])
                            playerCore.playerLoad(e.player, null)
                        } else {
                            val message: String?
                            val language = tool.getGeo(e.player)
                            message = if (configs.passwordMethod == "discord") {
                                """${Bundle(language)["system.login.require.discord"]}
                                    ${configs.discordLink}
                                    """.trimIndent()
                            } else {
                                Bundle(language)["system.login.require.password"]
                            }
                            Call.onInfoMessage(e.player.con, message)
                        }
                    } else {
                        if (!playerData.error && configs.autoLogin) {
                            e.player.sendMessage(bundle["account.autologin"])
                            playerCore.playerLoad(e.player, null)
                        } else {
                            val message: String?
                            val language = tool.getGeo(e.player)
                            message = if (configs.passwordMethod == "discord") {
                                """${Bundle(language)["system.login.require.discord"]}
                                    ${configs.discordLink}
                                    """.trimIndent()
                            } else {
                                Bundle(language)["system.login.require.password"]
                            }
                            Call.onInfoMessage(e.player.con, message)
                        }
                    }
                } else {
                    // 로그인 기능이 꺼져있을 때, 바로 계정 등록을 하고 데이터를 로딩함
                    if (!playerData.error && configs.autoLogin) {
                        e.player.sendMessage(bundle["account.autologin"])
                        playerCore.playerLoad(e.player, null)
                    } else {
                        val lc = tool.getGeo(e.player.con.address)
                        val register = playerCore.register(e.player.name, e.player.uuid, lc.displayCountry, lc.toString(), lc.displayLanguage, true, pluginVars.serverIP, "default", 0L, e.player.name, "none", false)
                        if (!register || playerCore.playerLoad(e.player, null)) {
                            Call.onKick(e.player.con, Bundle()["plugin-error-kick"])
                        }
                    }
                }

                // VPN을 사용중인지 확인
                if (configs.antiVPN) {
                    try {
                        val reader = javaClass.getResourceAsStream("/ipv4.txt")
                        val br = BufferedReader(InputStreamReader(reader))
                        var line: String
                        while (br.readLine().also { line = it } != null) {
                            val match = IpAddressMatcher(line)
                            if (match.matches(e.player.con.address)) {
                                Call.onKick(e.player.con, Bundle()["anti-grief.vpn"])
                            }
                        }
                    } catch (ex: IOException) {
                        log.warn("VPN File", ex)
                    }
                }

                // PvP 평화시간 설정
                if (configs.antiRush && Vars.state.rules.pvp && pluginVars.playtime < configs.antiRushtime) {
                    Vars.state.rules.playerDamageMultiplier = 0f
                    Vars.state.rules.playerHealthMultiplier = 0.001f
                    Call.onSetRules(Vars.state.rules)
                    pluginVars.isPvPPeace = true
                }

                // 플레이어 인원별 난이도 설정
                if (configs.autoDifficulty) {
                    val total = Vars.playerGroup.size()
                    when {
                        configs.difficultyEasy >= total -> {
                            Vars.state.rules.waveSpacing = Difficulty.valueOf("easy").waveTime * 60 * 60 * 2
                            //tool.sendMessageAll("system.difficulty.easy");
                        }
                        configs.difficultyNormal == total -> {
                            Vars.state.rules.waveSpacing = Difficulty.valueOf("normal").waveTime * 60 * 60 * 2
                            //tool.sendMessageAll("system.difficulty.normal");
                        }
                        configs.difficultyHard == total -> {
                            Vars.state.rules.waveSpacing = Difficulty.valueOf("hard").waveTime * 60 * 60 * 2
                            //tool.sendMessageAll("system.difficulty.hard");
                        }
                        configs.difficultyInsane <= total -> {
                            Vars.state.rules.waveSpacing = Difficulty.valueOf("insane").waveTime * 60 * 60 * 2
                            //tool.sendMessageAll("system.difficulty.insane");
                        }
                    }
                    NetClient.onSetRules(Vars.state.rules)
                }
            }
            t.start()
        }

        // 플레이어가 서버에서 탈주했을 때
        Events.on(PlayerLeave::class.java) { e: PlayerLeave ->
            if (configs.logging) Log.write(LogType.player, "log.player.leave", e.player.name, e.player.uuid, e.player.con.address)
            val player = playerCore[e.player.uuid]
            if (player.login) {
                player.connected = false
                player.connserver = "none"
                if (Vars.state.rules.pvp && !Vars.state.gameOver) player.pvpbreakout = player.pvpbreakout + 1
            }
            playerCore.save(player)
            pluginVars.removePlayerData(Boolf { p: PlayerData -> p.uuid == e.player.uuid })
            pluginVars.players.remove(e.player)
        }

        // 플레이어가 수다떨었을 때
        Events.on(PlayerChatEvent::class.java) { e: PlayerChatEvent ->
            if (configs.antiGrief && (e.message.length > Vars.maxTextLength || e.message.contains("Nexity#2671"))) {
                Call.onKick(e.player.con, "Hacked client detected")
            }
            val playerData = playerCore[e.player.uuid]
            val bundle = Bundle(playerData.locale)
            if (!e.message.startsWith("/")) Log.info("<&y" + e.player.name + ": &lm" + e.message + "&lg>")
            if (!playerData.error) {
                // 명령어인지 확인
                if (!e.message.startsWith("/")) {
                    if (e.message == "y" && vote.service.process) {
                        // 투표가 진행중일때
                        if (vote.service.voted.contains(e.player.uuid)) {
                            e.player.sendMessage(bundle["vote.already-voted"])
                        } else {
                            vote.service.set(e.player.uuid)
                        }
                    } else {
                        if (!playerData.mute) {
                            // 서버간 대화기능 작동
                            if (playerData.crosschat) {
                                when {
                                    configs.clientEnable -> {
                                        client.request(Client.Request.Chat, e.player, e.message)
                                    }
                                    configs.serverEnable -> {
                                        // 메세지를 모든 클라이언트에게 전송함
                                        val msg = "[" + e.player.name + "]: " + e.message
                                        try {
                                            for (ser in server.list) {
                                                ser!!.os.writeBytes(tool.encrypt(msg, ser.spec))
                                                ser.os.flush()
                                            }
                                        } catch (ex: Exception) {
                                            log.warn("Crosschat", ex)
                                        }
                                    }
                                    else -> {
                                        e.player.sendMessage(bundle["no-any-network"])
                                        playerData.crosschat = false
                                    }
                                }
                            }
                        }
                    }
                    if (configs.translate) {
                        Thread {
                            val buf = ArrayMap<String, String>()
                            try {
                                for (p in Vars.playerGroup.all()) {
                                    val target = playerCore[p.uuid]
                                    if (!target.error && !target.mute) {
                                        var original = playerData.locale.language
                                        var language = target.locale.language
                                        if (original == "zh") original = "zh-CN"
                                        if (language == "zh") language = "zh-CN"
                                        var match = false
                                        for (b in buf) {
                                            if (language == b.key) {
                                                match = true
                                                p.sendMessage("[orange][TR] [green]" + e.player.name + "[orange] >[white] " + b.value)
                                                break
                                            }
                                        }
                                        if (language != original && !match) {
                                            val con = URL("https://naveropenapi.apigw.ntruss.com/nmt/v1/translation").openConnection() as HttpURLConnection
                                            con.requestMethod = "POST"
                                            con.setRequestProperty("X-NCP-APIGW-API-KEY-ID", configs.translateId)
                                            con.setRequestProperty("X-NCP-APIGW-API-KEY", configs.translatePw)
                                            con.doOutput = true
                                            try {
                                                DataOutputStream(con.outputStream).use { wr ->
                                                    wr.writeBytes("source=" + original + "&target=" + language + "&text=" + URLEncoder.encode(e.message, "UTF-8"))
                                                    wr.flush()
                                                    wr.close()
                                                    if (con.responseCode != 200) {
                                                        BufferedReader(InputStreamReader(con.errorStream, StandardCharsets.UTF_8)).use { br ->
                                                            var inputLine: String?
                                                            val response = StringBuilder()
                                                            while (br.readLine().also { inputLine = it } != null) {
                                                                response.append(inputLine)
                                                            }
                                                            Log.write(LogType.error, response.toString())
                                                        }
                                                    } else {
                                                        BufferedReader(InputStreamReader(con.inputStream, StandardCharsets.UTF_8)).use { br ->
                                                            var inputLine: String?
                                                            val response = StringBuilder()
                                                            while (br.readLine().also { inputLine = it } != null) {
                                                                response.append(inputLine)
                                                            }
                                                            val `object` = JsonValue.readJSON(response.toString()).asObject()
                                                            val result = `object`["message"].asObject()["result"].asObject()["translatedText"].asString()
                                                            buf.put(language, result)
                                                            p.sendMessage("[orange][TR] [green]" + e.player.name + "[orange] >[white] " + result)
                                                        }
                                                    }
                                                }
                                            } catch (ex: Exception) {
                                                CrashReport(ex)
                                            }
                                        } else {
                                            if (perm.user[playerData.uuid].asObject()["prefix"] != null) {
                                                if (!playerData.crosschat) p.sendMessage(perm.user[playerData.uuid].asObject()["prefix"].asString().replace("%1", NetClient.colorizeName(e.player.id, e.player.name)).replace("%2", e.message))
                                            } else {
                                                if (!playerData.crosschat) p.sendMessage("[orange]" + NetClient.colorizeName(e.player.id, e.player.name) + "[orange] >[white] " + e.message)
                                            }
                                        }
                                    }
                                }
                            } catch (ex: Exception) {
                                CrashReport(ex)
                            }
                        }.start()
                    } else if (NetClient.colorizeName(e.player.id, e.player.name) != null) {
                        Call.sendMessage(perm.user[playerData.uuid].asObject()["prefix"].asString().replace("%1", NetClient.colorizeName(e.player.id, e.player.name)).replace("%2", e.message))
                    }
                }

                // 마지막 대화 데이터를 DB에 저장함
                playerData.lastchat = e.message
            }
        }

        // 플레이어가 블럭을 건설했을 때
        Events.on(BlockBuildEndEvent::class.java) { e: BlockBuildEndEvent ->
            if (e.player == null) return@on   // 만약 건설자가 드론일경우
            Log.write(LogType.block, "log.block.place", e.player.name, e.tile.block().name)
            val target = playerCore[e.player.uuid]
            if (!e.breaking && e.player.buildRequest() != null && !target.error && e.tile.block() != null) {
                val name = e.tile.block().name
                try {
                    val obj = JsonValue.readHjson(pluginRoot.child("Exp.hjson").reader()).asObject()
                    val blockexp = obj.getInt(name, 0)
                    target.lastplacename = e.tile.block().name
                    target.placecount = target.placecount + 1
                    target.exp = target.exp + blockexp
                    if (e.player.buildRequest().block === Blocks.thoriumReactor) target.reactorcount = target.reactorcount + 1
                } catch (ex: Exception) {
                    CrashReport(ex)
                }

                // 메세지 블럭을 설치했을 경우, 해당 블럭을 감시하기 위해 위치를 저장함.
                if (e.tile.block() === Blocks.message) {
                    pluginData.messagemonitors.add(PluginData.MessageMonitor(e.tile.pos()))
                }

                // 플레이어가 토륨 원자로를 만들었을 때, 감시를 위해 그 원자로의 위치를 저장함.
                if (e.tile.block() === Blocks.thoriumReactor) {
                    pluginData.nukeposition.add(e.tile)
                    pluginData.nukedata.add(e.tile)
                }
                if (configs.debug && configs.antiGrief) {
                    Log.info("anti-grief.build.finish", e.player.name, e.tile.block().name, e.tile.x, e.tile.y)
                }

                /*float range = new AntiGrief().getDistanceToCore(e.player, e.tile);
                if (config.antiGrief() && range < 35 && e.tile.block() == Blocks.thoriumReactor) {
                    e.player.sendMessage(new Bundle(target.locale).get("anti-grief.reactor.close"));
                    Call.onDeconstructFinish(e.tile, Blocks.air, e.player.id);
                } else if (config.antiGrief()) {
                    for (int rot = 0; rot < 4; rot++) {
                        if (e.tile.getNearby(rot).block() != Blocks.liquidTank &&
                                e.tile.getNearby(rot).block() != Blocks.conduit &&
                                e.tile.getNearby(rot).block() != Blocks.bridgeConduit &&
                                e.tile.getNearby(rot).block() != Blocks.phaseConduit &&
                                e.tile.getNearby(rot).block() != Blocks.platedConduit &&
                                e.tile.getNearby(rot).block() != Blocks.pulseConduit) {
                            // TODO 냉각수 감지 추가
                            Call.sendMessage("No cryofluid reactor detected");
                        }
                    }
                }*/
            }
        }

        // 플레이어가 블럭을 뽀갰을 때
        Events.on(BuildSelectEvent::class.java) { e: BuildSelectEvent ->
            if (e.builder is Player && e.builder.buildRequest() != null && !Pattern.matches(".*build.*", e.builder.buildRequest().block.name) && e.tile.block() !== Blocks.air) {
                if (e.breaking) {
                    Log.write(LogType.block, "log.block.remove", (e.builder as Player).name, e.tile.block().name, e.tile.x, e.tile.y)
                    val target = playerCore[(e.builder as Player).uuid]
                    val name = e.tile.block().name
                    try {
                        val obj = JsonValue.readHjson(pluginRoot.child("Exp.hjson").reader()).asObject()
                        val blockexp = obj.getInt(name, 0)
                        target.lastbreakname = e.tile.block().name
                        target.breakcount = target.breakcount + 1
                        target.exp = target.exp + blockexp
                    } catch (ex: Exception) {
                        CrashReport(ex)
                        Call.onKick((e.builder as Player).con, Bundle(target.locale)["not-logged"])
                    }

                    // 메세지 블럭을 파괴했을 때, 위치가 저장된 데이터를 삭제함
                    if (e.builder.buildRequest().block === Blocks.message) {
                        try {
                            for (i in 0 until pluginData.powerblocks.size) {
                                if (pluginData.powerblocks[i].pos == e.tile.pos()) {
                                    pluginData.powerblocks.remove(i)
                                    break
                                }
                            }
                        } catch (ex: Exception) {
                            CrashReport(ex)
                        }
                    }

                    // Exp Playing Game (EPG)
                    if (configs.expLimit) {
                        val level = target.level
                        try {
                            val obj = JsonValue.readHjson(pluginRoot.child("Exp.hjson").reader()).asObject()
                            if (obj[name] != null) {
                                val blockreqlevel = obj.getInt(name, 999)
                                if (level < blockreqlevel) {
                                    Call.onDeconstructFinish(e.tile, e.tile.block(), (e.builder as Player).id)
                                    (e.builder as Player).sendMessage(Bundle(playerCore[(e.builder as Player).uuid].locale)["system.epg.block-require", name, blockreqlevel])
                                }
                            } else {
                                Log.err("system.epg.block-not-valid", name)
                            }
                        } catch (ex: Exception) {
                            CrashReport(ex)
                        }
                    }
                }
                if (configs.debug && configs.antiGrief) {
                    Log.info("anti-grief.destroy", (e.builder as Player).name, e.tile.block().name, e.tile.x, e.tile.y)
                }
            }
        }

        // 유닛을 박살냈을 때
        Events.on(UnitDestroyEvent::class.java) { e: UnitDestroyEvent ->
            // 뒤진(?) 유닛이 플레이어일때
            if (e.unit is Player) {
                val player = e.unit as Player
                val target = playerCore[player.uuid]
                if (!Vars.state.teams[player.team].cores.isEmpty) target.deathcount = target.deathcount + 1
            }

            // 터진 유닛수만큼 카운트해줌
            if (Vars.playerGroup != null && Vars.playerGroup.size() > 0) {
                for (i in 0 until Vars.playerGroup.size()) {
                    val player = Vars.playerGroup.all()[i]
                    val target = playerCore[player.uuid]
                    if (!Vars.state.teams[player.team].cores.isEmpty) target.killcount = target.killcount + 1
                }
            }
        }

        // 플레이어가 밴당했을 때 공유기능 작동
        Events.on(PlayerBanEvent::class.java) { e: PlayerBanEvent ->
            val bansharing = Thread {
                if (configs.banShare && configs.clientEnable) {
                    client.request(Client.Request.BanSync, null, null)
                }
            }
            for (player in Vars.playerGroup.all()) {
                if (player === e.player) {
                    tool.sendMessageAll("player.banned", e.player.name)
                    if (Vars.netServer.admins.isIDBanned(player.uuid)) {
                        player.con.kick(Packets.KickReason.banned)
                    }
                }
            }
            mainThread.submit(bansharing)
        }

        // 이건 IP 밴당했을때 작동
        Events.on(PlayerIpBanEvent::class.java) {
            val bansharing = Thread {
                if (configs.banShare && client.activated) {
                    client.request(Client.Request.BanSync, null, null)
                }
            }
            mainThread.submit(bansharing)
        }

        // 이건 밴 해제되었을 때 작동
        Events.on(PlayerUnbanEvent::class.java) { e: PlayerUnbanEvent -> if (client.activated) client.request(Client.Request.UnbanID, null, e.player.uuid + "|<unknown>") }

        // 이건 IP 밴이 해제되었을 때 작동
        Events.on(PlayerIpUnbanEvent::class.java) { e: PlayerIpUnbanEvent -> if (client.activated) client.request(Client.Request.UnbanIP, null, "<unknown>|" + e.ip) }
        Events.on(ServerLoadEvent::class.java) { _: ServerLoadEvent? ->
            // 업데이트 확인
            if (configs.update) {
                Log.client("client.update-check")
                try {
                    val json = JsonValue.readJSON(tool.getWebContent("https://api.github.com/repos/kieaer/Essentials/releases/latest")).asObject()
                    for (a in 0 until Vars.mods.list().size) {
                        if (Vars.mods.list()[a].meta.name == "Essentials") {
                            pluginVars.pluginVersion = Vars.mods.list()[a].meta.version
                        }
                    }
                    val latest = DefaultArtifactVersion(json.getString("tag_name", pluginVars.pluginVersion))
                    val current = DefaultArtifactVersion(pluginVars.pluginVersion)
                    when {
                        latest > current -> {
                            Log.client("version-new")
                            val t = Thread {
                                try {
                                    Log.info(Bundle()["update-description", json["tag_name"]])
                                    println(json.getString("body", "No description found."))
                                    println(Bundle()["plugin-downloading-standby"])
                                    timer.cancel()
                                    if (configs.serverEnable) {
                                        try {
                                            for (ser in server.list) {
                                                ser!!.interrupt()
                                                ser.os.close()
                                                ser.br.close()
                                                ser.socket.close()
                                                server.list.remove(ser)
                                            }
                                            server.shutdown()
                                        } catch (ignored: Exception) {
                                        }
                                    }
                                    if (configs.clientEnable && client.activated) {
                                        client.request(Client.Request.Exit, null, null)
                                    }
                                    mainThread.shutdown()
                                    playerCore.dispose()
                                    println(Bundle()["plugin-downloading"])
                                    tool.download(URL(json["assets"].asArray()[0].asObject().getString("browser_download_url", null)),
                                            Core.settings.dataDirectory.child("mods/Essentials.jar").file())
                                } catch (ex: Exception) {
                                    println(Bundle()["plugin-downloading-fail"].trimIndent())
                                    CrashReport(ex)
                                }

                                // TODO make checksum
                                /*try {
                                            checksum sum = new checksum();
                                            sum.check()
                                            System.out.println(new Bundle().get("plugin-downloading-done"));
                                        } catch (Exception ignored){}*/Core.app.exit()
                            }
                            t.start()
                        }
                        latest.compareTo(current) == 0 -> {
                            Log.client("version-current")
                        }
                        latest.compareTo(current) < 0 -> {
                            Log.client("version-devel")
                        }
                    }
                } catch (ex: Exception) {
                    CrashReport(ex)
                }
            } else {
                for (a in 0 until Vars.mods.list().size) {
                    if (Vars.mods.list()[a].meta.name == "Essentials") {
                        pluginVars.pluginVersion = Vars.mods.list()[a].meta.version
                        break
                    }
                }
            }

            // Discord 봇 시작
            if (configs.passwordMethod == "discord" || configs.passwordMethod == "mixed") {
                discord.start()
            }
        }
    }
}