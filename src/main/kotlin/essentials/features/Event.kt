package essentials.features

import arc.Core
import arc.Events
import essentials.*
import essentials.Main.Companion.mainThread
import essentials.Main.Companion.pluginRoot
import essentials.Main.Companion.timer
import essentials.external.IpAddressMatcher
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.internal.Log.LogType
import essentials.internal.Tool
import essentials.network.Client
import essentials.network.Server
import mindustry.Vars
import mindustry.Vars.world
import mindustry.content.Blocks
import mindustry.core.NetClient
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Nulls
import mindustry.gen.Playerc
import mindustry.net.Packets
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.hjson.JsonValue
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.regex.Pattern
import kotlin.math.abs

object Event {
    fun register() {
        Events.on(ConfigEvent::class.java) { e: ConfigEvent ->
            if (e.tile != null && e.tile.block() != null && e.player != null && Config.alertAction) {
                for (p in Groups.player) {
                    val playerData = PlayerCore[p.uuid()]
                    if (playerData.alert) {
                        p.sendMessage(Bundle(playerData.locale)["tap-config", e.player.name, e.tile.block().name])
                    }
                }
                if (Config.debug) Log.info("anti-grief.build.config", e.player.name, e.tile.block().name, e.tile.pos())
                if (Config.logging) Log.write(LogType.Tap, "log.tap-config", e.player.name, e.tile.block().name)

                // Source by BasedUser(router)
                val entity = e.tile
                val value = e.value
                val other = world.tile(value as Int)
                val valid = other != null && other.block().hasPower
                if (valid) {
                    val oldGraph = entity.power.graph
                    val newgraph = other.build.power.graph
                    val oldGraphCount = oldGraph.toString().substring(oldGraph.toString().indexOf("all={"), oldGraph.toString().indexOf("}, l")).replaceFirst("all=\\{".toRegex(), "").split(",").toTypedArray().size
                    val newGraphCount = newgraph.toString().substring(newgraph.toString().indexOf("all={"), newgraph.toString().indexOf("}, l")).replaceFirst("all=\\{".toRegex(), "").split(",").toTypedArray().size
                    if (abs(oldGraphCount - newGraphCount) > 10) {
                        //Tool.sendMessageAll("anti-grief.powernode", e.player.name, "[green] " + Math.max(oldGraphCount, newGraphCount) + " [cyan]->[scarlet] " + Math.min(oldGraphCount, newGraphCount) + " [white](" + e.tile.x + ", " + e.tile.y + ")");
                        Call.sendMessage(e.player.name + " [white]player has [scarlet]unlinked[] the [yellow]power node[]. Number of connected buildings: [green] " + oldGraphCount.coerceAtLeast(newGraphCount) + " [cyan]->[scarlet] " + oldGraphCount.coerceAtMost(newGraphCount) + " [white](" + e.tile.x + ", " + e.tile.y + ")")
                    }
                }
            }
        }
        /*Events.on(TapEvent::class.java) { e: TapEvent ->
            if (Config.logging) Log.write(LogType.tap, "log.tap", e.player.name, e.tile.block().name)
            val playerData = PlayerCore[e.player.uuid]
            if (!playerData.error) {
                for (data in PluginData.warpblocks) {
                    if (e.tile.x >= world.tile(data.pos).link().x && e.tile.x <= world.tile(data.pos).link().x) {
                        if (e.tile.y >= world.tile(data.pos).link().y && e.tile.y <= world.tile(data.pos).link().y) {
                            if (data.online) {
                                Log.info("player.warped", e.player.name, data.ip + ":" + data.port)
                                playerData.connected = false
                                playerData.connserver = "none"
                                Call.Connect(e.player.con, data.ip, data.port)
                            }
                            break
                        }
                    }
                }

                for (PluginData.warpzone data : PluginData.warpzones) {
                    if (e.tile.x > data.getStartTile().x && e.tile.x < data.getFinishTile().x) {
                        if (e.tile.y > data.getStartTile().y && e.tile.y < data.getFinishTile().y) {
                            Log.info("player.warped", e.player.name, data.ip + ":" + data.port);
                            playerData.connected(false);
                            playerData.connserver("none");
                            Call.Connect(e.player.con, data.ip, data.port);
                            break;
                        }
                    }
                }
            }
        }*/
        Events.on(WithdrawEvent::class.java) { e: WithdrawEvent ->
            if (e.tile != null && e.player.miner().item() != null && e.player.name != null && Config.antiGrief) {
                for (p in Groups.player) {
                    val playerData = PlayerCore[p.uuid()]
                    if (playerData.alert) {
                        p.sendMessage(Bundle(playerData.locale)["log.withdraw", e.player.name, e.player.miner().item().name, e.amount, e.tile.block().name])
                    }
                }
                if (Config.debug) Log.info("log.withdraw", e.player.name, e.player.miner().item().name, e.amount, e.tile.block().name)
                if (Config.logging) Log.write(LogType.WithDraw, "log.withdraw", e.player.name, e.player.miner().item().name, e.amount, e.tile.block().name)
                if (Vars.state.rules.pvp) {
                    if (e.item.flammability > 0.001f) {
                        e.player.sendMessage(Bundle(PlayerCore[e.player.uuid()].locale)["system.flammable.disabled"])
                        e.player.miner().clearItem()
                    }
                }
            }
        }

        // 게임오버가 되었을 때
        Events.on(GameOverEvent::class.java) { e: GameOverEvent ->
            if (Vars.state.rules.pvp) {
                var index = 5
                for (a in 0..4) {
                    if (Vars.state.teams[Team.all[index]].cores.isEmpty) {
                        index--
                    }
                }
                if (index == 1) {
                    for (i in 0 until Groups.player.size()) {
                        val player = Groups.player.getByID(i)
                        val target = PlayerCore[player.uuid()]
                        if (target.login) {
                            if (player.team().name == e.winner.name) {
                                target.pvpwincount = target.pvpwincount + 1
                            } else if (player.team().name != e.winner.name) {
                                target.pvplosecount = target.pvplosecount + 1
                            }
                        }
                    }
                }
            } else if (Vars.state.rules.attackMode) {
                for (p in Groups.player) {
                    val target = PlayerCore[p.uuid()]
                    if (target.login) {
                        target.attackclear = target.attackclear + 1
                    }
                }
            }
        }

        // 맵이 불러와졌을 때
        Events.on(WorldLoadEvent::class.java) {
            PluginVars.playtime = 0L

            // 전력 노드 정보 초기화
            PluginData.powerblocks.clear()
        }
        Events.on(PlayerConnect::class.java) { e: PlayerConnect ->
            if (Config.logging) Log.write(LogType.Player, "log.player.connect", e.player.name, e.player.uuid(), e.player.con.address)

            // 닉네임이 블랙리스트에 등록되어 있는지 확인
            for (s in PluginData.blacklist) {
                if (e.player.name.matches(Regex(s))) {
                    try {
                        val locale = Tool.getGeo(e.player)
                        Call.kick(e.player.con, Bundle(locale)["system.nickname.blacklisted.kick"])
                        Log.info("system.nickname.blacklisted", e.player.name)
                    } catch (ex: Exception) {
                        CrashReport(ex)
                    }
                }
            }
            if (Config.strictName) {
                if (e.player.name.length > 32) Call.kick(e.player.con, "Nickname too long!")
                //if (e.player.name.matches(".*\\[.*].*"))
                //    Call.Kick(e.player.con, "Color tags can't be used for nicknames on this Server.");
                if (e.player.name.contains("　")) Call.kick(e.player.con, "Don't use blank speical charactor nickname!")
                if (e.player.name.contains(" ")) Call.kick(e.player.con, "Nicknames can't be used on this server!")
                if (Pattern.matches(".*\\[.*.].*", e.player.name)) Call.kick(e.player.con, "Can't use only color tags nickname in this Server.")
            }
        }

        // 플레이어가 아이템을 특정 블록에다 직접 가져다 놓았을 때
        Events.on(DepositEvent::class.java) { e: DepositEvent ->
            for (p in Groups.player) {
                val playerData = PlayerCore[p.uuid()]
                if (playerData.alert) {
                    p.sendMessage(Bundle(playerData.locale)["anti-grief.deposit", e.player.name, e.player.miner().item().name, e.tile.block().name])
                }
            }
            if (Config.logging) Log.write(LogType.Deposit, "log.deposit", e.player.name, e.player.miner().item().name, e.tile.block().name)
        }

        // 플레이어가 서버에 들어왔을 때
        Events.on(PlayerJoin::class.java) { e: PlayerJoin ->
            if (Config.logging) Log.write(LogType.Player, "log.player.join", e.player.name, e.player.uuid(), e.player.con.address)
            PluginVars.players.add(e.player)
            e.player.admin(false)
            val t = Thread {
                Thread.currentThread().name = e.player.name + " Player Join thread"
                val playerData = PlayerCore.load(e.player.uuid(), null)
                val bundle = Bundle(playerData.locale)
                if (Config.loginEnable) {
                    if (Config.passwordMethod == "mixed") {
                        if (!playerData.error && Config.autoLogin) {
                            if (playerData.udid != 0L) {
                                Thread { Call.connect(e.player.con, PluginVars.serverIP, 7060) }.start()
                            } else {
                                e.player.sendMessage(bundle["account.autologin"])
                                PlayerCore.playerLoad(e.player, null)
                            }
                        } else {
                            val lc = Tool.getGeo(e.player)
                            if (PlayerCore.register(e.player.name, e.player.uuid(), lc.displayCountry, lc.toString(), lc.displayLanguage, PluginVars.serverIP, "default", 0L, e.player.name, "none", false)) {
                                PlayerCore.playerLoad(e.player, null)
                            } else {
                                Call.kick(e.player.con, Bundle()["plugin-error-kick"])
                            }
                        }
                    } else if (Config.passwordMethod == "discord") {
                        if (!playerData.error && Config.autoLogin) {
                            e.player.sendMessage(bundle["account.autologin"])
                            PlayerCore.playerLoad(e.player, null)
                        } else {
                            val message: String?
                            val language = Tool.getGeo(e.player)
                            message = if (Config.passwordMethod == "discord") {
                                """${Bundle(language)["system.login.require.discord"]}
                                    ${Config.discordLink}
                                    """.trimIndent()
                            } else {
                                Bundle(language)["system.login.require.password"]
                            }
                            Call.infoMessage(e.player.con, message)
                        }
                    } else {
                        if (!playerData.error && Config.autoLogin) {
                            e.player.sendMessage(bundle["account.autologin"])
                            PlayerCore.playerLoad(e.player, null)
                        } else {
                            val message: String?
                            val language = Tool.getGeo(e.player)
                            message = if (Config.passwordMethod == "discord") {
                                """${Bundle(language)["system.login.require.discord"]}
                                    ${Config.discordLink}
                                    """.trimIndent()
                            } else {
                                Bundle(language)["system.login.require.password"]
                            }
                            Call.infoMessage(e.player.con, message)
                        }
                    }
                } else {
                    // 로그인 기능이 꺼져있을 때, 바로 계정 등록을 하고 데이터를 로딩함
                    if (!playerData.error && Config.autoLogin) {
                        e.player.sendMessage(bundle["account.autologin"])
                        PlayerCore.playerLoad(e.player, null)
                    } else {
                        val lc = Tool.getGeo(e.player.con.address)
                        val register = PlayerCore.register(e.player.name, e.player.uuid(), lc.displayCountry, lc.toString(), lc.displayLanguage, PluginVars.serverIP, "default", 0L, e.player.name, "none", false)
                        if (!register || PlayerCore.playerLoad(e.player, null)) {
                            Call.kick(e.player.con, Bundle()["plugin-error-kick"])
                        }
                    }
                }

                // VPN을 사용중인지 확인
                if (Config.antiVPN) {
                    val br = BufferedReader(InputStreamReader(javaClass.getResourceAsStream("/ipv4.txt")))
                    br.use {
                        var line: String
                        while (br.readLine().also { line = it } != null) {
                            val match = IpAddressMatcher(line)
                            if (match.matches(e.player.con.address)) {
                                Call.kick(e.player.con, Bundle()["anti-grief.vpn"])
                            }
                        }
                    }
                }


                // PvP 평화시간 설정
                if (Config.antiRush && Vars.state.rules.pvp && PluginVars.playtime < Config.antiRushtime) {
                    Vars.state.rules.unitDamageMultiplier = 0f
                    Vars.state.rules.unitHealthMultiplier = 0.001f
                    Call.setRules(Vars.state.rules)
                    PluginVars.isPvPPeace = true
                }
            }
            t.start()
        }

        // 플레이어가 서버에서 탈주했을 때
        Events.on(PlayerLeave::class.java) { e: PlayerLeave ->
            if (Config.logging) Log.write(LogType.Player, "log.player.leave", e.player.name, e.player.uuid(), e.player.con.address)
            val player = PlayerCore[e.player.uuid()]
            if (player.login) {
                player.connected = false
                player.connserver = "none"
                if (Vars.state.rules.pvp && !Vars.state.gameOver) player.pvpbreakout = player.pvpbreakout + 1
            }
            PlayerCore.save(player)
            PluginVars.playerData.find { p: PlayerData -> p.uuid == e.player.uuid() }?.let { PluginVars.removePlayerData(it) }
            PluginVars.players.remove(e.player)
        }

        // 플레이어가 수다떨었을 때
        Events.on(PlayerChatEvent::class.java) { e: PlayerChatEvent ->
            if (Config.antiGrief && (e.message.length > Vars.maxTextLength || e.message.contains("Nexity#2671"))) {
                Call.kick(e.player.con, "Hacked client detected")
            }
            val playerData = PlayerCore[e.player.uuid()]
            val bundle = Bundle(playerData.locale)
            if (!e.message.startsWith("/")) Log.info("<&y" + e.player.name + ": &lm" + e.message + "&lg>")
            if (!playerData.error) {
                // 명령어인지 확인
                if (!e.message.startsWith("/")) {
                    if (e.message == "y" && Vote.service.process) {
                        // 투표가 진행중일때
                        if (Vote.service.voted.contains(e.player.uuid())) {
                            e.player.sendMessage(bundle["vote.already-voted"])
                        } else {
                            Vote.service.set(e.player.uuid())
                        }
                    } else {
                        if (!playerData.mute) {
                            // 서버간 대화기능 작동
                            if (playerData.crosschat) {
                                when {
                                    Config.clientEnable -> {
                                        Client.request(Client.Request.Chat, e.player, e.message)
                                    }
                                    Config.serverEnable -> {
                                        // 메세지를 모든 클라이언트에게 전송함
                                        val msg = "[" + e.player.name + "]: " + e.message
                                        try {
                                            for (ser in Server.list) {
                                                ser!!.os.writeBytes(Tool.encrypt(msg, ser.spec))
                                                ser.os.flush()
                                            }
                                        } catch (ex: Exception) {
                                            Log.warn("Crosschat", ex)
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
                    if (NetClient.colorizeName(e.player.id, e.player.name) != null) {
                        Call.sendMessage(Permissions.user[playerData.uuid].asObject()["prefix"].asString().replace("%1", NetClient.colorizeName(e.player.id, e.player.name)).replace("%2", e.message))
                    }
                }

                // 마지막 대화 데이터를 DB에 저장함
                playerData.lastchat = e.message
            }
        }

        // 플레이어가 블럭을 건설했을 때
        Events.on(BlockBuildEndEvent::class.java) { e: BlockBuildEndEvent ->
            if (!e.unit.isNull && e.unit.isPlayer) {
                val player = e.unit.player

                Log.write(LogType.Block, "log.block.place", player.name, e.tile.block().name)
                val target = PlayerCore[player.uuid()]
                if (!e.breaking && player.builder().buildPlan().block != null && !target.error && e.tile.block() != null) {
                    val name = e.tile.block().name
                    try {
                        val obj = JsonValue.readHjson(pluginRoot.child("Exp.hjson").reader()).asObject()
                        val blockexp = obj.getInt(name, 0)
                        target.lastplacename = e.tile.block().name
                        target.placecount = target.placecount + 1
                        target.exp = target.exp + blockexp
                        if (player.builder().buildPlan().block === Blocks.thoriumReactor) target.reactorcount = target.reactorcount + 1
                    } catch (ex: Exception) {
                        CrashReport(ex)
                    }

                    // 메세지 블럭을 설치했을 경우, 해당 블럭을 감시하기 위해 위치를 저장함.
                    if (e.tile.block() === Blocks.message) {
                        PluginData.messagemonitors.add(PluginData.MessageMonitor(e.tile.pos()))
                    }

                    // 플레이어가 토륨 원자로를 만들었을 때, 감시를 위해 그 원자로의 위치를 저장함.
                    if (e.tile.block() === Blocks.thoriumReactor) {
                        PluginData.nukeposition.add(e.tile)
                        PluginData.nukedata.add(e.tile)
                    }
                    if (Config.debug && Config.antiGrief) {
                        Log.info("anti-grief.build.finish", player.name, e.tile.block().name, e.tile.x, e.tile.y)
                    }

                    /*float range = new AntiGrief().getDistanceToCore(e.player, e.tile);
                if (config.antiGrief() && range < 35 && e.tile.block() == Blocks.thoriumReactor) {
                    e.player.sendMessage(new Bundle(target.locale).get("anti-grief.reactor.close"));
                    Call.DeconstructFinish(e.tile, Blocks.air, e.player.id);
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
        }

        // 플레이어가 블럭을 뽀갰을 때
        Events.on(BuildSelectEvent::class.java) { e: BuildSelectEvent ->
            if (e.builder is Playerc && e.builder.buildPlan() != null && !Pattern.matches(".*build.*", e.builder.buildPlan().block.name) && e.tile.block() !== Blocks.air) {
                if (e.breaking) {
                    Log.write(LogType.Block, "log.block.remove", (e.builder as Playerc).name(), e.tile.block().name, e.tile.x, e.tile.y)
                    val target = PlayerCore[(e.builder as Playerc).uuid()]
                    val name = e.tile.block().name
                    try {
                        val obj = JsonValue.readHjson(pluginRoot.child("Exp.hjson").reader()).asObject()
                        val blockexp = obj.getInt(name, 0)
                        target.lastbreakname = e.tile.block().name
                        target.breakcount = target.breakcount + 1
                        target.exp = target.exp + blockexp
                    } catch (ex: Exception) {
                        CrashReport(ex)
                        Call.kick((e.builder as Playerc).con(), Bundle(target.locale)["not-logged"])
                    }

                    // 메세지 블럭을 파괴했을 때, 위치가 저장된 데이터를 삭제함
                    if (e.builder.buildPlan().block === Blocks.message) {
                        try {
                            for (i in 0 until PluginData.powerblocks.size) {
                                if (PluginData.powerblocks[i].pos == e.tile.pos()) {
                                    PluginData.powerblocks.remove(i)
                                    break
                                }
                            }
                        } catch (ex: Exception) {
                            CrashReport(ex)
                        }
                    }

                    // Exp Playing Game (EPG)
                    if (Config.expLimit) {
                        val level = target.level
                        try {
                            val obj = JsonValue.readHjson(pluginRoot.child("Exp.hjson").reader()).asObject()
                            if (obj[name] != null) {
                                val blockreqlevel = obj.getInt(name, 999)
                                if (level < blockreqlevel) {
                                    Call.deconstructFinish(e.tile, e.tile.block(), (e.builder as Playerc).unit())
                                    (e.builder as Playerc).sendMessage(Bundle(PlayerCore[(e.builder as Playerc).uuid()].locale)["system.epg.block-require", name, blockreqlevel])
                                }
                            } else {
                                Log.err("system.epg.block-not-valid", name)
                            }
                        } catch (ex: Exception) {
                            CrashReport(ex)
                        }
                    }
                }
                if (Config.debug && Config.antiGrief) {
                    Log.info("anti-grief.destroy", (e.builder as Playerc).name(), e.tile.block().name, e.tile.x, e.tile.y)
                }
            }
        }

        // 유닛을 박살냈을 때
        Events.on(UnitDestroyEvent::class.java) { e: UnitDestroyEvent ->
            // 뒤진(?) 유닛이 플레이어일때
            if (e.unit is Playerc) {
                val player = e.unit as Playerc
                val target = PlayerCore[player.uuid()]
                if (!Vars.state.teams[player.team()].cores.isEmpty) target.deathcount = target.deathcount + 1
            }

            // 터진 유닛수만큼 카운트해줌
            if (Groups.player != null && Groups.player.size() > 0) {
                for (p in Groups.player) {
                    val target = PlayerCore[p.uuid()]
                    if (!Vars.state.teams[p.team()].cores.isEmpty) target.killcount = target.killcount + 1
                }
            }
        }

        // 플레이어가 밴당했을 때 공유기능 작동
        Events.on(PlayerBanEvent::class.java) { e: PlayerBanEvent ->
            val bansharing = Thread {
                if (Config.banShare && Config.clientEnable) {
                    Client.request(Client.Request.BanSync, Nulls.player, null)
                }
            }
            for (player in Groups.player) {
                if (player === e.player) {
                    Tool.sendMessageAll("player.banned", e.player.name)
                    if (Vars.netServer.admins.isIDBanned(player.uuid())) {
                        player.con.kick(Packets.KickReason.banned)
                    }
                }
            }
            mainThread.submit(bansharing)
        }

        // 이건 IP 밴당했을때 작동
        Events.on(PlayerIpBanEvent::class.java) {
            val bansharing = Thread {
                if (Config.banShare && Client.activated) {
                    Client.request(Client.Request.BanSync, Nulls.player, null)
                }
            }
            mainThread.submit(bansharing)
        }

        // 이건 밴 해제되었을 때 작동
        Events.on(PlayerUnbanEvent::class.java) { e: PlayerUnbanEvent -> if (Client.activated) Client.request(Client.Request.UnbanID, null, e.player.uuid() + "|<unknown>") }

        // 이건 IP 밴이 해제되었을 때 작동
        Events.on(PlayerIpUnbanEvent::class.java) { e: PlayerIpUnbanEvent -> if (Client.activated) Client.request(Client.Request.UnbanIP, null, "<unknown>|" + e.ip) }
        Events.on(ServerLoadEvent::class.java) { _: ServerLoadEvent? ->
            // 업데이트 확인
            if (Config.update) {
                Log.client("client.update-check")
                try {
                    val web = Tool.getWebContent("https://api.github.com/repos/kieaer/Essentials/releases/latest")
                    if(web != null) {
                        val json = JsonValue.readJSON(web).asObject()
                        for (a in 0 until Vars.mods.list().size) {
                            if (Vars.mods.list()[a].meta.name == "Essentials") {
                                PluginVars.pluginVersion = Vars.mods.list()[a].meta.version
                            }
                        }
                        val latest = DefaultArtifactVersion(json.getString("tag_name", PluginVars.pluginVersion))
                        val current = DefaultArtifactVersion(PluginVars.pluginVersion)
                        when {
                            latest > current -> {
                                Log.client("version-new")
                                val t = Thread {
                                    try {
                                        Log.info(Bundle()["update-description", json["tag_name"]])
                                        println(json.getString("body", "No description found."))
                                        println(Bundle()["plugin-downloading-standby"])
                                        timer.cancel()
                                        if (Config.serverEnable) {
                                            try {
                                                for (ser in Server.list) {
                                                    ser!!.interrupt()
                                                    ser.os.close()
                                                    ser.br.close()
                                                    ser.socket.close()
                                                    Server.list.remove(ser)
                                                }
                                                Server.shutdown()
                                            } catch (ignored: Exception) {
                                            }
                                        }
                                        if (Config.clientEnable && Client.activated) {
                                            Client.request(Client.Request.Exit, null, null)
                                        }
                                        mainThread.shutdown()
                                        PlayerCore.dispose()
                                        println(Bundle()["plugin-downloading"])
                                        Tool.download(URL(json["assets"].asArray()[0].asObject().getString("browser_download_url", null)),
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
                            latest < current -> {
                                Log.client("version-devel")
                            }
                        }
                    }
                } catch (ex: Exception) {
                    CrashReport(ex)
                }
            } else {
                for (a in 0 until Vars.mods.list().size) {
                    if (Vars.mods.list()[a].meta.name == "Essentials") {
                        PluginVars.pluginVersion = Vars.mods.list()[a].meta.version
                        break
                    }
                }
            }

            // Discord 봇 시작
            if (Config.passwordMethod == "discord" || Config.passwordMethod == "mixed") {
                Discord.start()
            }
        }
    }
}