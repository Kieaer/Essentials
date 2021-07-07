package essentials.data.auth

import arc.struct.ObjectMap
import com.mewna.catnip.Catnip
import com.mewna.catnip.shard.DiscordEvent
import essentials.PluginData
import essentials.data.Config
import essentials.eof.sendMessage
import essentials.internal.Bundle
import essentials.internal.CrashReport
import essentials.internal.Log
import mindustry.gen.Playerc
import java.util.*

object Discord  {
    val pin: ObjectMap<String, Int> = ObjectMap()
    private lateinit var catnip: Catnip

    init {
        if(Config.discordBotToken.isNotEmpty() && Config.discordChannelToken.isNotEmpty()) {
            catnip = Catnip.catnip(Config.discordBotToken)
        } else {
            Log.info("discord.disabled")
        }
    }

    fun start() {
        catnip.observable(DiscordEvent.MESSAGE_CREATE).subscribe({
            if(it.channelIdAsLong().toString() == Config.discordChannelToken && !it.author().bot()) {
                // TODO discord
            }
        }) { e: Throwable -> CrashReport(e) }
    }

    fun queue(player: Playerc) {
        val number = Random().nextInt(9999)
        pin.put(player.name(), number)
        sendMessage(player, Bundle(PluginData[player.uuid()])["discord-pin-queue", number.toString()])
    }

    /*override fun onGuildMemberJoin(e: GuildMemberJoinEvent) {
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
    }*/

    fun shutdownNow() {
        if(Discord::catnip.isInitialized) catnip.shutdown()
    }
}