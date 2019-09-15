package essentials;

import io.anuke.arc.util.Log;
import io.anuke.mindustry.gen.Call;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

class EssentialChatClient {
    private static Socket socket;
    private static String host = EssentialConfig.host;
    private static int port = EssentialConfig.port;
    static void main(String msg) {
        Thread t = new Thread(new Runnable() {
            @Override
            public synchronized void run() {
                Thread.currentThread().setName("Chat client thread");
                try {
                    InetAddress address = InetAddress.getByName(host);
                    socket = new Socket(address, port);

                    OutputStream os = socket.getOutputStream();
                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    BufferedWriter bw = new BufferedWriter(osw);

                    bw.write(msg);
                    bw.flush();
                    Call.sendMessage("[#357EC7][SC] "+msg);
                    Log.info("[SC]"+msg);
                } catch (Exception exception) {
                    exception.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        t.start();
    }
}