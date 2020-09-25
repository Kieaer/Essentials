package essentials.features

import arc.struct.ObjectMap
import essentials.Config
import essentials.PlayerCore
import essentials.PluginVars
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Log
import essentials.internal.Tool
import mindustry.gen.Groups
import mindustry.gen.Playerc
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.ChannelType
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.mindrot.jbcrypt.BCrypt
import java.security.SecureRandom
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*
import java.util.regex.Pattern
import javax.security.auth.login.LoginException

object Discord : ListenerAdapter() {
    private var jda: JDA? = null
    private var event: MessageReceivedEvent? = null
    val pins: ObjectMap<String, Int> = ObjectMap()

    fun start() {
        // TODO discord 방식 변경
        try {
            jda = JDABuilder.createDefault(Config.discordToken).build()
            jda!!.awaitReady()
            jda!!.addEventListener(Discord)
            Log.info("system.discord.enabled")
        } catch (e: LoginException) {
            Log.err("system.discord.error")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }

    fun queue(player: Playerc) {
        val playerData = PlayerCore[player.uuid()]
        val bundle = Bundle(if (playerData.error) Config.locale else playerData.locale)
        val pin = Random().nextInt(9999)
        pins.put(player.name(), pin)
        player.sendMessage(bundle.prefix("discord-pin-queue", pin))
    }

    override fun onGuildMemberJoin(e: GuildMemberJoinEvent) {
        send("Use the ``!signup <PIN>`` command to register the server.")
    }

    override fun onMessageReceived(e: MessageReceivedEvent) {
        event = e
        if (e.isFromType(ChannelType.PRIVATE) && !e.author.isBot) {
            val msg = e.message.contentRaw
            val arr = msg.split(" ").toTypedArray()
            when (arr[0]) {
                "!signup" -> if (arr.size == 3) {
                    for (data in pins.entries()) {
                        val name = data.key
                        if (data.value == arr[1].toInt()) {
                            var pw = arr[2]
                            if (checkpw(e.author.name, name, pw)) {
                                var pstmt: PreparedStatement? = null
                                var rs: ResultSet? = null
                                try {
                                    pw = BCrypt.gensalt(12, SecureRandom.getInstanceStrong())
                                    pstmt = PlayerCore.conn.prepareStatement("SELECT * FROM players WHERE accountid=?")
                                    pstmt.setString(1, name)
                                    rs = pstmt.executeQuery()
                                    if (!rs.next()) {
                                        val player = Groups.player.find { p: Playerc -> p.name().equals(name, ignoreCase = true) }
                                        if (player != null) {
                                            val lc = Tool.getGeo(player)
                                            val register = PlayerCore.register(player.name, player.uuid(), lc.displayCountry, lc.toString(), lc.displayLanguage, PluginVars.serverIP, "default", e.author.idLong, name, pw, false)
                                            if (register) {
                                                PlayerCore.playerLoad(player, null)
                                                val playerData = PlayerCore[player.uuid()]
                                                player.sendMessage(Bundle(playerData.locale).prefix("register-success"))
                                                send(Bundle(playerData.locale)["success"])
                                                break
                                            }
                                        } else {
                                            send("Player not found in server!")
                                        }
                                    } else {
                                        send("You're already have account!")
                                    }
                                } catch (ex: Exception) {
                                    CrashReport(ex)
                                } finally {
                                    if (pstmt != null) try {
                                        pstmt.close()
                                    } catch (ignored: SQLException) {
                                    }
                                    if (rs != null) try {
                                        rs.close()
                                    } catch (ignored: SQLException) {
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
                        """.trimIndent()
                    send(message)
                }
            }
        }
    }

    private fun checkpw(username: String, id: String, pw: String): Boolean {
        // 영문(소문자), 숫자, 7~20자리
        var pwPattern = "^(?=.*\\d)(?=.*[a-z]).{7,20}$"
        val matcher = Pattern.compile(pwPattern).matcher(pw)

        // 같은 문자 4개이상 사용 불가
        pwPattern = "(.)\\1\\1\\1"
        val matcher2 = Pattern.compile(pwPattern).matcher(pw)
        if (!matcher.matches()) {
            // 정규식에 맞지 않을경우
            send("The password should be 7 ~ 20 letters long and contain alphanumeric characters and special characters!")
            Log.player("system.password.match.regex", username)
            return false
        } else if (matcher2.find()) {
            // 비밀번호에 ID에 사용된 같은 문자가 4개 이상일경우
            send("Passwords should not be similar to nicknames!")
            Log.player("system.password.match.name", username)
            return false
        } else if (pw.contains(id)) {
            // 비밀번호와 ID가 완전히 같은경우
            send("Password shouldn't be the same as your nickname.")
            return false
        } else if (pw.contains(" ")) {
            // 비밀번호에 공백이 있을경우
            send("Password must not contain spaces!")
            Log.player("system.password.match.blank", username)
            return false
        } else if (id.contains(" ")) {
            send("Username must not contain spaces!")
            Log.player("username-match-blank", username)
            return false
        } else if (pw.matches(Regex("<(.*?)>"))) {
            // 비밀번호 형식이 "<비밀번호>" 일경우
            send("<password> format isn't allowed! Use /register password")
            Log.player("system.password.match.invalid", username)
            return false
        }
        return true
    }

    fun send(message: String?) {
        event!!.privateChannel.sendMessage(message!!).queue()
    }

    fun shutdownNow() {
        if (jda != null) jda!!.shutdown()
    }
}