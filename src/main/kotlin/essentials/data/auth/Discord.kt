package essentials.data.auth

import arc.struct.ObjectMap
import arc.util.async.Threads
import com.mewna.catnip.Catnip
import com.mewna.catnip.entity.message.Message
import com.mewna.catnip.shard.DiscordEvent
import essentials.PlayerData
import essentials.PluginData
import essentials.PluginData.banned
import essentials.data.Config
import essentials.data.DB
import essentials.data.PlayerCore
import essentials.eof.sendMessage
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Log
import mindustry.gen.Groups
import mindustry.gen.Playerc
import org.hjson.JsonObject
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import java.util.regex.Pattern
import javax.security.auth.login.LoginException
import kotlin.system.exitProcess

object Discord  {
    val pin: ObjectMap<String, Int> = ObjectMap()
    var catnip: Catnip

    init {
        if(Config.discordBotToken.isNotEmpty() && Config.discordChannelToken.isNotEmpty()) {
            catnip = Catnip.catnip(Config.discordBotToken)
        } else {
            exitProcess(0)
        }
    }

    fun start() { // TODO discord 방식 변경
        catnip.observable(DiscordEvent.MESSAGE_CREATE).subscribe({
            if(it.channelIdAsLong().toString() == Config.discordChannelToken && !it.author().bot()) {
                with(it.content()) {
                    for(a in banned) {
                        if(a.json.has("discord")) {
                            if(it.author().idAsLong() == a.json.get("discord").asObject().get("id").asLong()) {
                                it.guild().blockingGet().ban(it.author().idAsLong(), "차단된 계정", 0)
                            }
                        }
                    }

                    when {
                        equals("!ping", true) -> {
                            val start = java.lang.System.currentTimeMillis()
                            it.reply("pong!", true).subscribe { ping: Message ->
                                val end = java.lang.System.currentTimeMillis()
                                ping.edit("pong! (" + (end - start) + "ms 소요됨).")
                                arc.util.async.Threads.sleep(5000)
                                ping.delete()
                            }
                        }
                        equals("!help", true) -> {
                            val message = """
                                    ``!help`` 사용 가능한 서버 봇 명령어를 확인합니다.
                                    ``!auth PIN`` 서버에 Discord 인증을 해서 서버에 특별한 효과를 추가시킵니다.
                                    ``!ping`` 그냥 봇 작동 확인하는 용도입니다.
                                """.trimIndent()
                            it.reply(message, true).subscribe { m: Message ->
                                arc.util.async.Threads.sleep(7000)
                                m.delete()
                            }
                        }
                        startsWith("!auth", true) -> {
                            if(Config.authType == Config.AuthType.Discord) {
                                val arg = it.content().replace("!auth ", "").split(" ")
                                if(arg.size == 1) {
                                    try {
                                        val buffer = PlayerCore.getAllData()
                                        var isMatch = false

                                        for(a in buffer) {
                                            if(a.json.has("discord")) {
                                                if(a.json.get("discord").asObject().get("id").asLong() == it.author().idAsLong()) {
                                                    isMatch = true
                                                }
                                            }
                                        }

                                        if(!isMatch) {
                                            var data: PlayerData? = null
                                            for(a in pin) {
                                                if(a.value.asLong() == arg[0].toLong()) {
                                                    data = PluginData[a.name]
                                                }
                                            }

                                            if(data != null) {
                                                val info = JsonObject()
                                                info.add("name", it.author().username())
                                                info.add("id", it.author().idAsLong())
                                                info.add("isAuthorized", true)

                                                data.json.add("discord", info)
                                                it.reply("성공! 서버 재접속 후에 효과가 발동됩니다.", true).subscribe { m: Message ->
                                                    arc.util.async.Threads.sleep(5000)
                                                    m.delete()
                                                }
                                                PluginData[pin.remove(arg[0]).asString()]
                                            } else {
                                                it.reply("등록되지 않은 계정입니다!", true).subscribe { m: Message ->
                                                    arc.util.async.Threads.sleep(5000)
                                                    m.delete()
                                                }
                                            }
                                        } else {
                                            it.reply("이미 등록된 계정입니다!", true).subscribe { m: Message ->
                                                arc.util.async.Threads.sleep(5000)
                                                m.delete()
                                            }
                                        }
                                    } catch(e: Exception) {
                                        it.reply("올바른 PIN 번호가 아닙니다! 사용법: ``!auth <PIN 번호>``", true).subscribe { m: Message ->
                                            arc.util.async.Threads.sleep(5000)
                                            m.delete()
                                        }
                                    }
                                } else {
                                    it.reply("사용법: ``!auth <PIN 번호>``", true).subscribe { m: Message ->
                                        arc.util.async.Threads.sleep(5000)
                                        m.delete()
                                    }
                                }
                            } else {
                                it.reply("현재 서버에 Discord 인증이 활성화 되어 있지 않습니다!", true)
                            }
                        } // Console commands
                        equals("") -> {
                        }
                        contains("!") -> {
                            it.reply("알 수 없는 명령어 입니다!", true).subscribe { m: Message ->
                                arc.util.async.Threads.sleep(5000)
                                m.delete()
                            }
                        }
                        else -> {

                        }
                    }
                }
            }
        }) { e: Throwable -> ErrorReport(e) }
    }

    fun queue(player: Playerc) {
        val pin = Random().nextInt(9999)
        pins.put(player.name(), pin)
        sendMessage(player, Bundle(PluginData[player.uuid()])["discord-pin-queue", pin.toString()])
    }

    override fun onGuildMemberJoin(e: GuildMemberJoinEvent) {
        send("Use the ``!signup <PIN>`` command to register the server.")
    }

    override fun onMessageReceived(e: MessageReceivedEvent) {
        event = e
        if(e.isFromType(ChannelType.PRIVATE) && !e.author.isBot) {
            val msg = e.message.contentRaw
            val arr = msg.split(" ").toTypedArray()
            when(arr[0]) {
                "!signup" -> if(arr.size == 3) {
                    for(data in pins.entries()) {
                        val name = data.key
                        if(data.value == arr[1].toInt()) {
                            var pw = arr[2]
                            if(checkpw(e.author.name, name, pw)) {
                                var pstmt: PreparedStatement? = null
                                var rs: ResultSet? = null
                                try {
                                    pw = BCrypt.gensalt(12, SecureRandom.getInstanceStrong())
                                    pstmt = DB.database.prepareStatement("SELECT * FROM players WHERE accountid=?")
                                    pstmt.setString(1, name)
                                    rs = pstmt.executeQuery()
                                    if(!rs.next()) {
                                        val player = Groups.player.find { p: Playerc -> p.name().equals(name, ignoreCase = true) }
                                        if(player != null) {
                                            val register = PlayerCore.register(player, player.name(), player.uuid(), name, pw)
                                            if(register) {
                                                PlayerCore.playerLoad(player, null)
                                                val playerData = PluginData[player.uuid()]
                                                sendMessage(player, Bundle(playerData))["register-success"]
                                                send(Bundle(playerData)["success"])
                                                break
                                            }
                                        } else {
                                            send("Player not found in server!")
                                        }
                                    } else {
                                        send("You're already have account!")
                                    }
                                } catch(ex: Exception) {
                                    CrashReport(ex)
                                } finally {
                                    if(pstmt != null) try {
                                        pstmt.close()
                                    } catch(ignored: SQLException) {
                                    }
                                    if(rs != null) try {
                                        rs.close()
                                    } catch(ignored: SQLException) {
                                    }
                                }
                            } else {
                                send("Check password failed.")
                            }
                        } else {
                            send("PIN number don't match!")
                        }
                    }
                } else {
                    send("Use ``!signup <PIN> <New password>``.")
                }
                "!changepw" -> {
                }
                else -> {
                    val message = """
                        >>> Command list
                        ``!signup <PIN> <New password>`` Account register to server. Nickname and password can't use blank.
                        """
                    send(message)
                }
            }
        }
    }

    private fun checkpw(username: String, id: String, pw: String): Boolean { // 영문(소문자), 숫자, 7~20자리
        var pwPattern = "^(?=.*\\d)(?=.*[a-z]).{7,20}$"
        val matcher = Pattern.compile(pwPattern).matcher(pw)

        // 같은 문자 4개이상 사용 불가
        pwPattern = "(.)\\1\\1\\1"
        val matcher2 = Pattern.compile(pwPattern).matcher(pw)
        if(!matcher.matches()) { // 정규식에 맞지 않을경우
            send("The password should be 7 ~ 20 letters long and contain alphanumeric characters and special characters!")
            Log.player("system.password.match.regex", username)
            return false
        } else if(matcher2.find()) { // 비밀번호에 ID에 사용된 같은 문자가 4개 이상일경우
            send("Passwords should not be similar to nicknames!")
            Log.player("system.password.match.name", username)
            return false
        } else if(pw.contains(id)) { // 비밀번호와 ID가 완전히 같은경우
            send("Password shouldn't be the same as your nickname.")
            return false
        } else if(pw.contains(" ")) { // 비밀번호에 공백이 있을경우
            send("Password must not contain spaces!")
            Log.player("system.password.match.blank", username)
            return false
        } else if(id.contains(" ")) {
            send("Username must not contain spaces!")
            Log.player("username-match-blank", username)
            return false
        } else if(pw.matches(Regex("<(.*?)>"))) { // 비밀번호 형식이 "<비밀번호>" 일경우
            send("<password> format isn't allowed! Use /register password")
            Log.player("system.password.match.invalid", username)
            return false
        }
        return true
    }

    private fun send(message: String?) {
        if(Discord::event.isInitialized) event.privateChannel.sendMessage(message!!).queue()
    }

    fun shutdownNow() {
        if(Discord::jda.isInitialized) jda.shutdown()
    }
}