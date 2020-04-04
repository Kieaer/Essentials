package essentials.special;

import arc.util.Strings;
import mindustry.net.Host;
import mindustry.net.NetworkIO;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class PingServer {
    public static void pingServer(String ip, Consumer<Host> listener) {
        try {
            String resultIP = ip;
            int port = 6567;
            if(ip.contains(":") && Strings.canParsePostiveInt(ip.split(":")[1])){
                resultIP = ip.split(":")[0];
                port = Strings.parseInt(ip.split(":")[1]);
            }

            DatagramSocket socket = new DatagramSocket();
            socket.setSoTimeout(1000);

            socket.send(new DatagramPacket(new byte[]{-2, 1}, 2, InetAddress.getByName(resultIP), port));

            DatagramPacket packet = new DatagramPacket(new byte[256], 256);

            long start = System.currentTimeMillis();
            socket.receive(packet);

            ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
            listener.accept(readServerData(buffer, ip, System.currentTimeMillis() - start));
            socket.disconnect();
            socket.close();
        } catch (Exception e) {
            listener.accept(new Host(null, ip, null, 0, 0, 0, null, null, 0, null));
        }
    }

    private static Host readServerData(ByteBuffer buffer, String ip, long ping){
        Host host = NetworkIO.readServerData(ip, buffer);
        host.ping = (int)ping;
        return host;
    }
}
