package essential.bridge;

import arc.util.Log;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static essential.core.Main.root;

public class Server implements Runnable {
    private ServerSocket server;
    String lastSentMessage = "";
    List<Socket> clients = new ArrayList<>();

    @Override
    public void run() {
        try {
            server = new ServerSocket(57293);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = server.accept();
                Log.info(Main.bundle.get("network.server.connected", socket.getInetAddress().getHostAddress()));
                clients.add(socket);
                Handler handler = new Handler(socket);
                handler.start();
            }
        } catch (SocketException ignored) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void shutdown() {
        Thread.currentThread().interrupt();
        try {
            server.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendAll(String type, String msg) {
        for (Socket a : clients) {
            try (BufferedWriter b = new BufferedWriter(new OutputStreamWriter(a.getOutputStream()))) {
                b.write(type);
                b.newLine();
                b.flush();
                b.write(msg);
                b.newLine();
                b.flush();
            } catch (SocketException e) {
                try {
                    a.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
                clients.remove(a);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                while (!currentThread().isInterrupted()) {
                    String d = reader.readLine();
                    if (d == null) {
                        interrupt();
                    } else {
                        switch (d) {
                            case "exit":
                                interrupt();
                                break;
                            case "message":
                                String msg = reader.readLine();
                                sendAll("message", msg);
                                break;
                            case "crash":
                                StringBuilder stacktrace = new StringBuilder();
                                String line;
                                while ((line = reader.readLine()) != null && !line.equals("null")) {
                                    stacktrace.append(line).append("\n");
                                }
                                root.child("report/" + LocalDateTime.now().withNano(0) + ".txt").writeString(stacktrace.toString());
                                Log.info("Crash log received from " + socket.getInetAddress().getHostAddress());
                                break;
                        }
                    }
                }
            } catch (SocketException ignored) {
            } catch (Exception e) {
                e.printStackTrace();
            }
            clients.remove(socket);
            Log.info(Main.bundle.get("network.server.disconnected", socket.getInetAddress().getHostAddress()));
        }
    }
}