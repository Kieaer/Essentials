package ksp.event

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.writeTo
import mindustry.game.EventType

class EventProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger) : SymbolProcessor {

    /**
     * Checks if a KSType represents an enum class.
     * @param type The KSType to check
     * @return true if the type represents an enum class, false otherwise
     */
    private fun isEnum(type: KSType?): Boolean {
        if (type == null) return false
        val declaration = type.declaration
        return declaration is KSClassDeclaration && declaration.classKind == ClassKind.ENUM_CLASS
    }

    /**
     * Checks if a KSType represents the EventType.Trigger enum.
     * @param type The KSType to check
     * @return true if the type is EventType.Trigger, false otherwise
     */
    private fun isTriggerEnum(type: KSType?): Boolean {
        if (type == null) return false
        val qualifiedName = type.declaration.qualifiedName?.asString() ?: return false
        return qualifiedName == "mindustry.game.EventType.Trigger"
    }

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
        val base = packageName.ifBlank { "essential.core" }
        return "$base.generated"
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val eventSymbols = resolver.getSymbolsWithAnnotation(Event::class.qualifiedName!!)
        val unprocessed = eventSymbols.filter { !it.validate() }.toList()

        // 이벤트 핸들러 생성 - 패키지별로 그룹화
        if (eventSymbols.any()) {
            val eventFunctions = eventSymbols
                .filter { it is KSFunctionDeclaration && it.validate() }
                .map { it as KSFunctionDeclaration }
                .toList()

            if (eventFunctions.isNotEmpty()) {
                // 패키지별로 그룹화
                val eventFunctionsByPackage = eventFunctions.groupBy { function ->
                    function.containingFile?.packageName?.asString() ?: "essential.core"
                }

                // 각 패키지별로 파일 생성
                eventFunctionsByPackage.forEach { (_, functions) ->
                    generateEventHandlersFile(functions)
                }
            }
        }

        return unprocessed
    }

    private fun generateEventHandlersFile(functions: List<KSFunctionDeclaration>) {
        // Determine package name based on the package of the first function
        val packageName = determinePackageName(functions)

        // Extract the base package (e.g., essential.core, essential.achievements)
        val fileSpec = FileSpec.builder(packageName, "EventHandlersGenerated")
            .addImport("arc.Events", "")
            .addImport("arc.func", "Cons")
            .addImport("essential.common", "eventListeners")
            .addImport("mindustry.game.EventType", "Trigger")
            .addFunction(generateRegisterEventHandlersFunction(functions))
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(true, *functions.mapNotNull { it.containingFile }.toTypedArray()))
    }

    private data class EventTypeInfo(
        val eventTypeName: String,
        val packageName: String,
        val functionName: String,
        val isEnumType: Boolean,
        val isTrigger: Boolean
    )

    private fun generateRegisterEventHandlersFunction(functions: List<KSFunctionDeclaration>): FunSpec {
        // Extract event types from function parameters
        val eventTypes = functions.map { function ->
            // Get the first parameter of the function, which should be the event type
            val parameter = function.parameters.firstOrNull()
            val parameterType = parameter?.type?.resolve()
            val eventTypeName = parameterType?.declaration?.qualifiedName?.asString() ?: "Unknown"
            val isEnumType = isEnum(parameterType)
            val isTrigger = isTriggerEnum(parameterType)

            // Get the simple name of the function
            val functionName = function.simpleName.asString()
            val packageName = function.packageName.asString()

            EventTypeInfo(eventTypeName, packageName, functionName, isEnumType, isTrigger)
        }

        return FunSpec.builder("registerGeneratedEventHandlers")
            .addCode(
                """
                ${eventTypes.joinToString("\n\n") { info ->
                    if (info.eventTypeName == "Unknown" || info.eventTypeName == "null") {
                        // If function doesn't have parameters, use the function name to determine which Trigger enum to use
                        try {
                            EventType.Trigger.valueOf(info.functionName)
                            """
                            Events.run(Trigger.${info.functionName}) {
                                ${info.packageName}.${info.functionName}()
                            }
                            """.trimIndent()
                        } catch (e: IllegalArgumentException) {
                            """
                            ${info.packageName}.${info.functionName}()
                            """.trimIndent()
                        }
                    } else if (info.isTrigger) {
                        // For Trigger enum parameters, use Events.run instead of Events.on
                        """
                        Events.run(Trigger.${info.functionName}) {
                            ${info.packageName}.${info.functionName}(it)
                        }
                        """.trimIndent()
                    } else if (info.isEnumType) {
                        """
                        Events.on(${info.eventTypeName}::class.java) {
                            ${info.packageName}.${info.functionName}(it)
                        }.also { listener -> eventListeners[${info.eventTypeName}::class.java] = listener }
                        """.trimIndent()
                    } else {
                        """
                        Events.on(${info.eventTypeName}::class.java, Cons<${info.eventTypeName}> {
                            ${info.packageName}.${info.functionName}(it)
                        }.also { listener -> eventListeners[${info.eventTypeName}::class.java] = listener })
                        """.trimIndent()
                    }
                }}
                """.trimIndent()
            )
            .build()
    }
}

class EventProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return EventProcessor(environment.codeGenerator, environment.logger)
    }
}
