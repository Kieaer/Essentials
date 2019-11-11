package essentials.special;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class PingServer {
    public static DatagramSocket socket;

    static {
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public static void pingServer(String ip, int port, Consumer<PingResult> listener) {
        try {
            socket.send(new DatagramPacket(new byte[]{-2, 1}, 2, InetAddress.getByName(ip), port));

            socket.setSoTimeout(500);

            DatagramPacket packet = new DatagramPacket(new byte[256], 256);

            long start = System.currentTimeMillis();
            socket.receive(packet);

            ByteBuffer buffer = ByteBuffer.wrap(packet.getData());
            listener.accept(readServerData(buffer, ip, System.currentTimeMillis() - start));
            socket.disconnect();
        } catch (Exception ignored) {}
    }

    private static PingResult readServerData(ByteBuffer buffer, String ip, long ping){
        byte hlength = buffer.get();
        byte[] hb = new byte[hlength];
        buffer.get(hb);

        byte mlength = buffer.get();
        byte[] mb = new byte[mlength];
        buffer.get(mb);

        String host = new String(hb);
        String map = new String(mb);

        int players = buffer.getInt();
        int wave = buffer.getInt();
        int version = buffer.getInt();

        return new PingResult(ip, ping, players+"", host, map, wave+"", version == -1 ? "Custom Build" : (""+version));
    }

    public static class PingResult{
        public boolean valid;
        public String players;
        public String host;
        public String error;
        public String wave;
        public String map;
        public String ip;
        public String version;
        public long ping;

        public PingResult(String ip, String error){
            this.valid = false;
            this.error = error;
            this.ip = ip;
        }

        public PingResult(String error){
            this.valid = false;
            this.error = error;
        }

        PingResult(String ip, long ping, String players, String host, String map, String wave, String version){
            this.ping = ping;
            this.ip = ip;
            this.valid = true;
            this.players = players;
            this.host = host;
            this.map = map;
            this.wave = wave;
            this.version = version;
        }
    }
}
