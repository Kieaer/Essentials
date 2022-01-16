package remake

import arc.ApplicationListener
import arc.Core
import arc.files.Fi
import arc.util.CommandHandler
import arc.util.Log
import arc.util.Timer
import mindustry.Vars
import mindustry.mod.Plugin
import org.hjson.JsonArray
import org.hjson.JsonObject
import org.hjson.Stringify
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

class Main : Plugin(){
    private val root: Fi = Core.settings.dataDirectory.child("mods/Essentials/")
    private val daemon: ExecutorService = Executors.newCachedThreadPool()
    private val timer = java.util.Timer()
    
    init {
        Log.info("[Essentials] Initializing..")

        createFile()
        DB.open()
        Config.load()
        Permission.load()
        PluginData.load()

        Event.register()

        Core.app.addListener(object : ApplicationListener {
            override fun dispose() {
                timer.cancel()
                DB.close()
                daemon.shutdown()
            }
        })
    }

    override fun init() {
        daemon.submit(FileWatchService)
        timer.scheduleAtFixedRate(Trigger.Time(), 1000, 1000)

        Vars.netServer.admins.addChatFilter { _, _ -> null }

        Vars.netServer.admins.addActionFilter { e ->
            if(e.player == null) return@addActionFilter true
            return@addActionFilter DB[e.player.uuid()] != null
        }
    }

    override fun registerClientCommands(handler: CommandHandler) {
        Commands(handler)
    }

    private fun createFile(){
        if(!root.child("data").exists()) {
            root.child("data").mkdirs()
        }

        if(!root.child("log").exists()) {
            val names = arrayOf("block", "chat", "deposit", "error", "griefer", "non-block", "player", "tap", "web", "withdraw")
            for(a in names) {
                if(!root.child("log/$a.log").exists()) {
                    root.child("log").mkdirs()
                    root.child("log/$a.log").file().createNewFile()
                }
            }
        }

        if(!root.child("motd").exists()) {
            root.child("motd").mkdirs()
            val names = arrayListOf("en_US", "ko_KR")
            val texts = arrayListOf("To edit this message, open [green]config/mods/Essentials/motd[] folder and edit [green]en_US.txt[]", "이 메세지를 수정할려면 [green]config/mods/Essentials/motd[] 폴더에서 [green]ko_KR.txt[] 파일을 수정하세요.")
            for(a in 0 until names.size) {
                if(!root.child("motd/${names[a]}.txt").exists()) {
                    root.child("motd/${names[a]}.txt").writeString(texts[a])
                }
            }
        }

        if(!root.child("permission.hjson").exists()) {
            val json = JsonObject()

            val owner = JsonObject()
            owner.add("admin", true)
            owner.add("chatFormat", "[sky][Owner] %1[orange] > [white]%2")
            owner.add("permission", JsonArray().add("all"))

            val admin = JsonObject()
            val adminPerm = JsonArray()
            adminPerm.add("color")
            adminPerm.add("spawn")
            adminPerm.add("weather")
            adminPerm.add("kill")
            adminPerm.add("team")
            adminPerm.add("mute")

            admin.add("inheritance", "user")
            admin.add("admin", true)
            admin.add("chatFormat", "[yellow][Admin] %1[orange] > [white]%2")
            admin.add("permission", adminPerm)

            val user = JsonObject()
            val userPerm = JsonArray()
            userPerm.add("ch")
            userPerm.add("info")
            userPerm.add("maps")
            userPerm.add("me")
            userPerm.add("motd")
            userPerm.add("players")
            userPerm.add("status")
            userPerm.add("time")
            userPerm.add("tp")
            userPerm.add("vote")

            user.add("inheritance", "visitor")
            user.add("chatFormat", "%1[orange] > [white]%2")
            user.add("permission", userPerm)

            val visitor = JsonObject()
            val visitorPerm = JsonArray()
            visitorPerm.add("register")
            visitorPerm.add("login")
            visitorPerm.add("help")
            visitorPerm.add("t")

            visitor.add("chatFormat", "%1[scarlet] > [white]%2")
            visitor.add("default", true)
            visitor.add("permission", visitorPerm)

            json.add("owner", owner)
            json.add("admin", admin)
            json.add("user", user)
            json.add("visitor", visitor)

            root.child("permission.hjson").writeString(json.toString(Stringify.HJSON))
        }

        if(!root.child("permission_user.hjson").exists()) {
            val obj = JsonArray()
            obj.setComment("""
                Usage
                {
                    uuid: String (Must need)
                    name: String (Set player name)
                    group: String (Set player permission group)
                    chatFormat: String (Set player chat format. %1 is name, %2 is text.)
                    admin: Boolean (Set player admin status)
                }
                
                Examples
                [
                    {
                        uuid: uuids
                        name: "my fun name"
                    },
                    {
                        uuid: uuida
                        chatFormat: "%1: %2"
                    },
                    {
                        uuid: babysuuid
                        name: baby
                        group: admin
                        chatFormat: "[blue][ADMIN][]%1[white]: %2"
                        admin: true
                    }
                ]
            """.trimIndent())
            root.child("permission_user.hjson").writeString(obj.toString(Stringify.HJSON_COMMENTS))
        }
    }
}