package essential.core.service.bridge

import arc.util.Log
import arc.util.serialization.Json
import essential.common.rootPath
import essential.core.service.bridge.BridgeService.Companion.bundle
import essential.core.service.bridge.BridgeService.Companion.conf
import mindustry.Vars
import mindustry.net.Administration
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.time.LocalDateTime
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Server(private var server: ServerSocket? = null) : Runnable {
    companion object {
        private const val MAX_CLIENTS = 32

        fun bind(port: Int): Server? = try {
            Server(ServerSocket(port))
        } catch (_: IOException) {
            null
        }
    }

    var lastSentMessage: String = ""
    val clients = CopyOnWriteArrayList<Socket>()
    private val executor = ThreadPoolExecutor(
        2,
        8,
        60,
        TimeUnit.SECONDS,
        ArrayBlockingQueue(32),
        ThreadPoolExecutor.AbortPolicy()
    )

    override fun run() {
        try {
            val activeServer = server ?: ServerSocket(conf.port).also { server = it }
            while (!Thread.currentThread().isInterrupted) {
                val socket = activeServer.accept()
                if (clients.size >= MAX_CLIENTS) {
                    socket.close()
                    continue
                }
                try {
                    executor.execute { start(socket) }
                } catch (_: RejectedExecutionException) {
                    socket.close()
                }
            }
        } catch (_: SocketException) {
        } catch (e: Exception) {
            Log.err(e)
        }
    }

    fun shutdown() {
        executor.shutdownNow()
        try {
            server?.close()
        } catch (e: IOException) {
            Log.err(e)
        }
    }

    fun sendAll(type: String, msg: String) {
        val encodedMessage = encodeBridgePayload(msg)
        for (socket in clients) {
            try {
                synchronized(socket) {
                    val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                    writer.write(type)
                    writer.newLine()
                    writer.write(encodedMessage)
                    writer.newLine()
                    writer.flush()
                }
            } catch (_: IOException) {
                clients.remove(socket)
                try {
                    socket.close()
                } catch (_: IOException) {
                }
            }
        }
    }

    private fun authenticate(socket: Socket, reader: BufferedReader, writer: BufferedWriter): Boolean {
        socket.soTimeout = 10_000
        val challenge = createBridgeChallenge()
        writer.write("auth-challenge")
        writer.newLine()
        writer.write(challenge)
        writer.newLine()
        writer.flush()

        val command = readBridgeLine(reader)
        val response = readBridgeLine(reader)
        val authenticated = command == "auth-response" && response != null &&
            isValidBridgeAuthentication(conf.sharedSecret, challenge, response)
        socket.soTimeout = 0
        return authenticated
    }

    fun start(socket: Socket) {
        try {
            socket.use {
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                if (!authenticate(socket, reader, writer)) {
                    Log.warn("Rejected unauthenticated bridge connection from @", socket.inetAddress.hostAddress)
                    return
                }

                clients.add(socket)
                Log.debug(bundle["network.server.connected", socket.inetAddress.hostAddress])
                while (!Thread.currentThread().isInterrupted) {
                    when (val command = readBridgeLine(reader) ?: break) {
                        "isBanned" -> handleBanCheck(readBridgeLine(reader))
                        "exit" -> break
                        "message" -> readBridgeLine(reader)?.let(::decodeBridgePayload)?.let { sendAll("message", it) }
                        "crash" -> readBridgeLine(reader)?.let(::decodeBridgePayload)?.let(::writeCrashReport)
                        else -> {
                            Log.warn("Rejected unknown bridge command from @: @", socket.inetAddress.hostAddress, command)
                            break
                        }
                    }
                }
            }
        } catch (_: SocketException) {
        } catch (e: Exception) {
            Log.err(e)
        } finally {
            clients.remove(socket)
            Log.info(bundle["network.server.disconnected", socket.inetAddress.hostAddress])
        }
    }

    private fun handleBanCheck(encodedInfo: String?) {
        val info = encodedInfo?.let(::decodeBridgePayload)?.let {
            runCatching { Json().fromJson(Administration.PlayerInfo::class.java, it) }.getOrNull()
        } ?: return
        var banned = Vars.netServer.admins.isIDBanned(info.id)
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

    private fun writeCrashReport(stacktrace: String) {
        if (stacktrace.toByteArray().size > MAX_BRIDGE_CRASH_REPORT_LENGTH) return
        rootPath.child("report/${LocalDateTime.now().withNano(0)}.txt").writeString(stacktrace)
    }
}
