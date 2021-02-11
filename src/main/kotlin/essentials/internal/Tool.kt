package essentials.internal

import arc.Core
import arc.struct.ObjectMap
import arc.struct.Seq
import com.ip2location.IP2Location
import com.neovisionaries.i18n.CountryCode
import essentials.Main.Companion.pluginRoot
import essentials.PluginData
import essentials.data.Config
import mindustry.Vars.*
import mindustry.content.Blocks
import mindustry.game.Team
import mindustry.gen.Call
import mindustry.gen.Groups
import mindustry.gen.Nulls
import mindustry.gen.Playerc
import mindustry.type.UnitType
import mindustry.world.Block
import mindustry.world.Tile
import mindustry.world.blocks.logic.MessageBlock
import java.io.*
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow


object Tool {
    val ipre = IP2Location()

    fun hostIP(): String {
        val ip = URL("http://checkip.amazonaws.com")
        val br = BufferedReader(InputStreamReader(ip.openStream()))

        br.use {
            return try {
                br.readLine()
            } catch (e: Exception) {
                "127.0.0.1"
            }
        }
    }

    fun longToDateTime(mils: Long): LocalDateTime {
        return Timestamp(mils).toLocalDateTime()
        //return LocalDateTime.ofInstant(Instant.ofEpochMilli(mils), TimeZone.getDefault().toZoneId())
    }

    fun longToTime(seconds: Long): String {
        val min = seconds / 60
        val hour = min / 60
        val days = hour / 24
        return String.format("%d:%02d:%02d:%02d",
                days % 365, hour % 24, min % 60, seconds % 60)
    }

    fun textToLocale(s: String): Locale {
        var locale = Locale(s)
        var lc = s
        if (s.contains(",")) lc = lc.split(",").toTypedArray()[0]
        if (lc.split("_").toTypedArray().size > 1) {
            val array = lc.split("_").toTypedArray()
            locale = Locale(array[0], array[1])
        }
        return locale
    }

    fun getMotd(l: Locale): String {
        return if (pluginRoot.child("motd/$l.txt").exists()) {
            pluginRoot.child("motd/$l.txt").readString()
        } else {
            val file = pluginRoot.child("motd/" + Config.locale.toString() + ".txt")
            if (file.exists()) file.readString() else ""
        }
    }

    fun getLocalTime(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
    }

    fun encrypt(privateString: String, key: SecretKey): String {
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val ivSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec)
        val text = cipher.doFinal(privateString.toByteArray(StandardCharsets.UTF_8))
        val encrypted = ByteArray(iv.size + text.size)
        System.arraycopy(iv, 0, encrypted, 0, iv.size)
        System.arraycopy(text, 0, encrypted, iv.size, text.size)
        return Base64.getEncoder().encodeToString(encrypted)
    }

    fun decrypt(encrypted: String?, key: SecretKey): String {
        val decoded = Base64.getDecoder().decode(encrypted)
        val iv = Arrays.copyOfRange(decoded, 0, 12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val ivSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        val text = cipher.doFinal(decoded, 12, decoded.size - 12)
        return String(text, StandardCharsets.UTF_8)
    }

    fun sendMessageAll(value: String, vararg parameter: String?) {
        Core.app.post {
            for (p in Groups.player) {
                val playerData = PluginData[p.uuid()]
                if (!playerData.isNull) p.sendMessage(Bundle(playerData.locale).prefix(value, *parameter))
            }
        }
    }

    fun getWebContent(url: String?): String? {
        try {
            Scanner(URL(url).openStream()).use { sc ->
                val sb = StringBuilder()
                while (sc.hasNext()) {
                    sb.append(sc.next())
                }
                return sb.toString()
            }
        } catch (e: IOException) {
            return null
        }
    }

    fun getGeo(data: Any): Locale {
        val ip = if (data is Playerc) netServer.admins.getInfo(data.uuid()).lastIP else (data as String?)!!

        val res = ipre.IPQuery(ip)
        val code = CountryCode.getByCode(res.countryShort)

        return if (code == null) Config.locale else code.toLocale()
    }

    fun download(url: URL, savepath: File) {
        try {
            BufferedOutputStream(FileOutputStream(savepath)).use { outputStream ->
                val urlConnection = url.openConnection()
                val br = urlConnection.getInputStream()
                val size = urlConnection.contentLength
                val buf = ByteArray(5120)
                var byteRead: Int
                var byteWritten = 0
                val startTime = System.currentTimeMillis()
                while (br.read(buf).also { byteRead = it } != -1) {
                    outputStream.write(buf, 0, byteRead)
                    byteWritten += byteRead
                    printProgress(startTime, size, byteWritten)
                }
                br.close()
            }
        } catch (e: Exception) {
            CrashReport(e)
        }
    }

    private fun printProgress(startTime: Long, total: Int, remain: Int) {
        val eta = if (remain == 0) 0 else (total - remain) * (System.currentTimeMillis() - startTime) / remain
        val etaHms = String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
                TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
                TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1))
        require(remain <= total)
        val maxBareSize = 20
        val remainPercent = 20 * remain / total
        val defaultChar = '-'
        val icon = "*"
        val bare = String(CharArray(maxBareSize)).replace('\u0000', defaultChar) + "]"
        val bareDone = StringBuilder()
        bareDone.append("[")
        for (i in 0 until remainPercent) {
            bareDone.append(icon)
        }
        val bareRemain = bare.substring(remainPercent)
        print("\r" + humanReadableByteCount(remain, true) + "/" + humanReadableByteCount(total, true) + "\t" + bareDone + bareRemain + " " + remainPercent * 5 + "%, ETA: " + etaHms)
        if (remain == total) {
            print("\n")
        }
    }

    // Source: https://programming.guide/worlds-most-copied-so-snippet.html
    private fun humanReadableByteCount(byte: Int, si: Boolean): String {
        var bytes = byte
        val unit = if (si) 1000 else 1024
        val absBytes = abs(bytes).toLong()
        if (absBytes < unit) return "$bytes B"
        var exp = (ln(absBytes.toDouble()) / ln(unit.toDouble())).toInt()
        val th = (unit.toDouble().pow(exp.toDouble()) * (unit - 0.05)).toLong()
        if (exp < 6 && absBytes >= th - (if (th and 0xfff == 0xd00L) 52 else 0)) exp++
        val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1].toString() + if (si) "" else "i"
        if (exp > 4) {
            bytes /= unit
            exp -= 1
        }
        return String.format("%.1f %sB", bytes / unit.toDouble().pow(exp.toDouble()), pre)
    }

    fun getUnitByName(name: String): UnitType? {
        return content.units().find { unitType: UnitType -> unitType.name == name }
    }

    fun findPlayer(name: String): Playerc? {
        return Groups.player.find { p: Playerc -> p.name() == name }
    }

    fun getTeamByName(name: String): Team? {
        for (t in Team.all) {
            if (t.name == name) {
                return t
            }
        }
        return null
    }

    fun setTileText(tile: Tile, block: Block, text: String) {
        var t = tile
        val letters = ObjectMap<String, IntArray>()
        letters.put("A", intArrayOf(0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1))
        letters.put("B", intArrayOf(1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0))
        letters.put("C", intArrayOf(0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1))
        letters.put("D", intArrayOf(1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0))
        letters.put("E", intArrayOf(1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1))
        letters.put("F", intArrayOf(1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0))
        letters.put("G", intArrayOf(0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0, 1, 1, 1))
        letters.put("H", intArrayOf(1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1))
        letters.put("I", intArrayOf(1, 0, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1))
        letters.put("J", intArrayOf(1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0))
        letters.put("K", intArrayOf(1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1))
        letters.put("L", intArrayOf(1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1))
        letters.put("M", intArrayOf(1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 1, 1, 1, 1))
        letters.put("N", intArrayOf(1, 1, 1, 1, 1, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1, 1, 1))
        letters.put("O", intArrayOf(0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0))
        letters.put("P", intArrayOf(1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0))
        letters.put("Q", intArrayOf(0, 1, 1, 1, 0, 1, 0, 0, 0, 1, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0, 0, 0, 0, 1))
        letters.put("R", intArrayOf(1, 1, 1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 1, 0, 0, 0, 1, 0, 1, 1))
        letters.put("S", intArrayOf(1, 1, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 0, 1, 1, 0, 1, 1, 1))
        letters.put("T", intArrayOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0))
        letters.put("U", intArrayOf(1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0))
        letters.put("V", intArrayOf(1, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 1, 1, 0, 0))
        letters.put("W", intArrayOf(1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0))
        letters.put("X", intArrayOf(1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0, 0, 1))
        letters.put("Y", intArrayOf(1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1, 1, 0, 0, 0))
        letters.put("Z", intArrayOf(1, 0, 0, 1, 1, 1, 0, 1, 0, 1, 1, 1, 0, 0, 1))
        letters.put("0", intArrayOf(1, 1, 1, 1, 0, 1, 1, 0, 1, 1, 0, 1, 1, 1, 1))
        letters.put("1", intArrayOf(0, 1, 0, 1, 1, 0, 0, 1, 0, 0, 1, 0, 1, 1, 1))
        letters.put("2", intArrayOf(1, 1, 1, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 1))
        letters.put("3", intArrayOf(1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1))
        letters.put("4", intArrayOf(1, 0, 1, 1, 0, 1, 1, 1, 1, 0, 0, 1, 0, 0, 1))
        letters.put("5", intArrayOf(1, 1, 1, 1, 0, 0, 1, 1, 1, 0, 0, 1, 1, 1, 1))
        letters.put("6", intArrayOf(1, 1, 1, 1, 0, 0, 1, 1, 1, 1, 0, 1, 1, 1, 1))
        letters.put("7", intArrayOf(1, 1, 1, 1, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1))
        letters.put("8", intArrayOf(1, 1, 1, 1, 0, 1, 1, 1, 1, 1, 0, 1, 1, 1, 1))
        letters.put("9", intArrayOf(1, 1, 1, 1, 0, 1, 1, 1, 1, 0, 0, 1, 1, 1, 1))
        letters.put("!", intArrayOf(1, 1, 1, 1, 0, 1))
        letters.put("?", intArrayOf(0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0, 1, 0, 0, 0, 0))
        letters.put(" ", intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
        val texts = text.toCharArray()
        for (i in texts) {
            val pos = Seq<IntArray>()
            val target = letters[i.toUpperCase().toString()]
            var xv = 0
            var yv = 0
            when (target.size) {
                20 -> {
                    xv = 5
                    yv = 4
                }
                15 -> {
                    xv = 5
                    yv = 3
                }
                18 -> {
                    xv = 6
                    yv = 3
                }
                25 -> {
                    xv = 5
                    yv = 5
                }
                6 -> {
                    xv = 6
                    yv = 1
                }
                10 -> {
                    xv = 2
                    yv = 5
                }
            }
            for (y in 0 until yv) {
                for (x in 0 until xv) {
                    pos.add(intArrayOf(y, -x))
                }
            }
            for (a in 0 until pos.size) {
                val tar = world.tile(t.x + pos[a][0], t.y + pos[a][1])
                if (target[a] == 1) {
                    Call.constructFinish(tar, block, Nulls.unit, 0.toByte(), Team.sharded, false)
                } else if (tar != null) {
                    Call.deconstructFinish(tar, Blocks.air, Nulls.unit)
                }
            }
            t = world.tile(t.x + (xv + 1), t.y.toInt())
        }
    }

    fun checkPassword(player: Playerc, id: String?, password: String, password_repeat: String): Boolean {
        // 영문(소문자), 숫자, 7~20자리
        var pwPattern = "^(?=.*\\d)(?=.*[a-z]).{7,20}$"
        val matcher = Pattern.compile(pwPattern).matcher(password)

        // 같은 문자 4개이상 사용 불가
        pwPattern = "(.)\\1\\1\\1"
        val matcher2 = Pattern.compile(pwPattern).matcher(password)
        if (password != password_repeat) {
            // 비밀번호가 비밀번호 재확인 문자열과 똑같지 않을경우
            /*player.sendMessage("""
    [green][Essentials] [sky]The password isn't the same.
    [green][Essentials] [sky]비밀번호가 똑같지 않습니다.
    """.trimIndent())*/
            return false
        } else if (!matcher.matches()) {
            // 정규식에 맞지 않을경우
            /*player.sendMessage("""
    [green][Essentials] [sky]The password should be 7 ~ 20 letters long and contain alphanumeric characters and special characters!
    [green][Essentials] [sky]비밀번호는 7~20자 내외로 설정해야 하며, 영문과 숫자를 포함해야 합니다!
    """.trimIndent())*/
            Log.player("system.password.match.regex", player.name())
            return false
        } else if (matcher2.find()) {
            // 비밀번호에 ID에 사용된 같은 문자가 4개 이상일경우
            /*player.sendMessage("""
    [green][Essentials] [sky]Passwords should not be similar to nicknames!
    [green][Essentials] [sky]비밀번호는 닉네임과 비슷하면 안됩니다!
    """.trimIndent())*/
            Log.player("system.password.match.name", player.name())
            return false
        } else if (password.contains(id!!)) {
            // 비밀번호와 ID가 완전히 같은경우
            /*player.sendMessage("""
    [green][Essentials] [sky]Password shouldn't be the same as your nickname.
    [green][Essentials] [sky]비밀번호는 ID와 비슷하게 설정할 수 없습니다!
    """.trimIndent())*/
            return false
        } else if (password.contains(" ")) {
            // 비밀번호에 공백이 있을경우
            /*player.sendMessage("""
    [green][Essentials] [sky]Password must not contain spaces!
    [green][Essentials] [sky]비밀번호에는 공백이 있으면 안됩니다!
    """.trimIndent())*/
            Log.player("system.password.match.blank", player.name())
            return false
        } else if (password.matches(Regex("<(.*?)>"))) {
            // 비밀번호 형식이 "<비밀번호>" 일경우
            /*player.sendMessage("""
    [green][Essentials] [green]<[sky]password[green]>[sky] format isn't allowed!
    [green][Essentials] [sky]Use /register password
    [green][Essentials] [green]<[sky]비밀번호[green]>[sky] 형식은 허용되지 않습니다!
    [green][Essentials] [sky]/register password 형식으로 사용하세요.
    """.trimIndent())*/
            Log.player("system.password.match.invalid", player.name())
            return false
        }
        return true
    }

    fun setMessage(tile: Tile, message: String) {
        Core.app.post {
            if (tile.block() is MessageBlock) {
                (tile.block() as MessageBlock).MessageBuild().configure(message)
            }
        }
    }
}