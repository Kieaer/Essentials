package remake

import com.ip2location.IP2Location
import com.neovisionaries.i18n.CountryCode
import mindustry.gen.Groups
import mindustry.gen.Playerc
import remake.Main.Companion.database
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.*
import java.util.*


object Trigger {
    fun loadPlayer(player: Playerc, data: DB.PlayerData) {
        player.name(data.name)
        data.lastdate = System.currentTimeMillis()
        data.joincount = data.joincount++

        val perm = Permission[player]
        if (perm.name.isNotEmpty()) player.name(Permission[player].name)
        player.admin(Permission[player].admin)
        data.permission = Permission[player].group

        database.players.add(data)

        player.sendMessage("Data Loaded. Welcome to the server.")
    }

    fun createPlayer(player: Playerc, id: String?, password: String?) {
        val data = DB.PlayerData

        val ip = player.ip()
        val isLocal = try {
            val address = InetAddress.getByName(ip)
            if (address.isAnyLocalAddress || address.isLoopbackAddress) {
                true
            } else {
                NetworkInterface.getByInetAddress(address) != null
            }
        } catch (e: SocketException) {
            false
        } catch (e: UnknownHostException) {
            false
        }

        val ip2location = IP2Location()
        ip2location.Open(Main::class.java.classLoader.getResourceAsStream("IP2LOCATION-LITE-DB1.BIN").readBytes())

        val res = if (isLocal) {
            val add = BufferedReader(InputStreamReader(URL("http://checkip.amazonaws.com").openStream())).readLine()
            ip2location.IPQuery(add).countryShort
        } else {
            ip2location.IPQuery(player.ip()).countryShort
        }
        val locale = CountryCode.getByCode(res).toLocale()

        data.languageTag = locale.toLanguageTag()
        data.name = player.name()
        data.uuid = player.uuid()
        data.joinDate = System.currentTimeMillis()
        data.id = id ?: player.name()
        data.pw = password ?: player.name()
        data.permission = "user"

        database.createData(data)

        player.sendMessage("Player data registered!")
        loadPlayer(player, data)
    }

    // 1초마다 작동함
    class Time : TimerTask() {
        private var colorOffset = 0

        override fun run() {
            database.players.forEach {
                it.playtime = it.playtime + 1
            }

            for (player in Groups.player) {
                if (!player.isNull) {
                    val p = database.players.find { a -> a.uuid == player.uuid() }

                    if (p != null) {
                        // 무지개 닉네임
                        if (p.colornick) {
                            val name = p.name.replace("\\[(.*?)]".toRegex(), "")
                            nickcolor(name, player)
                        } else {
                            player.name(p.name)
                        }

                        // 잠수 플레이어 카운트

                    }
                }
            }
        }

        private fun nickcolor(name: String, player: Playerc) {
            val stringBuilder = StringBuilder()
            val colors = arrayOfNulls<String>(11)
            colors[0] = "[#ff0000]"
            colors[1] = "[#ff7f00]"
            colors[2] = "[#ffff00]"
            colors[3] = "[#7fff00]"
            colors[4] = "[#00ff00]"
            colors[5] = "[#00ff7f]"
            colors[6] = "[#00ffff]"
            colors[7] = "[#007fff]"
            colors[8] = "[#0000ff]"
            colors[9] = "[#8000ff]"
            colors[10] = "[#ff00ff]"
            val newName = arrayOfNulls<String>(name.length)
            for (i in name.indices) {
                val c = name[i]
                var colorIndex = (i + colorOffset) % colors.size
                if (colorIndex < 0) {
                    colorIndex += colors.size
                }
                val newtext = colors[colorIndex] + c
                newName[i] = newtext
            }
            colorOffset--
            for (s in newName) {
                stringBuilder.append(s)
            }
            player.name(stringBuilder.toString())
        }
    }
}