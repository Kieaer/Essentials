package essentials

import arc.graphics.Color
import com.github.javafaker.Faker
import mindustry.Vars
import mindustry.content.UnitTypes
import mindustry.entities.Units
import mindustry.game.Team
import mindustry.gen.Groups
import mindustry.gen.Player
import mindustry.gen.Playerc
import mindustry.gen.Unit
import mindustry.net.Net.SendMode
import mindustry.net.NetConnection
import java.security.SecureRandom
import kotlin.test.assertNotNull

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
        val player = Player.create()
        player.reset()
        player.admin(false)
        player.con = object : NetConnection(r.nextInt(255).toString() + "." + r.nextInt(255) + "." + r.nextInt(255) + "." + r.nextInt(255)) {
            override fun send(o: Any, sendMode: SendMode) {}
            override fun close() {}
        }
        player.con.usid = randomString(22) + "=="
        player.name = Faker().cat().name()
        player.con.uuid = randomString(22) + "=="
        player.set(r.nextInt(300).toFloat(), r.nextInt(500).toFloat())
        player.color.set(Color.rgb(r.nextInt(255), r.nextInt(255), r.nextInt(255)))
        player.color.a = r.nextFloat()
        player.team(Team.sharded)
        player.unit(UnitTypes.dagger.create(player.team()))
        player.add()
        Groups.player.update()
        if (isFull) {
            PlayerCore.register(player.name, player.uuid(), "South Korea", "ko_KR", "ko-KR", "none", "default",0L, player.name, (if (password.isNotEmpty()) password[0] else "none")!!, false)
            PlayerCore.playerLoad(player, null)
        }
        assertNotNull(player)
        assertNotNull(player.unit())
        return player
    }
}