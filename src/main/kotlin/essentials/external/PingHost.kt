package essentials.external

import mindustry.net.Host
import mindustry.net.NetworkIO
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.function.Consumer

object PingHost {
    // source from https://github.com/Anuken/CoreBot/blob/master/src/corebot/Net.java#L57-L84
    operator fun get(ip: String, port: Int, listener: Consumer<Host>) {
        try {
            DatagramSocket().use { socket ->
                socket.send(DatagramPacket(byteArrayOf(-2, 1), 2, InetAddress.getByName(ip), port))
                socket.soTimeout = 1000
                val packet = DatagramPacket(ByteArray(256), 256)
                val start = System.currentTimeMillis()
                socket.receive(packet)
                val buffer = ByteBuffer.wrap(packet.data)
                listener.accept(readServerData(ip, buffer, ((System.currentTimeMillis() - start).toInt())))
                socket.disconnect()
            }
        } catch(e: Exception) {
            listener.accept(Host(0, null, ip, null, 0, 0, 0, null, null, 0, null, null))
        }
    }

    private fun readServerData(ip: String, buffer: ByteBuffer, ping: Int): Host {
        val host = NetworkIO.readServerData(ping, ip, buffer)
        host.ping = ping
        return host
    }
}