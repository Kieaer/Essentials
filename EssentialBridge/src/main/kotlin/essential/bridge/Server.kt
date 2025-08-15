package essential.bridge

import arc.util.Log
import arc.util.serialization.Json
import essential.reflection.EssentialLookup
import mindustry.Vars
import mindustry.net.Administration
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.time.LocalDateTime
import java.util.concurrent.Executors

class Server : Runnable {
    private var server: ServerSocket? = null
    var lastSentMessage: String = ""
    var clients = ArrayList<Socket>()
    val executor = Executors.newCachedThreadPool()

    override fun run() {
        try {
            server = ServerSocket(Main.Companion.conf.port)
            while (!Thread.currentThread().isInterrupted) {
                val socket = server!!.accept()
                Log.debug(
                    Main.Companion.bundle["network.server.connected", socket.getInetAddress().hostAddress]
                )
                clients.add(socket)

                executor.execute {
                    start(socket)
                }
            }
        } catch (_: SocketException) {
        } catch (e: Exception) {
            Log.err(e)
        }
    }

    fun shutdown() {
        try {
            server!!.close()
        } catch (e: IOException) {
            Log.err(e)
        }
    }

    fun sendAll(type: String, msg: String) {
        for (a in clients) {
            try {
                BufferedWriter(OutputStreamWriter(a.getOutputStream())).use { b ->
                    b.write(type)
                    b.newLine()
                    b.flush()
                    b.write(msg)
                    b.newLine()
                    b.flush()
                }
            } catch (e: SocketException) {
                Log.err(e)
                try {
                    a.close()
                } catch (ex: IOException) {
                    Log.err(ex)
                }
                clients.remove(a)
            } catch (e: IOException) {
                Log.err(e)
            }
        }
    }

    fun start(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            var isAlive = true

            while (isAlive) {
                val d = reader.readLine()
                if (d == null) {
                    println("reader data received null")
                    isAlive = false
                } else {
                    when (d) {
                        "isBanned" -> {
                            val info: Administration.PlayerInfo = Json().fromJson(Administration.PlayerInfo::class.java, reader.readLine())
                            var banned: Boolean = Vars.netServer.admins.isIDBanned(info.id)
                            for (ip in info.ips) {
                                if (Vars.netServer.admins.isIPBanned(ip)) {
                                    banned = true
                                    break
                                }
                            }
                            if (banned) {
                                info.banned = true
                                sendAll("banned", Json().toJson(info, Administration.PlayerInfo::class.java))
                            }
                        }

                        "exit" -> isAlive = false
                        "message" -> sendAll("message", reader.readLine())
                        "crash" -> {
                            val stacktrace = StringBuilder()
                            var line: String?
                            while ((reader.readLine().also { line = it }) != null && line != "null") {
                                stacktrace.append(line).append("\n")
                            }
                            (EssentialLookup.getRootPath() ?: arc.Core.settings.dataDirectory.child("mods/Essentials/")).child("report/" + LocalDateTime.now().withNano(0) + ".txt")
                                .writeString(stacktrace.toString())
                            Log.info("Crash log received from " + socket.getInetAddress().hostAddress)
                        }
                    }
                }
            }
        } catch (e: SocketException) {
            Log.err(e)
        } catch (e: Exception) {
            Log.err(e)
        }
        clients.remove(socket)
        Log.info(Main.Companion.bundle["network.server.disconnected", socket.getInetAddress().hostAddress])
    }
}