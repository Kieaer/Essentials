package essentials;

import io.anuke.arc.util.Log;
import io.anuke.mindustry.gen.Call;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

class EssentialChatServer implements Runnable {
    private static int port = EssentialConfig.serverport;
    private static Socket socket;
    static boolean active = true;
    static ServerSocket serverSocket;

    static {
        try {
            serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            Log.err("[Essentials] Failure to open port "+port+"!!!1");
        }
    }

    @Override
    public synchronized void run() {
        Thread.currentThread().setName("Chat server thread");
        try {
            Log.info("[Essentials] Chat server listening to the port " + port);
            while (active) {
                socket = serverSocket.accept();
                InputStream is = socket.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String message = br.readLine();
                Call.sendMessage("[#C77E36][RC] " + message);
                Log.info("[RC] "+message);
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