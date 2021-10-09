package essentials.event

import arc.Core
import arc.Events
import arc.util.Strings
import essentials.Main
import essentials.PlayerData
import essentials.PluginData
import essentials.data.Config
import essentials.data.DB
import essentials.data.PlayerCore
import essentials.data.auth.Discord
import essentials.eof.sendMessage
import essentials.event.feature.Exp
import essentials.event.feature.Permissions
import essentials.external.IpAddressMatcher
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.internal.Tool
import essentials.network.Client
import essentials.network.Server
import mindustry.Vars
import mindustry.content.Blocks
import mindustry.core.GameState
import mindustry.core.NetClient
import mindustry.game.EventType
import mindustry.game.EventType.*
import mindustry.game.Team
import mindustry.gen.*
import mindustry.net.Packets
import org.apache.maven.artifact.versioning.DefaultArtifactVersion
import org.hjson.JsonValue
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern
import kotlin.math.abs

object Event {
    var tick: Int = 0

    fun test(hitter: Bullet){
        hitter.owner
    }

    fun register() { // 플레이어가 블록에 대해 어떠한 설정을 했을 때 작동
        Events.on(ConfigEvent::class.java) {
            if (it.tile != null && it.tile.block() != null && it.player != null && it.value is Int) { // Source by BasedUser(router)
                val entity = it.tile
                val other = Vars.world.tile(it.value as Int)
                val valid = other != null && entity.power != null && other.block().hasPower
                if (valid) {
                    val oldGraph = entity.power.graph
                    val newGraph = other.build.power.graph
                    val oldGraphCount = oldGraph.toString()
                        .substring(oldGraph.toString().indexOf("all=["), oldGraph.toString().indexOf("], l"))
                        .replaceFirst("all=\\[".toRegex(), "").split(",").toTypedArray().size
                    val newGraphCount = newGraph.toString()
                        .substring(newGraph.toString().indexOf("all=["), newGraph.toString().indexOf("], l"))
                        .replaceFirst("all=\\[".toRegex(), "").split(",").toTypedArray().size
                    if (abs(oldGraphCount - newGraphCount) > 10) {
                        Call.sendMessage(
                            "${it.player.name} [white]player has [scarlet]unlinked[] the [yellow]power node[]. Number of connected buildings: [green] ${
                                oldGraphCount.coerceAtLeast(
                                    newGraphCount
                                )
                            } [cyan]->[scarlet] ${oldGraphCount.coerceAtMost(newGraphCount)} [white](" + it.tile.x + ", " + it.tile.y + ")"
                        )
                    }
                }
            }
        }

        // 플레이어가 블록을 터치하거나 클릭했을 때 작동
        Events.on(TapEvent::class.java) {
            Log.write(Log.LogType.Tap, "log.tap", it.player.name, it.tile.block().name)
            val playerData = PluginData[it.player.uuid()]
            if (playerData != null) {
                for (data in PluginData.warpblocks) {
                    if (it.tile.x >= Vars.world.tile(data.pos).x && it.tile.x <= Vars.world.tile(data.pos).x && it.tile.y >= Vars.world.tile(
                            data.pos
                        ).y && it.tile.y <= Vars.world.tile(data.pos).y
                    ) {
                        if (data.online) {
                            Log.info("player.warped", it.player.name, data.ip + ":" + data.port)
                            Call.connect(it.player.con(), data.ip, data.port)
                        }
                        Bundle()["register-success"]
                        break
                    }
                }

                for (data in PluginData.warpzones) {
                    if (it.tile.x > data.startTile.x && it.tile.x < data.finishTile.x && it.tile.y > data.startTile.y && it.tile.y < data.finishTile.y) {
                        Log.info("player.warped", it.player.name, data.ip + ":" + data.port)
                        Call.connect(it.player.con(), data.ip, data.port)
                        break
                    }
                }
            }
        }

        Events.on(WithdrawEvent::class.java) {
            if (it.tile != null && it.player.unit().item() != null && it.player.name != null) {
                Log.write(
                    Log.LogType.WithDraw,
                    "log.withdraw",
                    it.player.name,
                    it.player.unit().item().name,
                    it.amount.toString(),
                    it.tile.block().name
                )
                if (Vars.state.rules.pvp && it.item.flammability > 0.001f) {
                    it.player.sendMessage(Bundle(PluginData[it.player.uuid()])["system.flammable.disabled"])
                    it.player.unit().clearItem()
                }
            }
        }

        // 게임오버가 되었을 때 작동
        Events.on(GameOverEvent::class.java) {
            if (Vars.state.rules.pvp) {
                var index = 5
                for (a in 0..4) {
                    if (Vars.state.teams[Team.all[index]].cores.isEmpty) {
                        index--
                    }
                }
                if (index == 1) {
                    for (player in Groups.player) {
                        val target = PluginData[player.uuid()]
                        if (target != null) {
                            if (player.team().name == it.winner.name) {
                                target.pvpwincount++
                            } else if (player.team().name != it.winner.name) {
                                target.pvplosecount++
                            }
                        }
                    }
                }
            } else if (Vars.state.rules.attackMode) {
                for (p in Groups.player) {
                    val target = PluginData[p.uuid()]
                    if (target != null) target.attackclear++
                }
            }
        }

        Events.on(WorldLoadEvent::class.java) {
            PluginData.playtime = 0L
        }

        Events.on(PlayerConnect::class.java) {
            Log.write(Log.LogType.Player, "log.player.connect", it.player.name, it.player.uuid(), it.player.con.address)

            // 닉네임이 블랙리스트에 등록되어 있는지 확인
            for (s in PluginData.blacklist) {
                if (it.player.name.matches(Regex(s))) {
                    try {
                        val locale = Tool.getGeo(it.player)
                        Call.kick(it.player.con, Bundle(locale)["system.nickname.blacklisted.kick"])
                        Log.info("system.nickname.blacklisted", it.player.name)
                    } catch (ex: Exception) {
                        CrashReport(ex)
                    }
                }
            }
            if (Config.nameFixed) {
                if (it.player.name.length > 32) Call.kick(it.player.con(), "Nickname too long!")
                if (it.player.name.matches(Regex(".*\\[.*].*"))) Call.kick(
                    it.player.con(),
                    "Color tags can't be used for nicknames on this Server."
                )
                if (it.player.name.contains("　")) Call.kick(
                    it.player.con(),
                    "Don't use blank speical charactor nickname!"
                )
                if (it.player.name.contains(" ")) Call.kick(it.player.con(), "Nicknames can't be used on this server!")
                if (Pattern.matches(".*\\[.*.].*", it.player.name)) Call.kick(
                    it.player.con(),
                    "Can't use only color tags nickname in this Server."
                )
            }
        }

        // 플레이어가 아이템을 특정 블록에다 직접 가져다 놓았을 때 작동
        Events.on(DepositEvent::class.java) {
            Log.write(
                Log.LogType.Deposit,
                "log.deposit",
                it.player.name,
                it.player.unit().item().name,
                it.tile.block().name
            )
        }

        // 플레이어가 서버에 들어왔을 때 작동
        Events.on(PlayerJoin::class.java) { it ->
            val locale = Tool.getGeo(it.player)

            Log.write(Log.LogType.Player, "log.player.join", it.player.name, it.player.uuid(), it.player.con.address)
            PluginData.players.add(it.player)
            it.player.admin(false)

            val playerData = PlayerCore.load(it.player.uuid(), null)
            val sendMessage = sendMessage(it.player, Bundle(playerData))

            if (playerData != null) sendMessage["account.autologin"]

            if (Config.authType != Config.AuthType.None) {
                if (Config.authType == Config.AuthType.Discord) {
                    if (playerData != null) {
                        PlayerCore.playerLoad(it.player, null)
                    } else {
                        val message = if (Config.authType == Config.AuthType.Discord) {
                            Bundle(locale)["system.login.require.discord"]
                        } else {
                            Bundle(locale)["system.login.require.password"]
                        }
                        Call.infoMessage(it.player.con(), message)
                    }
                } else {
                    if (playerData != null) {
                        PlayerCore.playerLoad(it.player, null)
                    } else {
                        val language = Tool.getGeo(it.player)
                        Call.infoMessage(it.player.con, Bundle(language)["system.login.require.password"])
                    }
                }
            } else { // 로그인 기능이 꺼져있을 때, 바로 계정 등록을 하고 데이터를 로딩함
                if (playerData != null) {
                    PlayerCore.playerLoad(it.player, null)
                } else {
                    val register = PlayerCore.register(
                        it.player.name(),
                        it.player.uuid(),
                        locale.isO3Country,
                        "none",
                        "none",
                        "default"
                    )
                    if (register) {
                        if (!PlayerCore.playerLoad(it.player, null)) Call.kick(
                            it.player.con(),
                            Bundle()["plugin-error-kick"]
                        )
                        Call.kick(it.player.con(), Bundle()["plugin-error-kick"])
                    }
                }
            }

            // VPN을 사용중인지 확인
            if (Config.antiVPN) {
                val br = BufferedReader(InputStreamReader(javaClass.getResourceAsStream("/ipv4.txt")))
                br.use { _ ->
                    var line: String
                    while (br.readLine().also { line = it } != null) {
                        val match = IpAddressMatcher(line)
                        if (match.matches(it.player.con.address)) {
                            Call.kick(it.player.con(), Bundle()["anti-grief.vpn"])
                        }
                    }
                }
            }
        }

        // 플레이어가 서버에서 나갔을 때 작동
        Events.on(PlayerLeave::class.java) { it ->
            Log.write(Log.LogType.Player, "log.player.leave", it.player.name, it.player.uuid(), it.player.con.address)
            val player = PluginData[it.player.uuid()]
            if (player != null) {
                PlayerCore.save(player)
                PluginData.playerData.find { p: PlayerData -> p.uuid == it.player.uuid() }
                    ?.let { PluginData.removePlayerData(it) }
                PluginData.players.remove(it.player)
            }
        }

        // 플레이어가 채팅을 했을 때 작동
        Events.on(PlayerChatEvent::class.java) {
            if (it.message.length > Vars.maxTextLength) {
                Call.kick(it.player.con(), "Hacked client detected")
            }

            val playerData = PluginData[it.player.uuid()]
            val sendMessage = sendMessage(it.player, Bundle(playerData))

            if (!it.message.startsWith("/")) {
                Log.write(Log.LogType.Chat, "log.chat", it.player.name, it.message)
                Log.info("<&y" + it.player.name + ": &lm" + it.message + "&lg>")
            }

            if (playerData != null) {
                if (!it.message.startsWith("/")) {
                    if (it.message == "y" && PluginData.votingClass != null && PluginData.isVoting) {
                        if (PluginData.votingClass!!.voted.contains(it.player.uuid()) || PluginData.votingPlayer == it.player) {
                            sendMessage["vote.already-voted"]
                        } else {
                            PluginData.votingClass!!.voted.add(it.player.uuid())
                        }
                    } else {
                        if (!playerData.mute && playerData.crosschat) {
                            when (Config.networkMode) {
                                Config.NetworkMode.Client -> {
                                    if (Client.activated) Client.request(Client.Request.Chat, it.player, it.message)
                                }
                                Config.NetworkMode.Server -> {
                                    val msg = "[" + it.player.name + "]: " + it.message
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
                                    sendMessage["no-any-network"]
                                    playerData.crosschat = false
                                }
                            }
                        }
                    }
                    if (NetClient.colorizeName(it.player.id, it.player.name) != null) {
                        sendMessage(
                            Permissions.user[playerData.uuid].asObject()["chatFormat"].asString()
                                .replace("%1", NetClient.colorizeName(it.player.id, it.player.name))
                                .replace("%2", it.message)
                        )
                    }
                }
            } else {
                sendMessage(
                    "[gray][Unauthorized] ${
                        NetClient.colorizeName(
                            it.player.id,
                            it.player.name
                        )
                    }[white]: ${it.message}"
                )
            }
        }

        // 플레이어가 블럭 건설을 끝마쳤을 때 작동
        Events.on(BlockBuildEndEvent::class.java) {
            if (it.unit.isPlayer) {
                val player = it.unit.player
                val target = PluginData[player.uuid()]

                if (!player.unit().isNull && target != null && it.tile.block() != null && player.unit().buildPlan() != null) {
                    val name = it.tile.block().name
                    if (!it.breaking) {
                        Log.write(Log.LogType.Block, "log.block.place", player.name, it.tile.block().name)
                        val exp = PluginData.expData.getInt(name, 0)
                        target.placecount++
                        target.exp = target.exp + exp

                        if (Config.debug) Log.info(
                            "anti-grief.build.finish",
                            player.name,
                            it.tile.block().name,
                            it.tile.x,
                            it.tile.y
                        )
                    } else if (it.breaking) {
                        Log.write(
                            Log.LogType.Block,
                            "log.block.remove",
                            player.name,
                            player.unit().buildPlan().block.name
                        )
                        val exp = PluginData.expData.getInt(player.unit().buildPlan().block.name, 0)
                        target.breakcount++
                        target.exp = target.exp + exp

                        if (Config.debug) Log.info(
                            "anti-grief.destroy",
                            player.name,
                            player.unit().buildPlan().block.name,
                            it.tile.x,
                            it.tile.y
                        )
                    }
                }
            }
        }

        // 플레이어가 블럭을 선택했을 때 작동 (파괴 등)
        Events.on(BuildSelectEvent::class.java) {
            if (it.builder is Playerc && it.builder.buildPlan() != null && !Pattern.matches(
                    ".*build.*",
                    it.builder.buildPlan().block.name
                ) && it.tile.block() !== Blocks.air && it.breaking
            ) {
                Log.write(
                    Log.LogType.Block,
                    "log.block.remove",
                    (it.builder as Playerc).name(),
                    it.tile.block().name,
                    it.tile.x.toString(),
                    it.tile.y.toString()
                )
                val target = PluginData[(it.builder as Playerc).uuid()]

                // Exp Playing Game (EPG)
                if (target != null && Config.blockEXP) {
                    val level = target.level
                    val name = it.builder.buildPlan().block.name

                    try {
                        val obj = JsonValue.readHjson(Main.pluginRoot.child("Exp.hjson").reader()).asObject()
                        if (obj[name] != null) {
                            val req = obj.getInt(name, 999)
                            if (level < req) {
                                Call.deconstructFinish(it.tile, it.tile.block(), (it.builder as Playerc).unit())
                                sendMessage(
                                    (it.builder as Playerc),
                                    Bundle(PluginData[(it.builder as Playerc).uuid()])["system.epg.block-require", name, req.toString()]
                                )
                            }
                        } else {
                            Log.err("system.epg.block-not-valid", name)
                        }
                    } catch (ex: Exception) {
                        CrashReport(ex)
                    }
                }
            }
        }

        // 종류 상관없이 유닛이 파괴되었을 때 작동
        Events.on(UnitDestroyEvent::class.java) {

        }

        // 플레이어가 차단되었을 때 작동
        Events.on(PlayerBanEvent::class.java) {
            if (Config.banShare && Config.networkMode == Config.NetworkMode.Client) {
                Client.request(Client.Request.BanSync, Nulls.unit.player, null)
            }

            if (it.player != null) {
                Tool.sendMessageAll("player.banned", it.player.name)
                if (Vars.netServer.admins.isIDBanned(it.player.uuid())) {
                    it.player.con.kick(Packets.KickReason.banned)
                }
            }
        }

        // 플레이어가 IP 차단되었을 때 작동
        Events.on(PlayerIpBanEvent::class.java) {
            if (Config.banShare && Client.activated) {
                Client.request(Client.Request.BanSync, Nulls.unit.player, null)
            }
        }

        // 플레이어가 차단 해제되었을 때 작동
        Events.on(PlayerUnbanEvent::class.java) {
            if (Client.activated) Client.request(Client.Request.UnbanID, null, it.player.uuid() + "|<unknown>")
        }

        // 플레이어의 IP 차단이 해제되었을 때 작동
        Events.on(PlayerIpUnbanEvent::class.java) {
            if (Client.activated) Client.request(Client.Request.UnbanIP, null, "<unknown>|" + it.ip)
        }

        // 서버가 시작되었을 때 작동
        Events.on(ServerLoadEvent::class.java) {
            if (Config.update) {
                Log.client("client.update-check")
                try {
                    val web = Tool.getWebContent("https://api.github.com/repos/kieaer/Essentials/releases/latest")
                    if (web != null) {
                        val json = JsonValue.readJSON(web).asObject()
                        for (a in 0 until Vars.mods.list().size) {
                            if (Vars.mods.list()[a].meta.name == "Essentials") {
                                PluginData.pluginVersion = Vars.mods.list()[a].meta.version
                            }
                        }
                        val latest = DefaultArtifactVersion(json.getString("tag_name", PluginData.pluginVersion))
                        val current = DefaultArtifactVersion(PluginData.pluginVersion)
                        when {
                            latest > current -> {
                                Log.client("version-new")
                                val t = Thread {
                                    try {
                                        Log.info(Bundle()["update-description", json["tag_name"].toString()])
                                        println(json.getString("body", "No description found."))
                                        println(Bundle()["plugin-downloading-standby"])
                                        Main.timer.cancel()
                                        if (Config.networkMode == Config.NetworkMode.Server) {
                                            for (ser in Server.list) {
                                                ser!!.interrupt()
                                                ser.os.close()
                                                ser.br.close()
                                                ser.socket.close()
                                                Server.list.remove(ser)
                                            }
                                            Server.shutdown()
                                        }
                                        if (Config.networkMode == Config.NetworkMode.Client && Client.activated) {
                                            Client.request(Client.Request.Exit, null, null)
                                        }
                                        Main.mainThread.shutdown()
                                        DB.stop()
                                        println(Bundle()["plugin-downloading"])
                                        Tool.download(
                                            URL(
                                                json["assets"].asArray()[0].asObject()
                                                    .getString("browser_download_url", null)
                                            ), Core.settings.dataDirectory.child("mods/Essentials.jar").file()
                                        )
                                    } catch (ex: Exception) {
                                        println(Bundle()["plugin-downloading-fail"].trimIndent())
                                        CrashReport(ex)
                                    }
                                    Core.app.exit()
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
                        PluginData.pluginVersion = Vars.mods.list()[a].meta.version
                        break
                    }
                }
            }

            // Discord 봇 시작
            if (Config.authType == Config.AuthType.Discord) {
                Discord.start()
            }
        }

        Events.on(EventType.Trigger.update::class.java) {
            if(tick >= 60){
                PluginData.uptime = PluginData.uptime + 1

                // 임시로 밴당한 유저 감시
                for (a in 0 until PluginData.banned.size) {
                    val time = LocalDateTime.now()
                    if (time.isAfter(Tool.longToDateTime(PluginData.banned[a].time))) {
                        PluginData.banned.remove(a)
                        Vars.netServer.admins.unbanPlayerID(PluginData.banned[a]!!.uuid)
                        Log.info("[" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + PluginData.banned[a]!!.name + "/" + PluginData.banned[a]!!.uuid + " player unbanned!")
                        break
                    }
                }

                // 맵이 돌아가고 있을 때
                if (Vars.state.`is`(GameState.State.playing)) {
                    // 맵 플탐 카운트
                    PluginData.playtime = PluginData.playtime + 1

                    // 모든 클라이언트 서버에 대한 인원 총합 카운트
                    for (a in 0 until PluginData.warptotals.size) {
                        var result = 0
                        for (value in PluginData.warpcounts) result += value!!.players
                        val str = result.toString()
                        val digits = IntArray(str.length)
                        for (b in str.indices) digits[b] = str[b] - '0'
                        val tile = PluginData.warptotals[a].tile
                        if (PluginData.warptotals[a]!!.totalplayers != result && PluginData.warptotals[a]!!.numbersize != digits.size) {
                            for (px in 0..2) {
                                for (py in 0..4) {
                                    Call.deconstructFinish(Vars.world.tile(tile.x + 4 + px, tile.y + py), Blocks.air, Nulls.unit)
                                }
                            }
                        }
                        Tool.setTileText(tile, Blocks.copperWall, result.toString())
                        PluginData.warptotals[a] = PluginData.WarpTotal(Vars.state.map.name(), tile.pos(), result, digits.size)
                    }

                    // 플레이어 플탐 카운트 및 잠수확인
                    for (p in Groups.player) {
                        val target = PluginData[p.uuid()]
                        if (target != null) {
                            var kick = false

                            // Exp 계산
                            target.exp = target.exp++

                            // 잠수 및 플레이 시간 계산
                            target.playtime = target.playtime + 1
                            if (target.x == p.tileX() && target.y == p.tileY()) {
                                target.afk = target.afk + 1
                                if (Config.afktime != 0 && Config.afktime < target.afk) {
                                    kick = true
                                }
                            } else {
                                target.afk = 0
                            }
                            target.x = p.tileX()
                            target.y = p.tileY()
                            if (!Vars.state.rules.editor) Exp[target]
                            if (kick) Call.kick(p.con(), "AFK")
                        }
                    }
                }

                // 데이터 저장
                try {
                    PlayerCore.saveAll()
                    PluginData.saveAll()
                } catch (e: Exception) {
                    CrashReport(e)
                }

                tick = 0
            } else {
                tick++
            }

            if (Vars.state.`is`(GameState.State.playing)) {
                if (Config.border) {
                    for (p in Groups.player) {
                        if (p.x > Vars.world.width() * 8 || p.x < 0 || p.y > Vars.world.height() * 8 || p.y < 0) p.dead()
                    }
                }

                // 서버간 이동 영역에 플레이어가 있는지 확인
                for (value in PluginData.warpzones) {
                    if (!value!!.touch) {
                        for (ix in 0 until Groups.player.size()) {
                            val player = Groups.player.getByID(ix)
                            if (player.tileX() > value.startTile.x && player.tileX() < value.finishTile.x && player.tileY() > value.startTile.y && player.tileY() < value.finishTile.y) {
                                var resultIP = value.ip
                                var port = 6567
                                if (resultIP.contains(":") && Strings.canParsePositiveInt(
                                        resultIP.split(":").toTypedArray()[1]
                                    )
                                ) {
                                    val temp = resultIP.split(":").toTypedArray()
                                    resultIP = temp[0]
                                    port = temp[1].toInt()
                                }
                                Log.info("player.warped", player.name, "$resultIP:$port")
                                Call.connect(player.con, resultIP, port)
                            }
                        }
                    }
                }
            }
        }
    }
}