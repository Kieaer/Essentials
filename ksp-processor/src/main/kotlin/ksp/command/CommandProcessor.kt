package ksp.command

import arc.util.CommandHandler
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.writeTo

class CommandProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {


    /**
     * Determines the package name for generated code based on the package of the annotated functions.
     * Extracts the base package from the function's package and appends ".generated" to it.
     * For example, if the function is in "essential.core", the generated package will be "essential.core.generated".
     */
    private fun determinePackageName(functions: List<KSFunctionDeclaration>): String {
        if (functions.isEmpty()) {
            return "essential.core.generated" // Default package if no functions
        }

        // Get the package name from the first function's containing file
        val firstFunction = functions.first()
        val packageName = firstFunction.containingFile?.packageName?.asString() ?: ""

        logger.info("Package name: $packageName")

        // Always append .generated to the full original package to respect nested structures
        val base = if (packageName.isNotBlank()) packageName else "essential.core"
        return "$base.generated"
    }

    /**
     * Finds the package of the Commands class by looking at the parent class of the annotated functions.
     * If no function is in a class named Commands, falls back to the base package.
     */
    private fun findCommandsPackage(functions: List<KSFunctionDeclaration>): String {
        if (functions.isEmpty()) {
            return "essential.core" // Default package if no functions
        }

        // Check if any function is in a class named Commands
        for (function in functions) {
            val parentDeclaration = function.parentDeclaration
            if (parentDeclaration != null && parentDeclaration.simpleName.asString() == "Commands") {
                // Get the package of the parent class
                val packageName = function.containingFile?.packageName?.asString() ?: ""
                return packageName
            }
        }

        // If no function is in a Commands class, check if any file contains a Commands class
        for (function in functions) {
            val file = function.containingFile ?: continue
            val filePackage = file.packageName.asString()
            val fileDeclarations = file.declarations
            for (declaration in fileDeclarations) {
                if (declaration.simpleName.asString() == "Commands") {
                    return filePackage
                }
            }
        }

        // If no Commands class found, fall back to the base package
        val packageName = determinePackageName(functions)
        return packageName.substringBeforeLast(".generated")
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val serverSymbols = resolver.getSymbolsWithAnnotation(ServerCommand::class.qualifiedName!!)
        val clientSymbols = resolver.getSymbolsWithAnnotation(ClientCommand::class.qualifiedName!!)

        val unprocessedServer = serverSymbols.filter { !it.validate() }.toList()
        val unprocessedClient = clientSymbols.filter { !it.validate() }.toList()

        // 서버 명령어 생성 - 패키지별로 그룹화
        if (serverSymbols.any()) {
            val serverFunctions = serverSymbols
                .filter { it is KSFunctionDeclaration && it.validate() }
                .map { it as KSFunctionDeclaration }
                .toList()

            if (serverFunctions.isNotEmpty()) {
                // 패키지별로 그룹화
                val serverFunctionsByPackage = serverFunctions.groupBy { function ->
                    function.containingFile?.packageName?.asString() ?: "essential.core"
                }

                // 각 패키지별로 파일 생성
                serverFunctionsByPackage.forEach { (packageName, functions) ->
                    generateServerCommandsFile(packageName, functions)
                }
            }
        }

        // 클라이언트 명령어 생성 - 패키지별로 그룹화
        if (clientSymbols.any()) {
            val clientFunctions = clientSymbols
                .filter { it is KSFunctionDeclaration && it.validate() }
                .map { it as KSFunctionDeclaration }
                .toList()

            if (clientFunctions.isNotEmpty()) {
                // 패키지별로 그룹화
                val clientFunctionsByPackage = clientFunctions.groupBy { function ->
                    function.containingFile?.packageName?.asString() ?: "essential.core"
                }

                // 각 패키지별로 파일 생성
                clientFunctionsByPackage.forEach { (packageName, functions) ->
                    generateClientCommandsFile(functions)
                }
            }
        }

        return unprocessedServer + unprocessedClient
    }

    private fun generateServerCommandsFile(packageName: String, functions: List<KSFunctionDeclaration>) {
        // Determine target package for generated file based on module base (use .generated suffix)
        val targetPackage = determinePackageName(functions)

        // Find the package of the Commands class by looking at the parent class or file declarations
        val commandsPackage = findCommandsPackage(functions)

        val fileSpec = FileSpec.builder(targetPackage, "ServerCommandsGenerated")
            .addImport(commandsPackage, "Commands")
            .addImport("ksp.command", "ServerCommand")
            .addImport("arc.util", "CommandHandler")
            .addFunction(generateRegisterServerCommandsFunction(functions))
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(true, *functions.mapNotNull { it.containingFile }.toTypedArray()))
    }

    private fun generateClientCommandsFile(functions: List<KSFunctionDeclaration>) {
        // Determine package name based on the package of the first function
        val packageName = determinePackageName(functions)

        // Find the package of the Commands class by looking at the parent class or file declarations
        val commandsPackage = findCommandsPackage(functions)

        val builder = FileSpec.builder(packageName, "ClientCommandsGenerated")
            .addImport(commandsPackage, "Commands")
            .addImport("ksp.command", "ClientCommand")
            .addImport("arc.util", "CommandHandler")
            .addImport("mindustry.gen", "Playerc")

        builder
            .addImport("essential.common.util", "findPlayerData")
            .addImport("essential.common.permission", "Permission")
            .addImport("essential.common.bundle", "Bundle")
            .addImport("essential.common.database.data", "PlayerData")
            .addFunction(generateRegisterClientCommandsFunction(functions))

        val fileSpec = builder.build()
        fileSpec.writeTo(codeGenerator, Dependencies(true, *functions.mapNotNull { it.containingFile }.toTypedArray()))
    }

    private fun generateRegisterServerCommandsFunction(functions: List<KSFunctionDeclaration>): FunSpec {
        // Here get annotation parameter values
        val annotationValues = functions.map { function ->
            val annotation = function.annotations.find {
                it.shortName.asString() == "ServerCommand" ||
                        it.shortName.asString() == "ksp.command.ServerCommand"
            }

            val name = annotation?.arguments?.find { it.name?.asString() == "name" }?.value?.toString()
                ?: function.simpleName.asString()
            val parameter = annotation?.arguments?.find { it.name?.asString() == "parameter" }?.value?.toString() ?: ""
            val description = annotation?.arguments?.find { it.name?.asString() == "description" }?.value?.toString()
                ?: "Generated server command"

            Triple(name, parameter, description)
        }

        return FunSpec.builder("registerGeneratedServerCommands")
            .addParameter("handler", ClassName("arc.util", "CommandHandler"))
            .addCode(
                """
                val commands = Commands()
                val serverCommands = listOf(
                ${
                    functions.joinToString(",\n                    ") { function ->
                        if (function.parameters.isEmpty()) {
                            "{ args: Array<String> -> commands.${function.simpleName.asString()}() }"
                        } else {
                            "{ args: Array<String> -> commands.${function.simpleName.asString()}(args) }"
                        }
                    }
                }
                )

                val annotations = listOf(
                ${
                    functions.mapIndexed { index, _ ->
                        val (name, parameter, description) = annotationValues[index]
                        "ServerCommand(\"$name\", \"$parameter\", \"$description\")"
                    }.joinToString(",\n                    ")
                }
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
        // Get annotation parameter values
        val annotationValues = functions.map { function ->
            val annotation = function.annotations.find {
                it.shortName.asString() == "ClientCommand" ||
                        it.shortName.asString() == "ksp.command.ClientCommand"
            }

            val name = annotation?.arguments?.find { it.name?.asString() == "name" }?.value?.toString()
                ?: function.simpleName.asString()
            val parameter = annotation?.arguments?.find { it.name?.asString() == "parameter" }?.value?.toString() ?: ""
            val description = annotation?.arguments?.find { it.name?.asString() == "description" }?.value?.toString()
                ?: "Generated client command"

            Triple(name, parameter, description)
        }

        return FunSpec.builder("registerGeneratedClientCommands")
            .addParameter("handler", CommandHandler::class)
            .addCode(
                """
                val commands = Commands()
                val clientCommands = listOf(
                ${
                    functions.joinToString(",\n                    ") { function ->
                        if (function.parameters.size == 1) {
                            "{ data: PlayerData, args: Array<String> -> commands.${function.simpleName.asString()}(data) }"
                        } else {
                            "{ data: PlayerData, args: Array<String> -> commands.${function.simpleName.asString()}(data, args) }"
                        }
                    }
                }
                )

                val annotations = listOf(
                ${
                    functions.mapIndexed { index, _ ->
                        val (name, parameter, description) = annotationValues[index]
                        "ClientCommand(\"$name\", \"$parameter\", \"$description\")"
                    }.joinToString(",\n                    ")
                }
                )

                for (i in clientCommands.indices) {
                    val command = clientCommands[i]
                    val annotation = annotations[i]

                    handler.register<Playerc>(annotation.name, annotation.parameter, annotation.description) { args, player ->
                        val data = findPlayerData(player.uuid())
                        if (data != null) {
                            if (Permission.check(data, annotation.name)) {
                                command(data, args)
                            } else {
                                if (annotation.name == "js") {
                                    player.kick(Bundle(player.locale())["command.js.no.permission"])
                                } else {
                                    data.err("command.permission.false")
                                }
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
