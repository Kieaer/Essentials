package essentials;

import io.anuke.arc.util.Log;
import io.anuke.mindustry.gen.Call;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

class EssentialChatServer implements Runnable {
    private static int port = EssentialConfig.serverport;
    private static Socket socket;
    static boolean active = true;
    static ServerSocket serverSocket;

    static {
        try {
            serverSocket = new ServerSocket(port);
            Log.info("[Essentials] Chat server listening to the port " + port);
        } catch (IOException e) {
            Log.err("[Essentials] Failure to open port "+port+"!!!1");
        }
    }

    @Override
    public synchronized void run() {
        Thread.currentThread().setName("Chat server thread");
        try {
            while (active) {
                socket = serverSocket.accept();
                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                BufferedReader br = new BufferedReader(isr);

                String message = br.readLine();
                String remoteip = socket.getRemoteSocketAddress().toString();

                Log.info("[RC] Received message from "+remoteip+": "+message);

                if(!remoteip.equals(EssentialConfig.clienthost)) {
                    Log.warn("[EssentialsChat] ALERT! This message isn't received from "+EssentialConfig.clienthost+"!!");
                    Log.warn("[EssentialsChat] Message is "+message);
                } else {
                    Call.sendMessage("[#C77E36][RC] " + message);
                }
            }
        } catch (Exception ignored) {
        } finally {
            try {
                socket.close();
            } catch (Exception e) {
                if(active){
                    e.printStackTrace();
                }
            }
        }
    }
}