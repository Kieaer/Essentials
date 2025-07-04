package essential.permission

import arc.files.Fi
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import essential.bundle.Bundle
import essential.database.data.PlayerData
import essential.database.table.PlayerTable
import essential.players
import essential.rootPath
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import java.util.*

object Permission {
    private var main: Map<String, RoleConfig> = mapOf()
    private var user: Map<String, PermissionData>? = mapOf()
    var default = "user"
    private val mainFile: Fi = rootPath.child("permission.yaml")
    private val userFile: Fi = rootPath.child("permission_user.yaml")

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
        # chatFormat:${bundle["permission.usage.chatFormat"]}
        
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

    fun apply() {
        if (user != null) {
            for ((uuid, permissionData) in user!!) {
                val player = players.find { e -> e.uuid == uuid }
                if (player == null) {
                    transaction {
                        PlayerTable.update( { PlayerTable.uuid eq uuid } ) {
                            it[PlayerTable.name] = permissionData.name
                            it[PlayerTable.permission] = permissionData.group
                        }
                    }
                } else {
                    player.permission = permissionData.group
                    player.name = permissionData.name
                    player.player.name(permissionData.name)
                    player.player.admin(permissionData.admin)
                }
            }
        }
    }

    operator fun get(data: PlayerData): PermissionData {
        val result = PermissionData()

        val u = user?.get(data.uuid)
        if (u != null) {
            result.name = u.name.ifEmpty { data.player.name() }
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

    fun check(data: PlayerData, command: String): Boolean {
        val group = main[this[data].group]
        return if (group != null) {
            group.permission.contains(command) || group.permission.contains("all")
        } else {
            false
        }
    }

    data class PermissionData(
        var name: String = "",
        var group: String = default,
        var admin: Boolean = false,
        var isAlert: Boolean = false,
        var alertMessage: String = "",
        var chatFormat: String = "",
    )

    data class RoleConfig(
        val admin: Boolean? = null,
        val inheritance: String? = null,
        val permission: MutableList<String> = mutableListOf(),
        val default: Boolean? = null,
    )
}