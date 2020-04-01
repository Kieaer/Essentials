package remake.external;

import mindustry.net.Host;
import mindustry.net.NetworkIO;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class PingHost {
    // source from https://github.com/Anuken/CoreBot/blob/master/src/corebot/Net.java#L57-L84
    public PingHost(String ip, int port, Consumer<Host> listener) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.send(new DatagramPacket(new byte[]{-2, 1}, 2, InetAddress.getByName(ip), port));

            socket.setSoTimeout(1000);

            DatagramPacket packet = new DatagramPacket(new byte[256], 256);

            long start = System.currentTimeMillis();
            socket.receive(packet);

            ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
            listener.accept(readServerData(ip, buffer, System.currentTimeMillis() - start));
            socket.disconnect();
        } catch (Exception e) {
            listener.accept(new Host(null, ip, null, 0, 0, 0, null, null, 0, null));
        }
    }

    public Host readServerData(String ip, ByteBuffer buffer, long ping) {
        Host host = NetworkIO.readServerData(ip, buffer);
        host.ping = (int) ping;
        return host;
    }
}
