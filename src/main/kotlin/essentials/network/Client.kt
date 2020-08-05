package essentials.network

import essentials.Main
import essentials.Main.Companion.mainThread
import essentials.internal.CrashReport
import essentials.internal.Log
import mindustry.Vars
import mindustry.entities.type.Player
import mindustry.gen.Call
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.JsonValue
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.net.*
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.TimeUnit
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class Client : Runnable {
    lateinit var socket: Socket
    lateinit var br: BufferedReader
    lateinit var os: DataOutputStream
    lateinit var skey: SecretKey
    var activated = false
    private var disconnected = false

    fun shutdown() {
        try {
            Thread.currentThread().interrupt()
            os.close()
            br.close()
            socket.close()
            activated = false
        } catch (ignored: IOException) {
        }
    }

    fun wakeup() {
        try {
            val address = InetAddress.getByName(Main.configs.clientHost)
            socket = Socket(address, Main.configs.clientPort)
            socket.soTimeout = if (disconnected) 2000 else 10000

            // 키 생성
            br = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            os = DataOutputStream(socket.getOutputStream())

            // 키값 보내기
            val gen = KeyGenerator.getInstance("AES")
            gen.init(128)
            val key = gen.generateKey()
            val raw = key.encoded
            skey = SecretKeySpec(raw, "AES")
            os.writeBytes(String(Base64.getEncoder().encode(raw))+"\n")
            os.flush()

            // 데이터 전송
            val json = JsonObject()
            json.add("type", "Ping")
            val encrypted = Main.tool.encrypt(json.toString(), skey)
            os.writeBytes(encrypted+"\n")
            os.flush()
            val receive = Main.tool.decrypt(br.readLine(), skey)
            if (JsonValue.readJSON(receive).asObject()["result"] != null) {
                activated = true
                mainThread.execute(Thread(this))
                Log.client(JsonValue.readJSON(receive).asObject()["result"].asString())
                Log.client(if (disconnected) "client.reconnected" else "client.enabled", socket.inetAddress.toString().replace("/", ""))
                disconnected = false
            } else {
                throw SocketException("Invalid request!")
            }
        } catch (e: UnknownHostException) {
            Log.client("Invalid host!")
        } catch (e: SocketTimeoutException) {
            if (disconnected) {
                try {
                    socket.close()
                    TimeUnit.SECONDS.sleep(5)
                    wakeup()
                } catch (ex: InterruptedException) {
                    Thread.currentThread().interrupt()
                } catch (ex: IOException) {
                    Thread.currentThread().interrupt()
                }
            } else {
                try {
                    socket.close()
                    Log.client("remote-server-dead")
                } catch (ignored: IOException) {
                }
            }
        } catch (e: SocketException) {
            if (disconnected) {
                try {
                    socket.close()
                    TimeUnit.SECONDS.sleep(5)
                    wakeup()
                } catch (ex: InterruptedException) {
                    Thread.currentThread().interrupt()
                } catch (ex: IOException) {
                    Thread.currentThread().interrupt()
                }
            } else {
                try {
                    socket.close()
                    Log.client("remote-server-dead")
                } catch (ignored: IOException) {
                }
            }
        } catch (e: Exception) {
            CrashReport(e)
        }
    }

    fun request(request: Request?, player: Player?, message: String?) {
        val data = JsonObject()
        try {
            when (request) {
                Request.BanSync -> {
                    val ban = JsonArray()
                    val ipban = JsonArray()
                    val subban = JsonArray()
                    for (info in Vars.netServer.admins.banned) {
                        ban.add(info.id)
                        for (ipbans in info.ips) {
                            ipban.add(ipbans)
                        }
                    }
                    for (ipbans in Vars.netServer.admins.bannedIPs) {
                        ipban.add(ipbans)
                    }
                    for (subbans in Vars.netServer.admins.subnetBans) {
                        subban.add(subbans)
                    }
                    data.add("type", "BanSync")
                    data.add("ban", ban)
                    data.add("ipban", ipban)
                    data.add("subban", subban)
                    os.writeBytes(Main.tool.encrypt(data.toString(), skey)+"\n")
                    os.flush()
                    Log.client("client.banlist.sented")
                }
                Request.Chat -> {
                    data.add("type", "Chat")
                    data.add("name", player!!.name)
                    data.add("message", message)
                    os.writeBytes(Main.tool.encrypt(data.toString(), skey)+"\n")
                    os.flush()
                    Call.sendMessage("[#357EC7][SC] [orange]" + player.name + "[orange]: [white]" + message)
                    Log.client("client.message", Main.configs.clientHost, message)
                }
                Request.Exit -> {
                    data.add("type", "Exit")
                    os.writeBytes(Main.tool.encrypt(data.toString(), skey)+"\n")
                    os.flush()
                    shutdown()
                    return
                }
                Request.UnbanIP -> {
                    data.add("type", "UnbanIP")
                    val isIP: Boolean = try {
                        InetAddress.getByName(message).hostAddress == message
                    } catch (ex: UnknownHostException) {
                        false
                    }
                    if (isIP) data.add("ip", message)
                    os.writeBytes(Main.tool.encrypt(data.toString(), skey)+"\n")
                    os.flush()
                }
                Request.UnbanID -> {
                    data.add("type", "UnbanID")
                    data.add("uuid", message)
                    os.writeBytes(Main.tool.encrypt(data.toString(), skey)+"\n")
                    os.flush()
                }
                Request.DataShare -> {
                    data.add("type", "DataShare")
                    data.add("data", "")
                    os.writeBytes(Main.tool.encrypt("datashare", skey)+"\n")
                    os.flush()
                }
            }
        } catch (e: Exception) {
            CrashReport(e)
        }
    }

    override fun run() {
        disconnected = false
        try {
            socket.soTimeout = 0
        } catch (e: SocketException) {
            CrashReport(e)
        }
        while (!Thread.currentThread().isInterrupted) {
            try {
                var data: JsonObject
                try {
                    data = JsonValue.readJSON(Main.tool.decrypt(br.readLine(), skey)).asObject()
                } catch (e: IllegalArgumentException) {
                    disconnected = true
                    Log.client("server.disconnected", Main.configs.clientHost)
                    if (!Thread.currentThread().isInterrupted) wakeup()
                    return
                } catch (e: SocketException) {
                    disconnected = true
                    Log.client("server.disconnected", Main.configs.clientHost)
                    if (!Thread.currentThread().isInterrupted) wakeup()
                    return
                } catch (e: Exception) {
                    if (e.message != "Socket closed") CrashReport(e)
                    Log.client("server.disconnected", Main.configs.clientHost)
                    shutdown()
                    return
                }
                when (Request.valueOf(data["type"].asString())) {
                    Request.BanSync -> {
                        Log.client("client.banlist.received")

                        // 적용
                        val ban = data["ban"].asArray()
                        val ipban = data["ipban"].asArray()
                        val subban = data["subban"].asArray()
                        for (b in ban) {
                            Vars.netServer.admins.banPlayerID(b.asString())
                        }
                        for (b in ipban) {
                            Vars.netServer.admins.banPlayerIP(b.asString())
                        }
                        for (b in subban) {
                            Vars.netServer.admins.addSubnetBan(b.asString())
                        }
                        Log.client("success")
                    }
                    Request.Chat -> {
                        val name = data["name"].asString()
                        val message = data["message"].asString()
                        Call.sendMessage("[#C77E36][RC] [orange]$name [orange]:[white] $message")
                    }
                    Request.Exit -> {
                        shutdown()
                        return
                    }
                    Request.UnbanIP -> Vars.netServer.admins.unbanPlayerIP(data["ip"].asString())
                    Request.UnbanID -> Vars.netServer.admins.unbanPlayerID(data["uuid"].asString())
                    Request.DataShare -> {
                    }
                }
            } catch (e: Exception) {
                Log.client("server.disconnected", Main.configs.clientHost)
                shutdown()
                return
            }
        }
    }

    enum class Request {
        BanSync, Chat, Exit, UnbanIP, UnbanID, DataShare
    }
}