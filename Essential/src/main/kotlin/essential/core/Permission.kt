package essential.core

import arc.Core
import arc.files.Fi
import com.charleskorn.kaml.*
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import essential.core.Main.Companion.database
import mindustry.Vars
import org.hjson.*
import java.util.*

object Permission {
    private var main : Map<String, RoleConfig> = mapOf()
    var user : Map<String, UserPermissionConfig> = mapOf()
    var default = "user"
    val mainFile : Fi = Core.settings.dataDirectory.child("mods/Essentials/permission.yaml")
    val userFile : Fi = Core.settings.dataDirectory.child("mods/Essentials/permission_user.yaml")

    val bundle = Bundle(Locale.getDefault().toLanguageTag())

    val comment = """
        #${bundle["permission.wiki"]}
        #${bundle["permission.sort"]}
        #${bundle["permission.notice"]}
        #${bundle["permission.usage"]}
        # name:${bundle["permission.usage.name"]}
        # group:${bundle["permission.usage.group"]}
        # admin:${bundle["permission.usage.admin"]}
        # isAlert:${bundle["permission.usage.isAlert"]}
        # alertMessage:${bundle["permission.usage.alertMessage"]}
        
        #${bundle["permission.example"]}
        # uuid123:
        #     name: my fun name
        # uuids:
        #     name: asdfg
        #     group: admin
        #     admin: true
        #     isAlert: true
        #     alertMessage: Player asdfg has entered the server!
        exampleuuid:
            name: it is test name
        """.trimIndent()

    init {
        if (!mainFile.exists()) {
            mainFile.write(this::class.java.getResourceAsStream("/permission_default.yaml")!!, false)
        }

        if (!userFile.exists()) {
            userFile.writeString(comment)
        }
    }

    @Throws(ParseException::class)
    fun load() {
        val mapper = ObjectMapper(YAMLFactory()).apply {
            registerKotlinModule()
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        user = mapper.readValue(userFile.reader())
        main = mapper.readValue(mainFile.reader())

        main.forEach { (name, roleConfig) ->
            roleConfig.let {
                if (default == "user" && roleConfig.default == true) {
                    default = name
                }

                var inheritance: String? = roleConfig.inheritance
                while (inheritance != null) {
                    val inheritedRoleConfig = main[inheritance]
                    inheritedRoleConfig?.let { inheritedRole ->
                        for (permission in inheritedRole.permissions) {
                            if (!permission.contains("*") && !roleConfig.permissions.contains(permission)) {
                                roleConfig.permissions.add(permission)
                            }
                        }
                        inheritance = inheritedRole.inheritance
                    } ?: run {
                        inheritance = null
                    }
                }
            }
        }

        apply()
    }

    fun apply() {
        user.forEach { (uuid, config) ->
            val c = database.players.find { e -> e.uuid == uuid }
            if (c == null) {
                val data = database[uuid]
                if (data != null && config.group != null) {
                    data.permission = config.group
                    data.name = config.name ?: data.name
                    database.queue(data)
                }
            } else {
                c.permission = config.group ?: default
                c.name = config.name ?: Vars.netServer.admins.findByName(c.player.uuid()).first().lastName
                c.player.admin(config.admin ?: false)
                c.player.name(config.name ?: Vars.netServer.admins.findByName(c.player.uuid()).first().lastName)
                database.queue(c)
            }
        }
    }

    operator fun get(data : DB.PlayerData) : PermissionData {
        val result = PermissionData()

        val u = user[data.uuid]
        if (u != null) {
            result.name = u.name ?: data.player.name()
            result.group = u.group ?: data.permission
            result.admin = u.admin ?: false
            result.isAlert = u.isAlert ?: false
            result.alertMessage = u.alertMessage ?: ""
        } else {
            result.name = data.player.name()
            result.group = data.permission
            result.admin = false
            result.isAlert = false
            result.alertMessage = ""
        }

        return result
    }

    fun check(data : DB.PlayerData, command : String) : Boolean {
        val group = main[this[data].group]
        return if (group != null) {
            group.permissions.contains(command) || group.permissions.contains("*")
        } else {
            false
        }
    }

    class PermissionData {
        var name = ""
        var uuid = ""
        var group = default
        var admin = false
        var isAlert = false
        var alertMessage = ""
    }

    data class RoleConfig(
        val admin: Boolean? = null,
        val inheritance: String? = null,
        val permissions: MutableList<String> = mutableListOf(),
        val default: Boolean? = null
    )

    data class UserPermissionConfig(
        val name: String? = null,
        val group: String? = null,
        val admin: Boolean? = null,
        val isAlert: Boolean? = null,
        val alertMessage: String? = null
    )
}