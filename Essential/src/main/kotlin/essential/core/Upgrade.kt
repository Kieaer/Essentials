package essential.core

import arc.Events
import arc.util.Log
import com.charleskorn.kaml.Yaml
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import essential.core.Main.Companion.CONFIG_PATH
import essential.core.Main.Companion.conf
import essential.core.Main.Companion.root
import mindustry.game.EventType.ServerLoadEvent
import org.hjson.JsonObject
import org.hjson.JsonValue
import org.hjson.Stringify
import java.nio.charset.Charset

class Upgrade {
    fun upgrade() {
        fun edit(config: MutableList<String>, lineNumber: Int, new: String){
            val line = config[lineNumber - 1]
            val indent = line.takeWhile { it.isWhitespace() }
            config[lineNumber - 1] = "$indent$new"
        }

        if (root.child("config.txt").exists()) {
            val json = JsonValue.readHjson(root.child("config.txt").readString()).asObject()
            val features = json.get("features").asObject()
            val security = json.get("security").asObject()

            val config = root.child("config/config.yaml").file().readLines(Charset.forName("UTF-8")).toMutableList()
            edit(config, 9, "url: " + json.get("plugin").asObject().get("database").asString())
            edit(config, 10, "username: " + json.get("plugin").asObject().get("databaseID").asString())
            edit(config, 11, "password: " + json.get("plugin").asObject().get("databasePW").asString())
            edit(config, 17, "enabled: " + features.get("afk").asBoolean())
            edit(config, 19, "time: " + features.get("afkTime").asInt())
            edit(config, 21, "server: " + features.getString("afkServer", ""))
            edit(config, 26, "enabled: " + features.get("vote").asBoolean())
            edit(config, 29, "enableVotekick: " + security.get("votekick").asBoolean())
            edit(config, 36, "limit: " + features.get("spawnLimit").asInt())
            edit(config, 42, "enabled: " + features.get("message").asBoolean())
            edit(config, 44, "time: " + features.get("messageTime").asInt() * 60)
            edit(config, 49, "autoTeam: " + features.get("pvpAutoTeam").asBoolean())
            edit(config, 51, "spector: " + features.get("pvpSpector").asBoolean())
            edit(config, 57, "enabled: " + features.get("moveEffects").asBoolean())
            edit(config, 63, "display: " + features.get("expDisplay").asBoolean())
            edit(config, 67, "autoSkip: " + features.get("waveskip").asInt())
            edit(config, 83, "limit: " + features.get("skiplimit").asInt())
            edit(config, 88, "time: " + features.get("rollbackTime").asInt())

            root.child("config/config.yaml").file().writeText(config.joinToString("\n"))
            conf = Yaml.default.decodeFromString(CoreConfig.serializer(), root.child(CONFIG_PATH).readString())

            val permissionFile = root.child("permission.txt")
            if (permissionFile.exists()) {
                val file = permissionFile.file().reader(Charset.forName("UTF-8"))
                val data = JsonValue.readHjson(file).toString(Stringify.FORMATTED)
                file.close()
                val node = ObjectMapper().readTree(data)
                root.child("permission.yaml").writeString(YAMLMapper().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES).writeValueAsString(node))
                permissionFile.moveTo(root.child("old/permission.txt"))
            }

            val permissionUserFile = root.child("permission_user.txt")
            if (permissionUserFile.exists()) {
                val file = permissionUserFile.file().reader(Charset.forName("UTF-8"))
                val data = JsonValue.readHjson(file)
                file.close()
                val j = JsonObject()
                for(a in data.asArray()) {
                    val t = a.asObject()
                    val obj = JsonObject()
                    if (t.get("name") != null) {
                        obj.add("name", t.get("name").asString())
                    }
                    if (t.get("group") != null) {
                        obj.add("group", t.get("group").asString())
                    }
                    if (t.get("chatFormat") != null) {
                        obj.add("chatFormat", t.get("chatFormat").asString())
                    }
                    if (t.get("admin") != null) {
                        obj.add("admin", t.get("admin").asBoolean())
                    }
                    if (t.get("isAlert") != null) {
                        obj.add("isAlert", t.get("isAlert").asBoolean())
                    }
                    if (t.get("alertMessage") != null) {
                        obj.add("alertMessage", t.get("alertMessage").asString())
                    }

                    j.add(a.asObject().get("uuid").asString(), obj)
                }
                val node = ObjectMapper().readTree(j.toString(Stringify.PLAIN))
                root.child("permission_user.yaml").writeString(YAMLMapper().enable(YAMLGenerator.Feature.MINIMIZE_QUOTES).writeValueAsString(node))
                permissionUserFile.moveTo(root.child("old/permission_user.txt"))
            }

            Events.on(ServerLoadEvent::class.java) {
                if (root.child("config/config_protect.yaml").exists()) {
                    val protect =
                        root.child("config/config_protect.yaml").file().readLines(Charset.forName("UTF-8")).toMutableList()
                    edit(protect, 4, "enabled: " + features.get("pvpPeace").asBoolean())
                    edit(protect, 7, "time: " + features.get("pvpPeaceTime").asInt())
                    edit(protect, 11, "enabled: " + features.get("border").asBoolean())
                    edit(protect, 14, "destroyCore: " + features.get("destroyCore").asBoolean())
                    edit(
                        protect,
                        18,
                        "enabled: " + if (json.get("plugin").asObject().get("authType")
                                .asString() == "none"
                        ) "false" else "true"
                    )
                    edit(protect, 21, "authType: " + json.get("plugin").asObject().get("authType").asString())
                    edit(protect, 24, "discordURL: " + json.get("discord").asObject().getString("discordURL", ""))
                    edit(protect, 32, "powerDetect: " + security.get("antiGrief").asBoolean())
                    edit(protect, 36, "vpn: " + features.get("antiVPN").asBoolean())
                    edit(protect, 39, "foo: " + security.get("blockfooclient").asBoolean())
                    edit(protect, 42, "mobile: " + security.get("allowMobile").asBoolean())
                    edit(protect, 49, "enabled: " + security.get("minimumName").asBoolean())
                    edit(protect, 54, "strict: " + features.get("fixedName").asBoolean())
                    edit(protect, 57, "blockNewUser: " + security.get("blockNewUser").asBoolean())

                    root.child("config/config_protect.yaml").file().writeText(protect.joinToString("\n"))
                } else {
                    Log.warn(Bundle()["event.plugin.upgrade.protect"])
                }

                if (root.child("config/config_chat.yaml").exists()) {
                    val chat =
                        root.child("config/config_chat.yaml").file().readLines(Charset.forName("UTF-8")).toMutableList()
                    edit(chat, 1, "chatFormat: \"" + features.get("chatFormat").asString() + "\"")
                    edit(chat, 4, "enabled: " + features.get("chatlimit").asBoolean())
                    edit(chat, 6, "language: " + features.get("chatlanguage").asString())
                    edit(chat, 10, "enabled: " + features.get("chatBlacklist").asBoolean())
                    edit(chat, 12, "regex: " + features.get("chatBlacklistRegex").asBoolean())

                    root.child("config/config_chat.yaml").file().writeText(chat.joinToString("\n"))
                } else {
                    Log.warn(Bundle()["event.plugin.upgrade.chat"])
                }

                if (root.child("config/config_bridge.yaml").exists()) {
                    val bridge =
                        root.child("config/config_bridge.yaml").file().readLines(Charset.forName("UTF-8")).toMutableList()
                    edit(bridge, 2, "address: " + json.get("ban").asObject().get("shareBanListServer").asString())

                    root.child("config/config_bridge.yaml").file().writeText(bridge.joinToString("\n"))
                } else {
                    Log.warn(Bundle()["event.plugin.upgrade.bridge"])
                }

                root.child("old").mkdirs()
                root.child("config.txt").moveTo(root.child("old/config.txt"))
            }
        }
    }
}