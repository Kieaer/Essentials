package essential.bridge;

import arc.util.Log;
import arc.util.Timer;
import mindustry.gen.Call;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static essential.bridge.Main.bundle;
import static essential.bridge.Main.conf;

/**
 * Client implementation for the EssentialBridge plugin.
 * Handles communication with the server.
 */
public class Client implements Runnable {
    // Message queue for outgoing messages
    private final ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<>();

    // Async channel for non-blocking I/O
    private AsynchronousSocketChannel channel;

    // Executor service for async operations
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    // Buffer for reading data
    private final ByteBuffer readBuffer = ByteBuffer.allocate(8192);

    // Buffer for writing data
    private final ByteBuffer writeBuffer = ByteBuffer.allocate(8192);

    // Track if we're connected
    private final AtomicBoolean isConnected = new AtomicBoolean(false);

    // Track if we're currently reconnecting
    private final AtomicBoolean isReconnecting = new AtomicBoolean(false);

    // Last received message
    private String lastReceivedMessage = "";

    // Maximum number of reconnection attempts
    private final int maxReconnectAttempts = 5;

    // Current reconnection attempt
    private int reconnectAttempts = 0;

    // Delay between reconnection attempts (in seconds)
    private final float reconnectDelay = 5f;

    @Override
    public void run() {
        connect();
    }

    private void connect() {
        executor.submit(() -> {
            try {
                // Create a new async channel
                channel = AsynchronousSocketChannel.open();

                // Connect to the server
                int port = conf.getPort();
                InetSocketAddress address = new InetSocketAddress(conf.getAddress(), port);

                Future<Void> connectFuture = channel.connect(address);
                connectFuture.get(); // Wait for connection to complete

                isConnected.set(true);
                reconnectAttempts = 0;
                Log.info(bundle.get("network.client.connected", conf.getAddress()));

                // Start reading and writing
                startReading();
                startWriting();

            } catch (Exception e) {
                handleConnectionFailure(e);
            }
        });
    }

    private void handleConnectionFailure(Exception e) {
        isConnected.set(false);

        if (reconnectAttempts < maxReconnectAttempts && !isReconnecting.get()) {
            isReconnecting.set(true);
            reconnectAttempts++;

            Log.warn("Connection failed (attempt " + reconnectAttempts + "/" + maxReconnectAttempts + "): " + e.getMessage());

            // Schedule reconnection after delay
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    isReconnecting.set(false);
                    connect();
                }
            }, reconnectDelay);
        } else if (reconnectAttempts >= maxReconnectAttempts) {
            Log.err("Failed to connect after " + maxReconnectAttempts + " attempts: " + e.getMessage());
        }
    }

    private void startReading() {
        executor.submit(() -> {
            try {
                while (isConnected.get()) {
                    readBuffer.clear();

                    // Read data asynchronously
                    Future<Integer> readFuture = channel.read(readBuffer);
                    int bytesRead = readFuture.get(); // Wait for read to complete

                    if (bytesRead <= 0) {
                        // Connection closed
                        isConnected.set(false);
                        handleConnectionFailure(new IOException("Connection closed by server"));
                        break;
                    }

                    // Process the received data
                    readBuffer.flip();
                    String data = StandardCharsets.UTF_8.decode(readBuffer).toString();
                    processReceivedData(data);
                }
            } catch (Exception e) {
                if (isConnected.get()) {
                    isConnected.set(false);
                    handleConnectionFailure(e);
                }
            }
        });
    }

    private void processReceivedData(String data) {
        // Split the data by newlines to get individual messages
        String[] lines = data.split("\n");

        for (int i = 0; i < lines.length - 1; i += 2) {
            if (i + 1 < lines.length) {
                String command = lines[i].trim();
                String content = lines[i + 1].trim();

                switch (command) {
                    case "message":
                        lastReceivedMessage = content;
                        Call.sendMessage(content);
                        break;
                    case "exit":
                        closeConnection();
                        break;
                }
            }
        }
    }

    private void startWriting() {
        executor.submit(() -> {
            try {
                long lastFlushTime = System.currentTimeMillis();
                final long flushInterval = 100L; // Flush every 100ms instead of every message

                while (isConnected.get()) {
                    if (!messageQueue.isEmpty()) {
                        long currentTime = System.currentTimeMillis();
                        String message = messageQueue.poll();

                        if (message != null) {
                            writeBuffer.clear();
                            writeBuffer.put(message.getBytes(StandardCharsets.UTF_8));
                            writeBuffer.flip();

                            // Write data asynchronously
                            while (writeBuffer.hasRemaining()) {
                                Future<Integer> writeFuture = channel.write(writeBuffer);
                                writeFuture.get(); // Wait for write to complete
                            }

                            // Only flush if it's been long enough since the last flush
                            if (currentTime - lastFlushTime > flushInterval) {
                                lastFlushTime = currentTime;
                            }
                        }
                    } else {
                        // Sleep a bit to avoid busy waiting
                        Thread.sleep(10);
                    }
                }
            } catch (Exception e) {
                if (isConnected.get()) {
                    isConnected.set(false);
                    handleConnectionFailure(e);
                }
            }
        });
    }

    private void closeConnection() {
        isConnected.set(false);
        try {
            if (channel != null) {
                channel.close();
            }
        } catch (Exception e) {
            Log.err("Error closing connection", e);
        }
    }

    /**
     * Send a message to the server.
     *
     * @param message The message to send
     */
    public void message(String message) {
        if (isConnected.get()) {
            messageQueue.add("message\n" + message + "\n");
        } else {
            Log.warn("Cannot send message: not connected");
        }
    }

    /**
     * Send a command to the server.
     *
     * @param command The command to send
     * @param parameter The parameters for the command
     */
    public void send(String command, String... parameter) {
        switch (command) {
            case "crash":
                executor.submit(() -> {
                    try {
                        java.net.Socket socket = new java.net.Socket("mindustry.kr", 6000);
                        socket.setSoTimeout(5000);
                        java.io.BufferedWriter out = new java.io.BufferedWriter(new java.io.OutputStreamWriter(socket.getOutputStream()));
                        out.write("crash\n");
                        java.util.Scanner sc = new java.util.Scanner(socket.getInputStream());
                        sc.nextLine();
                        out.write(parameter[0] + "\n");
                        out.write("null");
                        sc.nextLine();
                        Log.info("Crash log reported!");
                        sc.close();
                        out.close();
                        socket.close();
                    } catch (java.net.SocketTimeoutException e) {
                        Log.warn("Connection timed out. Crash report server may be closed.");
                    } catch (IOException e) {
                        Log.err(e);
                    }
                });
                break;

            case "exit":
                if (isConnected.get()) {
                    messageQueue.add("exit\n");
                    closeConnection();
                }
                break;

            default:
                Log.warn("Unknown command: " + command);
                break;
        }
    }

    /**
     * Shutdown the client and clean up resources.
     */
    public void shutdown() {
        closeConnection();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Get the last received message.
     *
     * @return The last received message
     */
    public String getLastReceivedMessage() {
        return lastReceivedMessage;
    }

    /**
     * Set the last received message.
     *
     * @param lastReceivedMessage The last received message
     */
    public void setLastReceivedMessage(String lastReceivedMessage) {
        this.lastReceivedMessage = lastReceivedMessage;
    }
}