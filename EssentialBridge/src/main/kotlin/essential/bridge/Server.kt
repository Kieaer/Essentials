package essential.bridge

import arc.util.Log
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.time.LocalDateTime

class Server : Runnable {
    private var server: ServerSocket? = null
    var lastSentMessage: String = ""
    var clients: MutableList<Socket> = ArrayList<Socket>()

    override fun run() {
        try {
            server = ServerSocket(Main.Companion.conf.port)
            while (!Thread.currentThread().isInterrupted()) {
                val socket = server!!.accept()
                Log.debug(
                    Main.Companion.bundle.get(
                        "network.server.connected",
                        socket.getInetAddress().getHostAddress()
                    )
                )
                clients.add(socket)
                val handler: Handler = Server.Handler(socket)
                handler.start()
            }
        } catch (ignored: SocketException) {
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shutdown() {
        Thread.currentThread().interrupt()
        try {
            server!!.close()
        } catch (e: IOException) {
            e.printStackTrace()
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
                e.printStackTrace()
                try {
                    a.close()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                }
                clients.remove(a)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private inner class Handler(private val socket: Socket) : Thread() {
        override fun run() {
            try {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                while (!currentThread().isInterrupted()) {
                    val d = reader.readLine()
                    if (d == null) {
                        println("reader data received null")
                        interrupt()
                    } else {
                        when (d) {
                            "isBanned" -> {
                                val info: PlayerInfo = Json().fromJson(PlayerInfo::class.java, reader.readLine())
                                val banned: Boolean = Vars.netServer.admins.isIDBanned(info.id)
                                for (ip in info.ips) {
                                    if (Vars.netServer.admins.isIPBanned(ip)) {
                                        banned = true
                                        break
                                    }
                                }
                                if (banned) {
                                    info.banned = true
                                    sendAll("banned", Json().toJson(info, PlayerInfo::class.java))
                                }
                            }

                            "exit" -> interrupt()
                            "message" -> sendAll("message", reader.readLine())
                            "crash" -> {
                                val stacktrace = StringBuilder()
                                val line: String?
                                while ((reader.readLine().also { line = it }) != null && line != "null") {
                                    stacktrace.append(line).append("\n")
                                }
                                root.child("report/" + LocalDateTime.now().withNano(0) + ".txt")
                                    .writeString(stacktrace.toString())
                                Log.info("Crash log received from " + socket.getInetAddress().getHostAddress())
                            }
                        }
                    }
                }
            } catch (e: SocketException) {
                e.printStackTrace()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            clients.remove(socket)
            Log.info(Main.Companion.bundle.get("network.server.disconnected", socket.getInetAddress().getHostAddress()))
        }
    }
}