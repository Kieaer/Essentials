package essentials.event

import arc.Core
import essentials.Main
import essentials.PlayerData
import essentials.PluginData
import essentials.data.Config
import essentials.data.DB
import essentials.data.PlayerCore
import essentials.eof.connect
import essentials.eof.infoMessage
import essentials.eof.kick
import essentials.eof.sendMessage
import essentials.event.EventThread.EventTypes.*
import essentials.event.feature.Discord
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
import mindustry.core.NetClient
import mindustry.game.EventType
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

class EventThread(private val type: EventTypes, private val event: Any) {
    fun run() {
        try {
            when(type) {
                EventTypes.Config -> {
                    val e = event as EventType.ConfigEvent

                    if(e.tile != null && e.tile.block() != null && e.player != null) { // Source by BasedUser(router)
                        val entity = e.tile
                        val other = Vars.world.tile(e.value as Int)
                        val valid = other != null && other.block().hasPower
                        if(valid) {
                            val oldGraph = entity.power.graph
                            val newGraph = other.build.power.graph
                            val oldGraphCount = oldGraph.toString().substring(oldGraph.toString().indexOf("all=["), oldGraph.toString().indexOf("], l")).replaceFirst("all=\\[".toRegex(), "").split(",").toTypedArray().size
                            val newGraphCount = newGraph.toString().substring(newGraph.toString().indexOf("all=["), newGraph.toString().indexOf("], l")).replaceFirst("all=\\[".toRegex(), "").split(",").toTypedArray().size
                            if(abs(oldGraphCount - newGraphCount) > 10) {
                                sendMessage("${e.player.name} [white]player has [scarlet]unlinked[] the [yellow]power node[]. Number of connected buildings: [green] ${oldGraphCount.coerceAtLeast(newGraphCount)} [cyan]->[scarlet] ${oldGraphCount.coerceAtMost(newGraphCount)} [white](" + e.tile.x + ", " + e.tile.y + ")")
                            }
                        }
                    }
                }
                Tap -> {
                    val e = event as EventType.TapEvent

                    if(Config.logging) Log.write(Log.LogType.Tap, "log.tap", e.player.name, e.tile.block().name)
                    val playerData = PluginData[e.player.uuid()]
                    if(playerData != null) {
                        for(data in PluginData.warpblocks) {
                            if(e.tile.x >= Vars.world.tile(data.pos).x && e.tile.x <= Vars.world.tile(data.pos).x) {
                                if(e.tile.y >= Vars.world.tile(data.pos).y && e.tile.y <= Vars.world.tile(data.pos).y) {
                                    if(data.online) {
                                        Log.info("player.warped", e.player.name, data.ip + ":" + data.port)
                                        connect(e.player, data.ip, data.port)
                                    }
                                    break
                                }
                            }
                        }

                        for(data in PluginData.warpzones) {
                            if(e.tile.x > data.startTile.x && e.tile.x < data.finishTile.x) {
                                if(e.tile.y > data.startTile.y && e.tile.y < data.finishTile.y) {
                                    Log.info("player.warped", e.player.name, data.ip + ":" + data.port)
                                    connect(e.player, data.ip, data.port)
                                    break
                                }
                            }
                        }
                    }
                }
                Withdraw -> {
                    val e = event as EventType.WithdrawEvent

                    if(e.tile != null && e.player.unit().item() != null && e.player.name != null) {
                        if(Config.logging) Log.write(Log.LogType.WithDraw, "log.withdraw", e.player.name, e.player.unit().item().name, e.amount.toString(), e.tile.block().name)
                        if(Vars.state.rules.pvp) {
                            if(e.item.flammability > 0.001f) {
                                e.player.sendMessage(Bundle(PluginData[e.player.uuid()])["system.flammable.disabled"])
                                e.player.unit().clearItem()
                            }
                        }
                    }
                }
                Gameover -> {
                    val e = event as EventType.GameOverEvent

                    if(Vars.state.rules.pvp) {
                        var index = 5
                        for(a in 0..4) {
                            if(Vars.state.teams[Team.all[index]].cores.isEmpty) {
                                index--
                            }
                        }
                        if(index == 1) {
                            for(player in Groups.player) {
                                val target = PluginData[player.uuid()]
                                if(target != null) {
                                    if(player.team().name == e.winner.name) {
                                        target.pvpwincount++
                                    } else if(player.team().name != e.winner.name) {
                                        target.pvplosecount++
                                    }
                                }
                            }
                        }
                    } else if(Vars.state.rules.attackMode) {
                        for(p in Groups.player) {
                            val target = PluginData[p.uuid()]
                            if(target != null) target.attackclear++
                        }
                    }
                }
                WorldLoad -> {
                    PluginData.playtime = 0L
                }
                PlayerConnect -> {
                    val e = event as EventType.PlayerConnect

                    if(Config.logging) Log.write(Log.LogType.Player, "log.player.connect", e.player.name, e.player.uuid(), e.player.con.address)

                    // 닉네임이 블랙리스트에 등록되어 있는지 확인
                    for(s in PluginData.blacklist) {
                        if(e.player.name.matches(Regex(s))) {
                            try {
                                val locale = Tool.getGeo(e.player)
                                Call.kick(e.player.con, Bundle(locale)["system.nickname.blacklisted.kick"])
                                Log.info("system.nickname.blacklisted", e.player.name)
                            } catch(ex: Exception) {
                                CrashReport(ex)
                            }
                        }
                    }
                    if(Config.nameFixed) {
                        if(e.player.name.length > 32) kick(e.player, "Nickname too long!") //if (e.player.name.matches(Regex(".*\\[.*].*"))) kick(e.player, "Color tags can't be used for nicknames on this Server.");
                        if(e.player.name.contains("　")) kick(e.player, "Don't use blank speical charactor nickname!")
                        if(e.player.name.contains(" ")) kick(e.player, "Nicknames can't be used on this server!")
                        if(Pattern.matches(".*\\[.*.].*", e.player.name)) kick(e.player, "Can't use only color tags nickname in this Server.")
                    }
                }
                Deposit -> {
                    val e = event as EventType.DepositEvent

                    if(Config.logging) Log.write(Log.LogType.Deposit, "log.deposit", e.player.name, e.player.unit().item().name, e.tile.block().name)
                }
                PlayerJoin -> {
                    val e = event as EventType.PlayerJoin

                    if(Config.logging) Log.write(Log.LogType.Player, "log.player.join", e.player.name, e.player.uuid(), e.player.con.address)
                    PluginData.players.add(e.player)
                    e.player.admin(false)

                    val playerData = PlayerCore.load(e.player.uuid(), null)
                    val sendMessage = sendMessage(e.player, Bundle(playerData))

                    if(Config.authType != Config.AuthType.None) {
                        if(Config.authType == Config.AuthType.Discord) {
                            if(playerData != null) {
                                sendMessage["account.autologin"]
                                PlayerCore.playerLoad(e.player, null)
                            } else {
                                val message: String?
                                val language = Tool.getGeo(e.player)
                                message = if(Config.authType == Config.AuthType.Discord) {
                                    Bundle(language)["system.login.require.discord"]
                                } else {
                                    Bundle(language)["system.login.require.password"]
                                }
                                infoMessage(e.player, message)
                            }
                        } else {
                            if(playerData != null) {
                                sendMessage["account.autologin"]
                                PlayerCore.playerLoad(e.player, null)
                            } else {
                                val language = Tool.getGeo(e.player)
                                Call.infoMessage(e.player.con, Bundle(language)["system.login.require.password"])
                            }
                        }
                    } else { // 로그인 기능이 꺼져있을 때, 바로 계정 등록을 하고 데이터를 로딩함
                        if(playerData != null) {
                            sendMessage["account.autologin"]
                            PlayerCore.playerLoad(e.player, null)
                        } else {
                            val register = PlayerCore.register(e.player, e.player.name(), e.player.uuid(), "none", "none")
                            if(register) {
                                if(!PlayerCore.playerLoad(e.player, null)) kick(e.player, Bundle()["plugin-error-kick"])
                            } else {
                                kick(e.player, Bundle()["plugin-error-kick"])
                            }
                        }
                    }

                    // VPN을 사용중인지 확인
                    if(Config.antiVPN) {
                        val br = BufferedReader(InputStreamReader(javaClass.getResourceAsStream("/ipv4.txt")))
                        br.use {
                            var line: String
                            while(br.readLine().also { line = it } != null) {
                                val match = IpAddressMatcher(line)
                                if(match.matches(e.player.con.address)) {
                                    kick(e.player, Bundle()["anti-grief.vpn"])
                                }
                            }
                        }
                    }
                }
                PlayerLeave -> {
                    val e = event as EventType.PlayerLeave

                    if(Config.logging) Log.write(Log.LogType.Player, "log.player.leave", e.player.name, e.player.uuid(), e.player.con.address)
                    val player = PluginData[e.player.uuid()]
                    if(player != null) {
                        PlayerCore.save(player)
                        PluginData.playerData.find { p: PlayerData -> p.uuid == e.player.uuid() }?.let { PluginData.removePlayerData(it) }
                        PluginData.players.remove(e.player)
                    }
                }
                PlayerChat -> {
                    val e = event as EventType.PlayerChatEvent

                    if(e.message.length > Vars.maxTextLength) {
                        kick(e.player, "Hacked client detected")
                    }

                    val playerData = PluginData[e.player.uuid()]
                    val sendMessage = sendMessage(e.player, Bundle(playerData))

                    if(!e.message.startsWith("/")) Log.info("<&y" + e.player.name + ": &lm" + e.message + "&lg>")
                    if(playerData != null) {
                        if(!e.message.startsWith("/")) {
                            if(e.message == "y" && PluginData.votingClass != null && PluginData.isVoting) {
                                if(PluginData.votingClass!!.voted.contains(e.player.uuid()) || PluginData.votingPlayer == e.player) {
                                    sendMessage["vote.already-voted"]
                                } else {
                                    PluginData.votingClass!!.voted.add(e.player.uuid())
                                }
                            } else {
                                if(!playerData.mute) {
                                    if(playerData.crosschat) {
                                        when {
                                            Config.networkMode == Config.NetworkMode.Client -> {
                                                if(Client.activated) Client.request(Client.Request.Chat, e.player, e.message)
                                            }
                                            Config.networkMode == Config.NetworkMode.Server -> {
                                                val msg = "[" + e.player.name + "]: " + e.message
                                                try {
                                                    for(ser in Server.list) {
                                                        ser!!.os.writeBytes(Tool.encrypt(msg, ser.spec))
                                                        ser.os.flush()
                                                    }
                                                } catch(ex: Exception) {
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
                            }
                            if(NetClient.colorizeName(e.player.id, e.player.name) != null) {
                                sendMessage(Permissions.user[playerData.uuid].asObject()["prefix"].asString().replace("%1", NetClient.colorizeName(e.player.id, e.player.name)).replace("%2", e.message))
                            }
                        }
                    } else {
                        sendMessage("[gray][Unauthorized] ${NetClient.colorizeName(e.player.id, e.player.name)}[white]: ${e.message}")
                    }
                }
                BlockBuildEnd -> {
                    val e = event as EventType.BlockBuildEndEvent

                    if(!e.unit.isNull && e.unit.isPlayer) {
                        val player = e.unit.player

                        Log.write(Log.LogType.Block, "log.block.place", player.name, e.tile.block().name)
                        val target = PluginData[player.uuid()]

                        if(!e.breaking && !player.unit().isNull && target != null && e.tile.block() != null && player.unit().buildPlan() != null && player.unit().buildPlan().block != null) {
                            val name = e.tile.block().name
                            try {
                                val obj = JsonValue.readHjson(Main.pluginRoot.child("Exp.hjson").reader()).asObject()
                                val exp = obj.getInt(name, 0)
                                target.placecount++
                                target.exp = target.exp + exp
                            } catch(ex: Exception) {
                                CrashReport(ex)
                            }


                            if(Config.debug) {
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
                BuildSelect -> {
                    val e = event as EventType.BuildSelectEvent

                    if(e.builder is Playerc && e.builder.buildPlan() != null && !Pattern.matches(".*build.*", e.builder.buildPlan().block.name) && e.tile.block() !== Blocks.air) {
                        if(e.breaking) {
                            Log.write(Log.LogType.Block, "log.block.remove", (e.builder as Playerc).name(), e.tile.block().name, e.tile.x.toString(), e.tile.y.toString())
                            val target = PluginData[(e.builder as Playerc).uuid()]
                            if(target != null) {
                                val name = e.tile.block().name
                                try {
                                    val obj = JsonValue.readHjson(Main.pluginRoot.child("Exp.hjson").reader()).asObject()
                                    val exp = obj.getInt(name, 0)
                                    target.breakcount++
                                    target.exp = target.exp + exp
                                } catch(ex: Exception) {
                                    CrashReport(ex)
                                    kick((e.builder as Playerc), Bundle(target)["not-logged"])
                                }

                                // Exp Playing Game (EPG)
                                if(Config.blockEXP) {
                                    val level = target.level
                                    try {
                                        val obj = JsonValue.readHjson(Main.pluginRoot.child("Exp.hjson").reader()).asObject()
                                        if(obj[name] != null) {
                                            val req = obj.getInt(name, 999)
                                            if(level < req) {
                                                Call.deconstructFinish(e.tile, e.tile.block(), (e.builder as Playerc).unit())
                                                sendMessage((e.builder as Playerc), Bundle(PluginData[(e.builder as Playerc).uuid()])["system.epg.block-require", name, req.toString()])
                                            }
                                        } else {
                                            Log.err("system.epg.block-not-valid", name)
                                        }
                                    } catch(ex: Exception) {
                                        CrashReport(ex)
                                    }
                                }
                            }
                        }
                        if(Config.debug) {
                            Log.info("anti-grief.destroy", (e.builder as Playerc).name(), e.tile.block().name, e.tile.x, e.tile.y)
                        }
                    }
                }
                UnitDestroy -> {}
                PlayerBan -> {
                    val e = event as EventType.PlayerBanEvent

                    if(Config.banShare && Config.networkMode == Config.NetworkMode.Client) {
                        Client.request(Client.Request.BanSync, Nulls.unit.player, null)
                    }

                    if(e.player != null) {
                        Tool.sendMessageAll("player.banned", e.player.name)
                        if(Vars.netServer.admins.isIDBanned(e.player.uuid())) {
                            e.player.con.kick(Packets.KickReason.banned)
                        }
                    }
                }
                PlayerIpBan -> {
                    if(Config.banShare && Client.activated) {
                        Client.request(Client.Request.BanSync, Nulls.unit.player, null)
                    }
                }
                PlayerUnban -> {
                    val e = event as EventType.PlayerUnbanEvent

                    if(Client.activated) Client.request(Client.Request.UnbanID, null, e.player.uuid() + "|<unknown>")
                }
                PlayerIpUnban -> {
                    val e = event as EventType.PlayerIpUnbanEvent

                    if(Client.activated) Client.request(Client.Request.UnbanIP, null, "<unknown>|" + e.ip)
                }
                ServerLoaded -> { // 업데이트 확인
                    if(Config.update) {
                        Log.client("client.update-check")
                        try {
                            val web = Tool.getWebContent("https://api.github.com/repos/kieaer/Essentials/releases/latest")
                            if(web != null) {
                                val json = JsonValue.readJSON(web).asObject()
                                for(a in 0 until Vars.mods.list().size) {
                                    if(Vars.mods.list()[a].meta.name == "Essentials") {
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
                                                if(Config.networkMode == Config.NetworkMode.Server) {
                                                    try {
                                                        for(ser in Server.list) {
                                                            ser!!.interrupt()
                                                            ser.os.close()
                                                            ser.br.close()
                                                            ser.socket.close()
                                                            Server.list.remove(ser)
                                                        }
                                                        Server.shutdown()
                                                    } catch(ignored: Exception) {
                                                    }
                                                }
                                                if(Config.networkMode == Config.NetworkMode.Client && Client.activated) {
                                                    Client.request(Client.Request.Exit, null, null)
                                                }
                                                Main.mainThread.shutdown()
                                                DB.stop()
                                                println(Bundle()["plugin-downloading"])
                                                Tool.download(URL(json["assets"].asArray()[0].asObject().getString("browser_download_url", null)), Core.settings.dataDirectory.child("mods/Essentials.jar").file())
                                            } catch(ex: Exception) {
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
                        } catch(ex: Exception) {
                            CrashReport(ex)
                        }
                    } else {
                        for(a in 0 until Vars.mods.list().size) {
                            if(Vars.mods.list()[a].meta.name == "Essentials") {
                                PluginData.pluginVersion = Vars.mods.list()[a].meta.version
                                break
                            }
                        }
                    }

                    // Discord 봇 시작
                    if(Config.authType == Config.AuthType.Discord) {
                        Discord.start()
                    }
                }
            }
        } catch(e: Exception) {
            e.printStackTrace()
            CrashReport(e)
        }
    }

    enum class EventTypes {
        Config, Tap, Withdraw, Gameover, WorldLoad, PlayerConnect, Deposit, PlayerJoin, PlayerLeave, PlayerChat, BlockBuildEnd, BuildSelect, UnitDestroy, PlayerBan, PlayerIpBan, PlayerUnban, PlayerIpUnban, ServerLoaded;
    }
}