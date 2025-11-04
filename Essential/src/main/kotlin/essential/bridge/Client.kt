package essential.bridge

import arc.util.Log
import arc.util.Timer
import essential.common.AppDispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mindustry.gen.Call
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class Client : Runnable {
    // Message queue for outgoing messages
    private val messageQueue = ConcurrentLinkedQueue<String>()

    // Async channel for non-blocking I/O
    private var channel: AsynchronousSocketChannel? = null

    // Coroutine scope for async operations
    private val scope = CoroutineScope(AppDispatchers.io)

    // Buffer for reading data
    private val readBuffer = ByteBuffer.allocate(8192)

    // Buffer for writing data
    private val writeBuffer = ByteBuffer.allocate(8192)

    // Track if we're connected
    private val isConnected = AtomicBoolean(false)

    // Track if we're currently reconnecting
    private val isReconnecting = AtomicBoolean(false)

    // Last received message
    var lastReceivedMessage: String? = ""

    // Maximum number of reconnection attempts
    private val maxReconnectAttempts = 5

    // Current reconnection attempt
    private var reconnectAttempts = 0

    // Delay between reconnection attempts (in seconds)
    private val reconnectDelay = 5f

    override fun run() {
        connect()
    }

    private fun connect() {
        scope.launch {
            try {
                // Create a new async channel
                channel = AsynchronousSocketChannel.open()

                // Connect to the server
                val port = BridgeService.conf.port
                val address = InetSocketAddress(BridgeService.conf.address, port)

                scope.async {
                    channel?.connect(address)?.get()
                }.await()

                isConnected.set(true)
                reconnectAttempts = 0
                Log.info(BridgeService.bundle["network.client.connected", BridgeService.conf.address])

                // Start reading and writing
                startReading()
                startWriting()

            } catch (e: Exception) {
                handleConnectionFailure(e)
            }
        }
    }

    private fun handleConnectionFailure(e: Exception) {
        isConnected.set(false)

        if (reconnectAttempts < maxReconnectAttempts && !isReconnecting.get()) {
            isReconnecting.set(true)
            reconnectAttempts++

            Log.warn("Connection failed (attempt $reconnectAttempts/$maxReconnectAttempts): ${e.message}")

            // Schedule reconnection after delay
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

    private fun startReading() {
        scope.launch {
            try {
                while (isConnected.get()) {
                    readBuffer.clear()

                    // Read data asynchronously
                    val bytesRead = scope.async {
                        channel?.read(readBuffer)?.get() ?: -1
                    }.await()

                    if (bytesRead <= 0) {
                        // Connection closed
                        isConnected.set(false)
                        handleConnectionFailure(IOException("Connection closed by server"))
                        break
                    }

                    // Process the received data
                    readBuffer.flip()
                    val data = StandardCharsets.UTF_8.decode(readBuffer).toString()
                    processReceivedData(data)
                }
            } catch (e: Exception) {
                if (isConnected.get()) {
                    isConnected.set(false)
                    handleConnectionFailure(e)
                }
            }
        }
    }

    private fun processReceivedData(data: String) {
        // Split the data by newlines to get individual messages
        val lines = data.split("\n")

        for (i in 0 until lines.size - 1 step 2) {
            if (i + 1 < lines.size) {
                val command = lines[i].trim()
                val content = lines[i + 1].trim()

                when (command) {
                    "message" -> {
                        lastReceivedMessage = content
                        Call.sendMessage(content)
                    }
                    "exit" -> {
                        closeConnection()
                    }
                }
            }
        }
    }

    private fun startWriting() {
        scope.launch {
            try {
                var lastFlushTime = System.currentTimeMillis()
                val flushInterval = 100L // Flush every 100ms instead of every message

                while (isConnected.get()) {
                    if (messageQueue.isNotEmpty()) {
                        val currentTime = System.currentTimeMillis()
                        val message = messageQueue.poll()

                        if (message != null) {
                            writeBuffer.clear()
                            writeBuffer.put(message.toByteArray(StandardCharsets.UTF_8))
                            writeBuffer.flip()

                            // Write data asynchronously
                            scope.async {
                                while (writeBuffer.hasRemaining()) {
                                    channel?.write(writeBuffer)?.get()
                                }
                            }.await()

                            // Only flush if it's been long enough since the last flush
                            if (currentTime - lastFlushTime > flushInterval) {
                                lastFlushTime = currentTime
                            }
                        }
                    } else {
                        // Sleep a bit to avoid busy waiting
                        withContext(AppDispatchers.io) {
                            Thread.sleep(10)
                        }
                    }
                }
            } catch (e: Exception) {
                if (isConnected.get()) {
                    isConnected.set(false)
                    handleConnectionFailure(e)
                }
            }
        }
    }

    private fun closeConnection() {
        isConnected.set(false)
        try {
            channel?.close()
        } catch (e: Exception) {
            Log.err("Error closing connection", e)
        }
    }

    fun message(message: String) {
        if (isConnected.get()) {
            messageQueue.add("message\n$message\n")
        } else {
            Log.warn("Cannot send message: not connected")
        }
    }

    fun send(command: String, vararg parameter: String?) {
        when (command) {
            "crash" -> {
                scope.launch {
                    try {
                        withContext(AppDispatchers.io) {
                            Socket("mindustry.kr", 6000).use { socket ->
                                socket.setSoTimeout(5000)
                                BufferedWriter(OutputStreamWriter(socket.getOutputStream())).use { out ->
                                    out.write("crash\n")
                                    Scanner(socket.getInputStream()).use { sc ->
                                        sc.nextLine()
                                        out.write(parameter[0] + "\n")
                                        out.write("null")
                                        sc.nextLine()
                                        Log.info("Crash log reported!")
                                    }
                                }
                            }
                        }
                    } catch (e: SocketTimeoutException) {
                        Log.warn("Connection timed out. Crash report server may be closed.")
                    } catch (e: IOException) {
                        Log.err(e)
                    }
                }
            }

            "exit" -> {
                if (isConnected.get()) {
                    messageQueue.add("exit\n")
                    closeConnection()
                }
            }

            else -> {
                Log.warn("Unknown command: $command")
            }
        }
    }
}
