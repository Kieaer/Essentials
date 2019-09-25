package essentials;

import io.anuke.arc.collection.Array;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.net.Administration;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class EssentialBanClient implements Runnable {
    private static Socket socket;
    private static String host = EssentialConfig.clienthost;
    private static int port = EssentialConfig.clientport;

    @Override
    public void run() {
        Log.info("[Essential] Welcome to the ban sharing features!");
        Array<Administration.PlayerInfo> bans = Vars.netServer.admins.getBanned();
        for (Administration.PlayerInfo info : bans) {
            try {
                InetAddress address = InetAddress.getByName(host);
                socket = new Socket(address, port);

                BufferedWriter bw;
                bw = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

                bw.write(info.id);
                bw.flush();
                Log.info("[Essentials] Banned " + info.id + " sented.");
            } catch (Exception ignored) {}
        }
    }
}
