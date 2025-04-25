package essential.bridge;

import arc.util.Log;
import mindustry.gen.Call;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

public class Client implements Runnable {
    // todo ban 공유 서버 ip
    Socket socket = new Socket();
    String lastReceivedMessage = "";
    BufferedWriter writer;
    BufferedReader reader;

    @Override
    public void run() {
        try {
            socket.connect(new InetSocketAddress(Main.conf.address, Main.conf.port), 5000);

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            while (!java.lang.Thread.currentThread().isInterrupted()) {
                try {
                    String d = reader.readLine();
                    if (d != null) {
                        switch (d) {
                            case "message":
                                String msg = reader.readLine();
                                lastReceivedMessage = msg;
                                Call.sendMessage(msg);
                                break;
                            case "exit":
                                socket.close();
                                Thread.currentThread().interrupt();
                                break;
                            default:
                                break;
                        }
                    }
                } catch (Exception e) {
                    java.lang.Thread.currentThread().interrupt();
                }
            }
        } catch (SocketTimeoutException e) {
            Log.info(Main.bundle.get("network.client.timeout"));
        } catch (InterruptedIOException e) {
            try {
                socket.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void write(String msg) throws IOException {
        writer.write(msg);
        writer.newLine();
        writer.flush();
    }

    void message(String message) {
        try {
            write("message");
            write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void send(String command, String... parameter) {
        switch (command) {
            case "crash":
                try {
                    try (Socket socket = new Socket("mindustry.kr", 6000)) {
                        socket.setSoTimeout(5000);
                        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                            out.write("crash\n");
                            try (Scanner sc = new Scanner(socket.getInputStream())) {
                                sc.nextLine();
                                out.write(parameter[0] + "\n");
                                out.write("null");
                                sc.nextLine();
                                System.out.println("Crash log reported!");
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("Connection timed out. crash report server may be closed.");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case "exit":
                if (socket != null) {
                    try {
                        write("exit");
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
            default:
                break;
        }
    }
}