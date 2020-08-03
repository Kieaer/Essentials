package essentials

import arc.graphics.Color
import com.github.javafaker.Faker
import essentials.Main.Companion.perm
import essentials.Main.Companion.playerCore
import mindustry.Vars
import mindustry.entities.type.Player
import mindustry.net.Net.SendMode
import mindustry.net.NetConnection
import java.security.SecureRandom

object PluginTestDB {
    var r = SecureRandom()
    fun randomString(length: Int): String {
        val leftLimit = 48
        val rightLimit = 122
        return r.ints(leftLimit, rightLimit + 1)
                .filter { i: Int -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97) }
                .limit(length.toLong())
                .collect({ StringBuilder() }, { obj: StringBuilder, codePoint: Int -> obj.appendCodePoint(codePoint) }) { obj: StringBuilder, s: StringBuilder? -> obj.append(s) }
                .toString()
    }

    fun createNewPlayer(isFull: Boolean, vararg password: String?): Player {
        val player = Player()
        player.isAdmin = false
        player.con = object : NetConnection(r.nextInt(255).toString() + "." + r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255)) {
            override fun send(o: Any, sendMode: SendMode) {}
            override fun close() {}
        }
        player.usid = randomString(22) + "=="
        player.name = Faker().cat().name()
        player.uuid = randomString(22) + "=="
        player.isMobile = false
        player.dead = false
        player.setNet(r.nextInt(300).toFloat(), r.nextInt(500).toFloat())
        player.color.set(Color.rgb(r.nextInt(255), r.nextInt(255), r.nextInt(255)))
        player.color.a = r.nextFloat()
        player.add()
        Vars.playerGroup.updateEvents()
        if (isFull) {
            playerCore.register(player.name, player.uuid, "South Korea", "ko_KR", "ko-KR", true, "127.0.0.1", "default", 0L, player.name, (if (password.size != 0) password[0] else "none")!!, false)
            playerCore.playerLoad(player, null)
        }
        return player
    }
}