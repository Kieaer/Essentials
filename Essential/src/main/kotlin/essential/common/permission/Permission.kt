package essential.common.permission

import arc.files.Fi
import arc.util.Log
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import essential.common.bundle.Bundle
import essential.common.database.data.PlayerData
import essential.common.database.table.PlayerTable
import essential.common.players
import essential.common.rootPath
import essential.core.Main.Companion.scope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.r2dbc.update
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
        val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))
        
        user = try {
            if (userFile.exists()) {
                val raw = userFile.readString()
                // Remove YAML comments and whitespace to check if there's any real content
                val stripped = raw.lineSequence()
                    .filter { line -> !line.trimStart().startsWith("#") }
                    .joinToString("\n")
                    .trim()
                if (stripped.isEmpty() || stripped == "---") {
                    // Treat comment-only or effectively empty files as empty map
                    mapOf()
                } else {
                    yaml.decodeFromString(MapSerializer(String.serializer(), PermissionData.serializer()), raw)
                }
            } else {
                mapOf()
            }
        } catch (e: Exception) {
            Log.warn("Failed to parse permission_user.yaml: ${e.message}")
            // Create a new empty file with the comment template
            userFile.writeString(comment)
            mapOf()
        }
        
        main = try {
            if (mainFile.exists()) {
                yaml.decodeFromString(MapSerializer(String.serializer(), RoleConfig.serializer()), mainFile.readString())
            } else {
                mapOf()
            }
        } catch (e: Exception) {
            Log.warn("Failed to parse permission.yaml: ${e.message}")
            // Reset to default permissions file
            if (mainFile.exists()) mainFile.delete()
            mainFile.write(this::class.java.getResourceAsStream("/permission_default.yaml")!!, false)
            try {
                yaml.decodeFromString(MapSerializer(String.serializer(), RoleConfig.serializer()), mainFile.readString())
            } catch (e2: Exception) {
                Log.err("Failed to parse default permission.yaml: ${e2.message}")
                mapOf()
            }
        }

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
                    scope.launch {
                        suspendTransaction {
                            PlayerTable.update({ PlayerTable.uuid eq uuid }) {
                                it[PlayerTable.name] = permissionData.name
                                it[PlayerTable.permission] = permissionData.group
                            }
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
            val passed = group.permission.contains(command) || group.permission.contains("all")
            Log.debug("[Permission] ${data.name} > group: ${this[data].group} -> command: $command -> $passed")
            passed
        } else {
            Log.debug("[Permission] ${data.name} > group: ${this[data].group} -> command: $command -> false")
            false
        }
    }

    @Serializable
    data class PermissionData(
        var name: String = "",
        var group: String = default,
        var admin: Boolean = false,
        var isAlert: Boolean = false,
        var alertMessage: String = "",
        var chatFormat: String = "",
    )

    @Serializable
    data class RoleConfig(
        val admin: Boolean? = null,
        val inheritance: String? = null,
        val permission: MutableList<String> = mutableListOf(),
        val default: Boolean? = null,
    )
}