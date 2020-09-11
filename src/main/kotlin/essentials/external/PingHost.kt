package essentials.external

import mindustry.game.Gamemode
import mindustry.net.Host
import mindustry.net.NetworkIO
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.util.function.Consumer

class PingHost(ip: String, port: Int, listener: Consumer<Host>) {
    fun readServerData(ip: String, buffer: ByteBuffer, ping: Long): Host {
        val host = NetworkIO.readServerData(0, ip, buffer)
        host.ping = ping.toInt()
        return host
    }

    // source from https://github.com/Anuken/CoreBot/blob/master/src/corebot/Net.java#L57-L84
    init {
        try {
            DatagramSocket().use { socket ->
                socket.send(DatagramPacket(byteArrayOf(-2, 1), 2, InetAddress.getByName(ip), port))
                socket.soTimeout = 1000
                val packet = DatagramPacket(ByteArray(256), 256)
                val start = System.currentTimeMillis()
                socket.receive(packet)
                val buffer = ByteBuffer.wrap(packet.data)
                listener.accept(readServerData(ip, buffer, System.currentTimeMillis() - start))
                socket.disconnect()
            }
        } catch (e: Exception) {
            listener.accept(Host(0, ip, null, "invaild", 0, 0, 0, null, Gamemode.editor, 0, "invalid description", "invalid modename"))
        }
    }
}