package essential.core.service.bridge

import arc.util.Log
import arc.util.Timer
import essential.core.service.bridge.BridgeService.Companion.conf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mindustry.gen.Call
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class Client : Runnable {
    private val messageQueue = ArrayBlockingQueue<String>(128)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isConnected = AtomicBoolean(false)
    private val isReconnecting = AtomicBoolean(false)

    @Volatile private var socket: Socket? = null
    @Volatile private var writer: BufferedWriter? = null
    var lastReceivedMessage: String? = ""

    private val maxReconnectAttempts = 5
    private var reconnectAttempts = 0
    private val reconnectDelay = 5f

    override fun run() {
        connect()
    }

    private fun connect() {
        scope.launch {
            try {
                val connectedSocket = withContext(Dispatchers.IO) {
                    Socket().apply {
                        connect(InetSocketAddress(conf.address, conf.port), 5_000)
                        soTimeout = 10_000
                    }
                }
                val connectedReader = BufferedReader(InputStreamReader(connectedSocket.getInputStream()))
                val connectedWriter = BufferedWriter(OutputStreamWriter(connectedSocket.getOutputStream()))
                if (!authenticate(connectedReader, connectedWriter)) {
                    connectedSocket.close()
                    throw SecurityException("Bridge authentication failed")
                }

                connectedSocket.soTimeout = 0
                socket = connectedSocket
                writer = connectedWriter
                isConnected.set(true)
                reconnectAttempts = 0
                Log.info(BridgeService.bundle["network.client.connected", conf.address])
                startReading(connectedReader)
                startWriting()
            } catch (e: Exception) {
                handleConnectionFailure(e)
            }
        }
    }

    private fun authenticate(reader: BufferedReader, writer: BufferedWriter): Boolean {
        val command = readBridgeLine(reader)
        val challenge = readBridgeLine(reader)
        if (command != "auth-challenge" || challenge == null) return false
        writer.write("auth-response")
        writer.newLine()
        writer.write(bridgeAuthenticationResponse(conf.sharedSecret, challenge))
        writer.newLine()
        writer.flush()
        return true
    }

    private fun handleConnectionFailure(e: Exception) {
        closeConnection()
        if (reconnectAttempts < maxReconnectAttempts && isReconnecting.compareAndSet(false, true)) {
            reconnectAttempts++
            Log.warn("Connection failed (attempt $reconnectAttempts/$maxReconnectAttempts): ${e.message}")
            Timer.schedule(object : Timer.Task() {
                override fun run() {
                    isReconnecting.set(false)
                    connect()
                }
            }, reconnectDelay)
        } else if (reconnectAttempts >= maxReconnectAttempts) {
            Log.err("Failed to connect after $maxReconnectAttempts attempts: ${e.message}")
        }
    }

    private fun startReading(reader: BufferedReader) {
        scope.launch {
            try {
                while (isConnected.get()) {
                    when (val command = readBridgeLine(reader) ?: break) {
                        "message" -> {
                            val message = readBridgeLine(reader)?.let(::decodeBridgePayload)
                                ?: throw IOException("Invalid bridge message payload")
                            lastReceivedMessage = message
                            Call.sendMessage(message)
                        }
                        "banned" -> readBridgeLine(reader) ?: throw IOException("Missing bridge ban payload")
                        "exit" -> break
                        else -> throw IOException("Unknown bridge command: $command")
                    }
                }
                handleConnectionFailure(IOException("Connection closed by server"))
            } catch (e: Exception) {
                if (isConnected.get()) handleConnectionFailure(e)
            }
        }
    }

    private fun startWriting() {
        scope.launch {
            try {
                while (isConnected.get()) {
                    val message = messageQueue.poll(100, TimeUnit.MILLISECONDS) ?: continue
                    val activeWriter = writer ?: throw IOException("Bridge connection has no writer")
                    synchronized(activeWriter) {
                        activeWriter.write(message)
                        activeWriter.flush()
                    }
                }
            } catch (e: Exception) {
                if (isConnected.get()) handleConnectionFailure(e)
            }
        }
    }

    fun cancel() {
        closeConnection()
        scope.cancel()
    }

    private fun closeConnection() {
        isConnected.set(false)
        writer = null
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.err("Error closing bridge connection", e)
        } finally {
            socket = null
        }
    }

    fun message(message: String) {
        sendPayload("message", message)
    }

    fun send(command: String, vararg parameter: String?) {
        when (command) {
            "crash" -> sendPayload("crash", parameter.firstOrNull().orEmpty())
            "exit" -> closeConnection()
            else -> Log.warn("Unknown bridge command: $command")
        }
    }

    private fun sendPayload(command: String, payload: String) {
        if (!isConnected.get()) {
            Log.warn("Cannot send bridge message: not connected")
            return
        }
        if (payload.toByteArray().size > MAX_BRIDGE_LINE_LENGTH / 2) {
            Log.warn("Bridge message exceeds the configured size limit")
            return
        }
        if (!messageQueue.offer("$command\n${encodeBridgePayload(payload)}\n")) {
            Log.warn("Bridge message queue is full")
        }
    }
}
