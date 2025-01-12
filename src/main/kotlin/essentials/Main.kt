package essentials

import arc.Core
import arc.files.Fi
import arc.util.CommandHandler
import arc.util.Log
import essentials.command.ClientCommand
import essentials.command.ServerCommand
import essentials.database.DatabaseConfig
import mindustry.gen.Playerc
import mindustry.mod.Plugin
import org.reflections.Reflections
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

class Main : Plugin() {
    private val rootPath: Fi = Core.settings.dataDirectory.child("mods/Essentials/")

    override fun init() {
        if (!rootPath.exists()) rootPath.mkdirs()

        val database = DatabaseConfig(rootPath)
        database.connect()

        super.init()
    }

    override fun registerServerCommands(handler: CommandHandler) {
        val serverCommandPackage = Reflections("essentials.command.server").getSubTypesOf(Any::class.java)

        for (clazz in serverCommandPackage) {
            val kClass = clazz.kotlin
            val instance = kClass.objectInstance ?: kClass.createInstance()

            val commandList = kClass.functions.filter { it.findAnnotation<ServerCommand>() != null }
            for (method in commandList) {
                val annotation = method.findAnnotation<ServerCommand>()
                handler.register(annotation?.name, annotation?.parameter, annotation?.description) { args ->
                    if (args.isNotEmpty()) {
                        method.call(instance, arrayOf(*args))
                    } else {
                        try {
                            method.call(instance, arrayOf<String>())
                        } catch (e: Exception) {
                            Log.err("arg size - ${args.size}")
                            Log.err("command - ${annotation.name}")
                        }
                    }
                }
            }
        }
    }


    override fun registerClientCommands(handler: CommandHandler) {
        val commands = Commands()

        for (functions in commands::class.declaredFunctions) {
            val annotation = functions.findAnnotation<ClientCommand>()
            if (annotation != null) {
                handler.register(
                    annotation.name,
                    annotation.parameter,
                    annotation.description
                ) { args, player: Playerc ->
                    val data = findPlayerData(player.uuid()) ?: DB.PlayerData()
                    if (Permission.check(data, annotation.name)) {
                        if (args.isNotEmpty()) {
                            functions.call(commands, player, data, arrayOf(*args))
                        } else {
                            functions.call(commands, player, data, arrayOf<String>())
                        }
                    } else {
                        if (annotation.name == "js") {
                            player.kick(Bundle(player.locale())["command.js.no.permission"])
                        } else {
                            data.send("command.permission.false")
                        }
                    }
                }
            }
        }
    }
}