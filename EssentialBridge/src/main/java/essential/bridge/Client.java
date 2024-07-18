package essential.bridge;

import arc.util.Log;
import essential.core.Bundle;
import mindustry.gen.Call;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Scanner;

import static essential.bridge.Main.bundle;

public class Client implements Runnable {
    // todo ban 공유 서버 ip
    private String address = "";
    private int port = 6000;
    private Socket socket = new Socket();
    private BufferedReader reader;
    private BufferedWriter writer;
    String lastReceivedMessage = "";

    @Override
    public void run() {
        try {
            socket.connect(new InetSocketAddress(address, port), 5000);
            Log.info(bundle.get("network.client.connected", "$address:$port"));

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

            while (!java.lang.Thread.currentThread().isInterrupted()) {
                try {
                    String d = reader.readLine();
                    switch (d) {
                        case "message":
                            String msg = reader.readLine();
                            lastReceivedMessage = msg;
                            Call.sendMessage(msg);
                            break;
                        case "exit":
                            writer.close();
                            reader.close();
                            socket.close();
                            Thread.currentThread().interrupt();
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    java.lang.Thread.currentThread().interrupt();
                }
            }
        } catch (SocketTimeoutException e) {
            Log.info(bundle.get("network.client.timeout"));
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

    void send(String command, String... parameter) throws IOException {
        switch (command) {
            case "crash":
                try {
                    try (Socket socket = new Socket("mindustry.kr", 6000)) {
                        socket.setSoTimeout(5000);
                        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                            out.write("crash\n");
                            try (Scanner sc = new Scanner(socket.getInputStream())) {
                                sc.nextLine(); // ok
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
                if (reader != null) {
                    write("exit");
                    writer.close();
                    reader.close();
                    socket.close();
                }
                break;
            default:
                break;
        }
    }
}