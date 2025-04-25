package essential.permission

import arc.Core
import arc.files.Fi
import com.charleskorn.kaml.*
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import essential.bundle.Bundle
import essential.core.Main.Companion.database
import essential.database.data.PlayerData
import essential.players
import org.hjson.*
import org.jetbrains.exposed.sql.transactions.experimental.suspendedTransactionAsync
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

object Permission {
    private var main: Map<String, RoleConfig> = mapOf()
    private var user: Map<String, PermissionData>? = mapOf()
    var default = "user"
    private val mainFile: Fi = Core.settings.dataDirectory.child("mods/Essentials/permission.yaml")
    private val userFile: Fi = Core.settings.dataDirectory.child("mods/Essentials/permission_user.yaml")

    private val bundle = Bundle(Locale.getDefault().toLanguageTag())

    private val comment = """
        #${bundle["permission.wiki"]}
        #${bundle["permission.sort"]}
        #${bundle["permission.notice"]}
        #${bundle["permission.usage"]}
        # name:${bundle["permission.usage.name"]}
        # group:${bundle["permission.usage.group"]}
        # admin:${bundle["permission.usage.admin"]}
        # isAlert:${bundle["permission.usage.isAlert"]}
        # alertMessage:${bundle["permission.usage.alertMessage"]}
        # chatFormat:${bundle["permission.usage.chatformat"]}
        
        #${bundle["permission.example"]}
        # uuid123:
        #     name: my fun name
        # uuids:
        #     name: asdfg
        #     group: admin
        #     admin: true
        #     isAlert: true
        #     alertMessage: Player asdfg has entered the server!
        #     chatFormat: [admin] %1 > %2
        ---
        """.trimIndent()

    init {
        if (!mainFile.exists()) {
            mainFile.write(this::class.java.getResourceAsStream("/permission_default.yaml")!!, false)
        }

        if (!userFile.exists()) {
            userFile.writeString(comment)
        }
    }

    fun load() {
        val mapper = ObjectMapper(YAMLFactory()).apply {
            registerKotlinModule()
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

        user = mapper.readValue(userFile.reader())
        main = mapper.readValue(mainFile.reader())

        for ((name, roleConfig) in main) {
            if (default == "user" && roleConfig.default == true) {
                default = name
            }

            var inheritance: String? = roleConfig.inheritance
            while (inheritance != null) {
                val inheritedRoleConfig = main[inheritance]
                inheritedRoleConfig?.let { inheritedRole ->
                    for (permission in inheritedRole.permission) {
                        if (!permission.contains("all", true) && !roleConfig.permission.contains(permission)) {
                            roleConfig.permission.add(permission)
                        }
                    }
                    inheritance = inheritedRole.inheritance
                } ?: run {
                    inheritance = null
                }
            }
        }

        apply()
    }

    suspend fun apply() {
        if (user != null) {
            suspendedTransactionAsync {
                for ((uuid, permissionData) in user!!) {
                    val c = players.find { e -> e.uuid == uuid }
                    if (c == null) {
                        val data = PlayerData.find {  }[uuid]
                        if (data != null) {
                            data.permission = permissionData.group
                            data.name = permissionData.name
                            database.queue(data)
                        }
                    } else {
                        c.permission = permissionData.group
                        c.name = permissionData.name
                        c.player.name(permissionData.name)
                        c.player.admin(permissionData.admin)
                        database.queue(c)
                    }
                }
            }
        }
    }

    operator fun get(data: PlayerData): PermissionData {
        val result = PermissionData()

        val u = user?.get(data.uuid)
        if (u != null) {
            result.name = if (u.name.isEmpty()) data.player.name() else u.name
            result.group = u.group
            result.admin = u.admin
            result.isAlert = u.isAlert
            result.alertMessage = u.alertMessage
            result.chatFormat = u.chatFormat
        } else {
            result.name = data.player.name()
            result.group = data.permission
            result.admin = false
            result.isAlert = false
            result.alertMessage = ""
            result.chatFormat = ""
        }

        return result
    }

    fun check(data: DB.PlayerData, command: String): Boolean {
        val group = main[this[data].group]
        return if (group != null) {
            group.permission.contains(command) || group.permission.contains("all")
        } else {
            false
        }
    }

    data class PermissionData (
        var name: String = "",
        var group: String = default,
        var admin: Boolean = false,
        var isAlert: Boolean = false,
        var alertMessage: String = "",
        var chatFormat: String = ""
    )

    data class RoleConfig (
        val admin: Boolean? = null,
        val inheritance: String? = null,
        val permission: MutableList<String> = mutableListOf(),
        val default: Boolean? = null
    )
}