package essential.ksp

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class CommandProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val serverSymbols = resolver.getSymbolsWithAnnotation(ServerCommand::class.qualifiedName!!)
        val clientSymbols = resolver.getSymbolsWithAnnotation(ClientCommand::class.qualifiedName!!)
        
        val unprocessedServer = serverSymbols.filter { !it.validate() }.toList()
        val unprocessedClient = clientSymbols.filter { !it.validate() }.toList()

        // 서버 명령어 생성
        if (serverSymbols.any()) {
            val serverFunctions = serverSymbols
                .filter { it is KSFunctionDeclaration && it.validate() }
                .map { it as KSFunctionDeclaration }
                .toList()
            
            if (serverFunctions.isNotEmpty()) {
                generateServerCommandsFile(serverFunctions)
            }
        }

        // 클라이언트 명령어 생성
        if (clientSymbols.any()) {
            val clientFunctions = clientSymbols
                .filter { it is KSFunctionDeclaration && it.validate() }
                .map { it as KSFunctionDeclaration }
                .toList()
            
            if (clientFunctions.isNotEmpty()) {
                generateClientCommandsFile(clientFunctions)
            }
        }

        return unprocessedServer + unprocessedClient
    }

    private fun generateServerCommandsFile(functions: List<KSFunctionDeclaration>) {
        val packageName = "essential.core.generated"
        
        val fileSpec = FileSpec.builder(packageName, "ServerCommandsGenerated")
            .addImport("essential.core", "Commands")
            .addImport("essential.ksp", "ServerCommand")
            .addImport("arc.util", "CommandHandler")
            .addFunction(generateRegisterServerCommandsFunction(functions))
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(true, *functions.mapNotNull { it.containingFile }.toTypedArray()))
    }

    private fun generateClientCommandsFile(functions: List<KSFunctionDeclaration>) {
        val packageName = "essential.core.generated"
        
        val fileSpec = FileSpec.builder(packageName, "ClientCommandsGenerated")
            .addImport("essential.core", "Commands")
            .addImport("essential.ksp", "ClientCommand")
            .addImport("arc.util", "CommandHandler")
            .addImport("mindustry.gen", "Playerc")
            .addImport("essential.core.Event", "findPlayerData")
            .addImport("essential.permission", "Permission")
            .addImport("essential.bundle", "Bundle")
            .addImport("essential.database.data", "PlayerData")
            .addFunction(generateRegisterClientCommandsFunction(functions))
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(true, *functions.mapNotNull { it.containingFile }.toTypedArray()))
    }

    private fun generateRegisterServerCommandsFunction(functions: List<KSFunctionDeclaration>): FunSpec {
        return FunSpec.builder("registerGeneratedServerCommands")
            .addParameter("handler", ClassName("arc.util", "CommandHandler"))
            .addCode(
                """
                val commands = Commands()
                val serverCommands = listOf(
                ${functions.joinToString(",\n                    ") { 
                    "{ args: Array<String> -> commands.${it.simpleName.asString()}(args) }" 
                }}
                )
                
                val annotations = listOf(
                ${functions.joinToString(",\n                    ") { 
                    "ServerCommand(\"${it.simpleName.asString()}\", \"\", \"Generated server command\")" 
                }}
                )
                
                for (i in serverCommands.indices) {
                    val command = serverCommands[i]
                    val annotation = annotations[i]
                    
                    handler.register(annotation.name, annotation.parameter, annotation.description) { args ->
                        if (args.isNotEmpty()) {
                            command(args)
                        } else {
                            try {
                                command(arrayOf<String>())
                            } catch (e: Exception) {
                                arc.util.Log.err("arg size - 0")
                                arc.util.Log.err("command - ${'$'}{annotation.name}")
                            }
                        }
                    }
                }
                """.trimIndent()
            )
            .build()
    }

    private fun generateRegisterClientCommandsFunction(functions: List<KSFunctionDeclaration>): FunSpec {


        return FunSpec.builder("registerGeneratedClientCommands")
            .addParameter("handler", ClassName("arc.util", "CommandHandler"))
            .addCode(
                """
                val commands = Commands()
                val clientCommands = listOf(
                ${functions.joinToString(",\n                    ") { 
                    "{ player: Playerc, data: PlayerData, args: Array<String> -> commands.${it.simpleName.asString()}(player, data, args) }" 
                }}
                )
                
                val annotations = listOf(
                ${functions.joinToString(",\n                    ") { 
                    "ClientCommand(\"${it.simpleName.asString()}\", \"\", \"Generated client command\")" 
                }}
                )
                
                for (i in clientCommands.indices) {
                    val command = clientCommands[i]
                    val annotation = annotations[i]
                    
                    handler.register<Playerc>(annotation.name, annotation.parameter, annotation.description) { args, player ->
                        val data = findPlayerData(player.uuid()) ?: PlayerData()
                        if (Permission.check(data, annotation.name)) {
                            command(player, data, args)
                        } else {
                            if (annotation.name == "js") {
                                player.kick(Bundle(player.locale())["command.js.no.permission"])
                            } else {
                                data.send("command.permission.false")
                            }
                        }
                    }
                }
                """.trimIndent()
            )
            .build()
    }
}

class CommandProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return CommandProcessor(environment.codeGenerator, environment.logger)
    }
}