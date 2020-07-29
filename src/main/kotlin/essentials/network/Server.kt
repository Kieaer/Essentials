package essentials.network

import arc.Core
import arc.struct.Array
import essentials.Main
import essentials.Main.Companion.configs
import essentials.Main.Companion.playerCore
import essentials.Main.Companion.pluginVars
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.internal.Log.LogType
import essentials.internal.PluginException
import mindustry.Vars
import mindustry.core.GameState
import mindustry.core.Version
import mindustry.game.Team
import mindustry.type.ItemType
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.JsonValue
import org.jsoup.Jsoup
import org.mindrot.jbcrypt.BCrypt
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.regex.Pattern
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class Server : Runnable {
    var list = Array<Service?>()
    lateinit var serverSocket: ServerSocket
    var bundle = Bundle()
    fun shutdown() {
        try {
            Thread.currentThread().interrupt()
            serverSocket.close()
        } catch (e: IOException) {
            CrashReport(e)
        }
    }

    override fun run() {
        try {
            serverSocket = ServerSocket(configs.serverPort)
            Log.info("server.enabled")
            while (!serverSocket.isClosed) {
                val socket = serverSocket.accept()
                try {
                    val service = Service(socket)
                    service.start()
                    list.add(service)
                } catch (ignored: PluginException) {
                    // if key is null
                }
            }
        } catch (e: IOException) {
            if (!e.message.equals("socket closed", ignoreCase = true)) {
                CrashReport(e)
            }
            Thread.currentThread().interrupt()
        }
    }

    internal enum class Request {
        Ping, BanSync, Chat, Exit, UnbanIP, UnbanID, DataShare, CheckBan
    }

    inner class Service(var socket: Socket) : Thread() {
        var br: BufferedReader
        var os: DataOutputStream
        lateinit var spec: SecretKey
        var ip: String

        fun shutdown(bundle: String?, vararg parameter: String?) {
            try {
                os.close()
                br.close()
                socket.close()
                list.remove(this)
                if (bundle != null) Log.server(bundle, *parameter)
            } catch (ignored: Exception) {
            }
        }

        override fun run() {
            try {
                while (!currentThread().isInterrupted) {
                    currentThread().name = "$ip Client Thread"
                    ip = socket.inetAddress.toString().replace("/", "")
                    val value = Main.tool.decrypt(br.readLine(), spec)
                    val answer = JsonObject()
                    val data = JsonValue.readJSON(value).asObject()
                    when (Request.valueOf(data["type"].asString())) {
                        Request.Ping -> {
                            val msg = arrayOf("Hi $ip! Your connection is successful!", "Hello $ip! I'm server!", "Welcome to the server $ip!")
                            val rnd = SecureRandom().nextInt(msg.size)
                            answer.add("result", msg[rnd])
                            os.writeBytes(Main.tool.encrypt(answer.toString(), spec).trimIndent())
                            os.flush()
                            Log.server("client.connected", ip)
                        }
                        Request.BanSync -> {
                            Log.server("client.request.banlist", ip)

                            // 적용
                            val ban = data["ban"].asArray()
                            val ipban = data["ipban"].asArray()
                            val subban = data["subban"].asArray()
                            for (b in ban) {
                                Vars.netServer.admins.banPlayerID(b.asString())
                            }
                            for (b in ipban) {
                                Vars.netServer.admins.banPlayerIP(b.asString())
                            }
                            for (b in subban) {
                                Vars.netServer.admins.addSubnetBan(b.asString())
                            }

                            // 가져오기
                            val bans = JsonArray()
                            val ipbans = JsonArray()
                            val subbans = JsonArray()
                            for (b in Vars.netServer.admins.banned) {
                                bans.add(b.id)
                            }
                            for (b in Vars.netServer.admins.bannedIPs) {
                                ipbans.add(b)
                            }
                            for (b in Vars.netServer.admins.subnetBans) {
                                subbans.add(b)
                            }
                            answer.add("type", "bansync")
                            answer.add("ban", ban)
                            answer.add("ipban", ipban)
                            answer.add("subban", subban)
                            for (ser in list) {
                                val remoteip = ser!!.socket.inetAddress.toString().replace("/", "")
                                for (b in configs.banTrust) {
                                    if (b.asString() == remoteip) {
                                        ser.os.writeBytes(Main.tool.encrypt(answer.toString(), ser.spec).trimIndent())
                                        ser.os.flush()
                                        Log.server("server.data-sented", ser.socket.inetAddress.toString())
                                    }
                                }
                            }
                        }
                        Request.Chat -> {
                            val message = data["message"].asString()
                            for (p in Vars.playerGroup) {
                                p.sendMessage(if (p.isAdmin) "[#C77E36][$ip][RC] $message" else "[#C77E36][RC] $message")
                            }
                            for (ser in list) {
                                if (ser!!.spec !== spec) {
                                    ser!!.os.writeBytes(Main.tool.encrypt(value, ser.spec).trimIndent())
                                    ser.os.flush()
                                }
                            }
                            Log.server("server-message-received", ip, message)
                        }
                        Request.Exit -> {
                            shutdown("client.disconnected", ip, bundle["client.disconnected.reason.exit"])
                            interrupt()
                            return
                        }
                        Request.UnbanIP -> Vars.netServer.admins.unbanPlayerIP(data["ip"].asString())
                        Request.UnbanID -> Vars.netServer.admins.unbanPlayerID(data["uuid"].asString())
                        Request.DataShare -> {
                        }
                        Request.CheckBan -> {
                            var found = false
                            val uuid = data["target_uuid"].asString()
                            val ip = data["target_ip"].asString()
                            for (info in Vars.netServer.admins.banned) {
                                if (info.id == uuid) {
                                    found = true
                                    break
                                }
                            }
                            for (info in Vars.netServer.admins.bannedIPs) {
                                if (info == ip) {
                                    found = true
                                    break
                                }
                            }
                            answer.add("result", if (found) "true" else "false")
                            os.writeBytes(Main.tool.encrypt(answer.toString(), spec).trimIndent())
                            os.flush()
                        }
                    }
                }
                shutdown(null)
            } catch (e: IOException) {
                if (e.message != "Stream closed") CrashReport(e)
            } catch (e: Exception) {
                Log.server("client.disconnected", ip, bundle["client.disconnected.reason.error"])
            }
        }

        private fun query(): String {
            val result = JsonObject()
            result.add("players", Vars.playerGroup.size()) // 플레이어 인원
            result.add("version", Version.build) // 버전
            result.add("plugin-version", pluginVars.pluginVersion)
            result.add("playtime", Main.tool.longToTime(pluginVars.playtime))
            result.add("name", Core.settings.getString("servername"))
            result.add("mapname", Vars.world.map.name())
            result.add("wave", Vars.state.wave)
            result.add("enemy-count", Vars.state.enemies)
            var online = false
            for (p in Vars.playerGroup.all()) {
                if (p.isAdmin) {
                    online = true
                    break
                }
            }
            result.add("admin_online", online)
            val array = JsonArray()
            for (p in Vars.playerGroup.all()) {
                array.add(p.name) // player list
            }
            result.add("playerlist", array)
            val items = JsonObject()
            for (item in Vars.content.items()) {
                if (item.type == ItemType.material) {
                    items.add(item.name, Vars.state.teams[Team.sharded].cores.first().items[item]) // resources
                }
            }
            result.add("resource", items)
            val rank = JsonObject()
            val list = arrayOf("placecount", "breakcount", "killcount", "joincount", "kickcount", "exp", "playtime", "pvpwincount", "reactorcount")
            for (s in list) {
                val sql = "SELECT $s,name FROM players ORDER BY `$s`"
                try {
                    playerCore.conn.prepareStatement(sql).use { pstmt ->
                        pstmt.executeQuery().use { rs ->
                            while (rs.next()) {
                                rank.add(rs.getString("name"), rs.getString(s))
                            }
                        }
                    }
                } catch (e: SQLException) {
                    CrashReport(e)
                }
            }
            return result.toString()
        }

        private fun serverinfo(): String {
            return if (Vars.state.`is`(GameState.State.playing)) {
                val playercount = Vars.playerGroup.size()
                val playerdata = StringBuilder()
                for (p in Vars.playerGroup.all()) {
                    playerdata.append(p.name).append(",")
                }
                if (playerdata.isNotEmpty()) {
                    playerdata.substring(playerdata.length - 1, playerdata.length)
                }
                val version = Version.build
                val description = Core.settings.getString("servername")
                val worldtime = Main.tool.longToTime(pluginVars.playtime)
                val serveruptime = Main.tool.longToTime(pluginVars.uptime)
                val items = StringBuilder()
                for (item in Vars.content.items()) {
                    if (item.type == ItemType.material) {
                        items.append(item.name).append(": ").append(Vars.state.teams[Team.sharded].cores.first().items[item]).append("<br>")
                    }
                }
                val coreitem = items.toString()
                "Player count: " + playercount + "<br>" +
                        "Player list: " + playerdata + "<br>" +
                        "Version: " + version + "<br>" +
                        "Description: " + description + "<br>" +
                        "World playtime: " + worldtime + "<br>" +
                        "Server uptime: " + serveruptime + "<br>" +
                        "Core items<br>" + coreitem
            } else {
                "Server isn't hosted!"
            }
        }

        @Throws(IOException::class)
        private fun rankingdata(): String {
            val lists = arrayOf("placecount", "breakcount", "killcount", "joincount", "kickcount", "exp", "playtime", "pvpwincount", "reactorcount", "attackclear")
            val results = JsonObject()
            val language = Main.tool.getGeo(ip)
            val sql = arrayOfNulls<String>(10)
            sql[0] = "SELECT * FROM players ORDER BY `placecount` DESC LIMIT 10"
            sql[1] = "SELECT * FROM players ORDER BY `breakcount` DESC LIMIT 10"
            sql[2] = "SELECT * FROM players ORDER BY `killcount` DESC LIMIT 10"
            sql[3] = "SELECT * FROM players ORDER BY `joincount` DESC LIMIT 10"
            sql[4] = "SELECT * FROM players ORDER BY `kickcount` DESC LIMIT 10"
            sql[5] = "SELECT * FROM players ORDER BY `exp` DESC LIMIT 10"
            sql[6] = "SELECT * FROM players ORDER BY `playtime` DESC LIMIT 10"
            sql[7] = "SELECT * FROM players ORDER BY `pvpwincount` DESC LIMIT 10"
            sql[8] = "SELECT * FROM players ORDER BY `reactorcount` DESC LIMIT 10"
            sql[9] = "SELECT * FROM players ORDER BY `attackclear` DESC LIMIT 10"
            val bundle = Bundle(language)
            val name = bundle["server.http.rank.name"]
            val country = bundle["server.http.rank.country"]
            val win = bundle["server.http.rank.pvp-win"]
            val lose = bundle["server.http.rank.pvp-lose"]
            val rate = bundle["server.http.rank.pvp-rate"]
            var stmt: Statement? = null
            var rs: ResultSet? = null
            try {
                stmt = playerCore.conn.createStatement()
                for (a in sql.indices) {
                    rs = stmt.executeQuery(sql[a])
                    val array = JsonArray()
                    if (lists[a] == "pvpwincount") {
                        val header = "<tr><th>$name</th><th>$country</th><th>$win</th><th>$lose</th><th>$rate</th></tr>"
                        array.add(header)
                        while (rs.next()) {
                            val percent: Int = try {
                                rs.getInt("pvpwincount") / rs.getInt("pvplosecount") * 100
                            } catch (e: Exception) {
                                0
                            }
                            val data = """
                                <tr><td>${rs.getString("name")}</td><td>${rs.getString("country")}</td><td>${rs.getInt("pvpwincount")}</td><td>${rs.getInt("pvplosecount")}</td><td>$percent%</td></tr>
                                
                                """.trimIndent()
                            array.add(data)
                        }
                    } else {
                        val header = "<tr><th>" + name + "</th><th>" + country + "</th><th>" + lists[a] + "</th></tr>"
                        array.add(header)
                        while (rs.next()) {
                            val data = """<tr><td>${rs.getString("name")}</td><td>${rs.getString("country")}</td><td>${rs.getString(lists[a])}</td></tr>""".trimIndent()
                            array.add(data)
                        }
                    }
                    results.add(lists[a], array)
                }
            } catch (e: SQLException) {
                CrashReport(e)
            } finally {
                if (stmt != null) try {
                    stmt.close()
                } catch (ignored: SQLException) {
                }
                if (rs != null) try {
                    rs.close()
                } catch (ignored: SQLException) {
                }
            }
            val reader = javaClass.getResourceAsStream("/HTML/rank.html")
            val br = BufferedReader(InputStreamReader(reader, StandardCharsets.UTF_8))
            var line: String?
            val result = StringBuilder()
            while (br.readLine().also { line = it } != null) {
                result.append(line).append("\n")
            }
            val doc = Jsoup.parse(result.toString())
            for (s in lists) {
                for (b in 0 until results[s].asArray().size()) {
                    doc.getElementById(s).append(results[s].asArray()[b].asString())
                }
            }
            doc.getElementById("info_body").appendText(serverinfo())
            doc.getElementById("rank-placecount").appendText(bundle["server.http.rank.placecount"])
            doc.getElementById("rank-breakcount").appendText(bundle["server.http.rank.breakcount"])
            doc.getElementById("rank-killcount").appendText(bundle["server.http.rank.killcount"])
            doc.getElementById("rank-joincount").appendText(bundle["server.http.rank.joincount"])
            doc.getElementById("rank-kickcount").appendText(bundle["server.http.rank.kickcount"])
            doc.getElementById("rank-exp").appendText(bundle["server.http.rank.exp"])
            doc.getElementById("rank-playtime").appendText(bundle["server.http.rank.playtime"])
            doc.getElementById("rank-pvpwincount").appendText(bundle["server.http.rank.pvpcount"])
            doc.getElementById("rank-reactorcount").appendText(bundle["server.http.rank.reactorcount"])
            doc.getElementById("rank-attackclear").appendText(bundle["server.http.rank.attackclear"])
            return doc.toString()
        }

        private fun httpserver(receive: String, payload: String) {
            val now = LocalDateTime.now()
            val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd a HH:mm:ss", Locale.ENGLISH)
            val time = now.format(dateTimeFormatter)
            try {
                BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)).use { bw ->
                    if (receive.matches(Regex("GET / HTTP/.*"))) {
                        val data = query()
                        bw.write("HTTP/1.1 200 OK\r\n")
                        bw.write("Date: $time\r\n")
                        bw.write("""Server: Mindustry/Essentials ${pluginVars.pluginVersion}""".trimIndent())
                        bw.write("Content-Type: application/json; charset=utf-8\r\n")
                        bw.write("""Content-Length: ${data.toByteArray().size + 1}""".trimIndent())
                        bw.write("\r\n")
                        bw.flush()
                        bw.write(query())
                        bw.flush()
                        Log.info("Web request :$receive")
                    } else if (receive.matches(Regex("GET /rank HTTP/.*")) || receive.matches(Regex("GET /rank# HTTP/.*"))) {
                        val rank = rankingdata()
                        bw.write("HTTP/1.1 200 OK\r\n")
                        bw.write("Date: $time\r\n")
                        bw.write("""Server: Mindustry/Essentials ${pluginVars.pluginVersion}""".trimIndent())
                        bw.write("Content-Type: text/html; charset=utf-8\r\n")
                        bw.write("""Content-Length: ${rank.toByteArray().size + 1}""".trimIndent())
                        bw.write("\r\n")
                        bw.flush()
                        bw.write(rank)
                        bw.flush()
                        Log.info("Web request :$receive")
                    } else if (receive.matches(Regex("POST /rank HTTP/.*"))) {
                        val value = payload.split(";").toTypedArray()
                        val id = value[0].replace("id=", "")
                        val pw = value[1].replace("pw=", "")
                        try {
                            playerCore.conn.prepareStatement("SELECT * FROM players WHERE accountid = ?").use { pstm ->
                                pstm.setString(1, id)
                                pstm.executeQuery().use { rs ->
                                    if (rs.next()) {
                                        val accountpw = rs.getString("accountpw")
                                        if (pw == accountpw || BCrypt.checkpw(pw, accountpw)) {
                                            val db = playerCore.load(rs.getString("uuid"), id)
                                            val ranking = arrayOfNulls<String>(12)
                                            ranking[0] = "SELECT uuid, placecount, RANK() over (ORDER BY placecount desc) valrank FROM players"
                                            ranking[1] = "SELECT uuid, breakcount, RANK() over (ORDER BY breakcount desc) valrank FROM players"
                                            ranking[2] = "SELECT uuid, killcount, RANK() over (ORDER BY killcount desc) valrank FROM players"
                                            ranking[3] = "SELECT uuid, deathcount, RANK() over (ORDER BY deathcount desc) valrank FROM players"
                                            ranking[4] = "SELECT uuid, joincount, RANK() over (ORDER BY joincount desc) valrank FROM players"
                                            ranking[5] = "SELECT uuid, kickcount, RANK() over (ORDER BY kickcount desc) valrank FROM players"
                                            ranking[6] = "SELECT uuid, level, RANK() over (ORDER BY level desc) valrank FROM players"
                                            ranking[7] = "SELECT uuid, playtime, RANK() over (ORDER BY playtime desc) valrank FROM players"
                                            ranking[8] = "SELECT uuid, attackclear, RANK() over (ORDER BY attackclear desc) valrank FROM players"
                                            ranking[9] = "SELECT uuid, pvpwincount, RANK() over (ORDER BY pvpwincount desc) valrank FROM players"
                                            ranking[10] = "SELECT uuid, pvplosecount, RANK() over (ORDER BY pvplosecount desc) valrank FROM players"
                                            ranking[11] = "SELECT uuid, pvpbreakout, RANK() over (ORDER BY pvpbreakout desc) valrank FROM players"
                                            val datatext: String
                                            val array = Array<String>()
                                            for (s in ranking) {
                                                try {
                                                    playerCore.conn.prepareStatement(s).use { pstmt ->
                                                        pstmt.executeQuery().use { rs1 ->
                                                            while (rs1.next()) {
                                                                if (rs1.getString("uuid") == db.uuid) {
                                                                    array.add(rs1.getString("valrank"))
                                                                    break
                                                                }
                                                            }
                                                        }
                                                    }
                                                } catch (e: SQLException) {
                                                    CrashReport(e)
                                                }
                                            }
                                            datatext = bundle["player.info"] + "<br>" +
                                                    "========================================<br>" +
                                                    bundle["player.name"] + ": " + rs.getString("name") + "<br>" +
                                                    bundle["player.uuid"] + ": " + rs.getString("uuid") + "<br>" +
                                                    bundle["player.country"] + ": " + db.country + "<br>" +
                                                    bundle["player.placecount"] + ": " + db.placecount + " - <b>#" + array[0] + "</b><br>" +
                                                    bundle["player.breakcount"] + ": " + db.breakcount + " - <b>#" + array[1] + "</b><br>" +
                                                    bundle["player.killcount"] + ": " + db.killcount + " - <b>#" + array[2] + "</b><br>" +
                                                    bundle["player.deathcount"] + ": " + db.deathcount + " - <b>#" + array[3] + "</b><br>" +
                                                    bundle["player.joincount"] + ": " + db.joincount + " - <b>#" + array[4] + "</b><br>" +
                                                    bundle["player.kickcount"] + ": " + db.kickcount + " - <b>#" + array[5] + "</b><br>" +
                                                    bundle["player.level"] + ": " + db.level + " - <b>#" + array[6] + "</b><br>" +
                                                    bundle["player.firstdate"] + ": " + db.firstdate + "<br>" +
                                                    bundle["player.lastdate"] + ": " + db.lastdate + "<br>" +
                                                    bundle["player.playtime"] + ": " + Main.tool.longToTime(db.playtime) + " - <b>#" + array[7] + "</b><br>" +
                                                    bundle["player.attackclear"] + ": " + db.attackclear + " - <b>#" + array[8] + "</b><br>" +
                                                    bundle["player.pvpwincount"] + ": " + db.pvpwincount + " - <b>#" + array[9] + "</b><br>" +
                                                    bundle["player.pvplosecount"] + ": " + db.pvplosecount + " - <b>#" + array[10] + "</b><br>" +
                                                    bundle["player.pvpbreakout"] + ": " + db.pvpbreakout + " - <b>#" + array[11] + "</b><br>"
                                            bw.write("HTTP/1.1 200 OK\r\n")
                                            bw.write("Date: $time\r\n")
                                            bw.write("""Server: Mindustry/Essentials ${pluginVars.pluginVersion}""".trimIndent())
                                            bw.write("Content-Type: text/html; charset=utf-8\r\n")
                                            bw.write("""Content-Length: ${datatext.toByteArray().size + 1}""".trimIndent())
                                            bw.write("\r\n")
                                            bw.flush()
                                            bw.write(datatext)
                                            bw.flush()
                                        } else {
                                            bw.write("Login failed!")
                                            bw.flush()
                                        }
                                    } else {
                                        bw.write("Login failed!")
                                        bw.flush()
                                    }
                                }
                            }
                        } catch (e: SQLException) {
                            CrashReport(e)
                        }
                    } else {
                        val reader = javaClass.getResourceAsStream("/HTML/404.html")
                        val br = BufferedReader(InputStreamReader(reader, StandardCharsets.UTF_8))
                        var line: String?
                        val result = StringBuilder()
                        while (br.readLine().also { line = it } != null) {
                            result.append(line).append("\n")
                        }
                        val rand = SecureRandom().nextBoolean()
                        val image: InputStream
                        image = if (rand) {
                            javaClass.getResourceAsStream("/HTML/404_Error.gif")
                        } else {
                            javaClass.getResourceAsStream("/HTML/404.webp")
                        }
                        val byteOutStream = ByteArrayOutputStream()
                        var len: Int
                        val buf = ByteArray(1024)
                        while (image.read(buf).also { len = it } != -1) {
                            byteOutStream.write(buf, 0, len)
                        }
                        val fileArray = byteOutStream.toByteArray()
                        val changeString: String
                        val encoder = Base64.getEncoder()
                        changeString = if (rand) {
                            "data:image/gif;base64," + encoder.encodeToString(fileArray)
                        } else {
                            "data:image/webp;base64," + encoder.encodeToString(fileArray)
                        }
                        val doc = Jsoup.parse(result.toString())
                        doc.getElementById("box").append("<img src=$changeString alt=\"\">")
                        bw.write("HTTP/1.1 404 Internal error\r\n")
                        bw.write("Date: $time\r\n")
                        bw.write("Server: Mindustry/Essentials 7.0\r\n")
                        bw.write("\r\n")
                        bw.flush()
                        bw.write(doc.toString())
                        bw.flush()
                        Log.info("Web request 404 :$receive")
                    }
                }
            } catch (e: IOException) {
                CrashReport(e)
            } finally {
                shutdown("client.disconnected.http", ip)
            }
        }

        init {
            ip = socket.inetAddress.toString()
            br = BufferedReader(InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))
            os = DataOutputStream(socket.getOutputStream())

            // 키 값 읽기
            val authkey = br.readLine() ?: throw PluginException("Auth key is null")
            if (Pattern.matches(".*HTTP/.*", authkey)) {
                val headers = StringBuilder()
                headers.append(authkey).append("\n")
                headers.append("Remote IP: ").append(ip).append("\n")
                var headerLine: String?
                while (br.readLine().also { headerLine = it }.isNotEmpty()) {
                    headers.append(headerLine).append("\n")
                }
                headers.append("========================")
                Log.write(LogType.web, headers.toString())
                val payload = StringBuilder()
                while (br.ready()) {
                    payload.append(br.read().toChar())
                }
                if (authkey.matches(Regex("POST /rank HTTP/.*")) && payload.toString().split(";").toTypedArray().size != 2) {
                    try {
                        BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)).use { bw ->
                            bw.write("Login failed!\n")
                            bw.flush()
                        }
                    } finally {
                        shutdown("client.disconnected.http", ip)
                    }
                } else if (configs.query) {
                    httpserver(authkey, payload.toString())
                } else if (!configs.query) {
                    try {
                        BufferedWriter(OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8)).use { bw ->
                            bw.write("HTTP/1.1 403 Forbidden\r\n")
                            bw.write("""Date: ${Main.tool.getLocalTime()}""".trimIndent())
                            bw.write("""Server: Mindustry/Essentials ${pluginVars.pluginVersion}""".trimIndent())
                            bw.write("Content-Encoding: gzip")
                            bw.write("\r\n")
                            bw.write("<TITLE>403 Forbidden</TITLE>")
                            bw.write("<p>This server isn't allowed query!</p>")
                        }
                    } finally {
                        shutdown("client.disconnected.http", ip)
                    }
                }
            } else {
                spec = SecretKeySpec(Base64.getDecoder().decode(authkey), "AES")
            }
        }
    }
}