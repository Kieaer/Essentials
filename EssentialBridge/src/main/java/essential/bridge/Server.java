package essential.bridge;

import arc.util.Log;
import arc.util.serialization.Json;
import essential.PluginDataKt;
import mindustry.Vars;
import mindustry.net.Administration;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server implementation for the EssentialBridge plugin.
 * Handles server-side communication.
 */
public class Server implements Runnable {
    private ServerSocket server;
    private String lastSentMessage = "";
    private ArrayList<Socket> clients = new ArrayList<>();
    private ExecutorService executor = Executors.newCachedThreadPool();
    private volatile boolean running = true;

    @Override
    public void run() {
        try {
            server = new ServerSocket(Main.conf.getPort());
            while (!Thread.currentThread().isInterrupted() && running) {
                Socket socket = server.accept();
                Log.debug(
                    Main.bundle.get("network.server.connected", socket.getInetAddress().getHostAddress())
                );
                clients.add(socket);
                executor.submit(() -> start(socket));
            }
        } catch (SocketException e) {
            // Socket closed, likely during shutdown
        } catch (Exception e) {
            Log.err(e);
        }
    }

    /**
     * Shutdown the server and clean up resources.
     */
    public void shutdown() {
        running = false;
        try {
            if (server != null) {
                server.close();
            }
        } catch (IOException e) {
            Log.err(e);
        }
        executor.shutdown();
    }

    /**
     * Send a message to all connected clients.
     *
     * @param type The message type
     * @param msg The message content
     */
    public void sendAll(String type, String msg) {
        for (Socket socket : new ArrayList<>(clients)) { // Create a copy to avoid concurrent modification
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                writer.write(type);
                writer.newLine();
                writer.flush();
                writer.write(msg);
                writer.newLine();
                writer.flush();
            } catch (SocketException e) {
                Log.err(e);
                try {
                    socket.close();
                } catch (IOException ex) {
                    Log.err(ex);
                }
                clients.remove(socket);
            } catch (IOException e) {
                Log.err(e);
            }
        }
    }

    /**
     * Handle a client connection.
     *
     * @param socket The client socket
     */
    private void start(Socket socket) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            boolean isAlive = true;

            while (isAlive && running) {
                String data = reader.readLine();
                if (data == null) {
                    System.out.println("reader data received null");
                    isAlive = false;
                } else {
                    switch (data) {
                        case "isBanned":
                            Administration.PlayerInfo info = new Json().fromJson(Administration.PlayerInfo.class, reader.readLine());
                            boolean banned = Vars.netServer.admins.isIDBanned(info.id);
                            for (String ip : info.ips) {
                                if (Vars.netServer.admins.isIPBanned(ip)) {
                                    banned = true;
                                    break;
                                }
                            }
                            if (banned) {
                                info.banned = true;
                                sendAll("banned", new Json().toJson(info, Administration.PlayerInfo.class));
                            }
                            break;

                        case "exit":
                            isAlive = false;
                            break;

                        case "message":
                            sendAll("message", reader.readLine());
                            break;

                        case "crash":
                            StringBuilder stacktrace = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null && !line.equals("null")) {
                                stacktrace.append(line).append("\n");
                            }
                            PluginDataKt.getRootPath().child("report/" + LocalDateTime.now().withNano(0) + ".txt")
                                .writeString(stacktrace.toString());
                            Log.info("Crash log received from " + socket.getInetAddress().getHostAddress());
                            break;
                    }
                }
            }
        } catch (SocketException e) {
            Log.err(e);
        } catch (Exception e) {
            Log.err(e);
        }
        clients.remove(socket);
        Log.info(Main.bundle.get("network.server.disconnected", socket.getInetAddress().getHostAddress()));
    }

    /**
     * Get the last sent message.
     *
     * @return The last sent message
     */
    public String getLastSentMessage() {
        return lastSentMessage;
    }

    /**
     * Set the last sent message.
     *
     * @param lastSentMessage The last sent message
     */
    public void setLastSentMessage(String lastSentMessage) {
        this.lastSentMessage = lastSentMessage;
    }

    /**
     * Get the list of connected clients.
     *
     * @return The list of connected clients
     */
    public ArrayList<Socket> getClients() {
        return clients;
    }
}