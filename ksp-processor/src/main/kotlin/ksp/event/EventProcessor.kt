package ksp.event

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.writeTo

class EventProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger) : SymbolProcessor {

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

        // Extract the base package (e.g., essential.core, essential.achievements)
        // The pattern is "essential.X" where X is the module name
        val regex = "essential\\.[a-zA-Z0-9]+"
        val pattern = Regex(regex)
        val matchResult = pattern.find(packageName)

        return if (matchResult != null) {
            "${matchResult.value}.generated"
        } else {
            "essential.core.generated" // Default fallback
        }
    }

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val eventSymbols = resolver.getSymbolsWithAnnotation(Event::class.qualifiedName!!)
        val unprocessed = eventSymbols.filter { !it.validate() }.toList()

        if (eventSymbols.any()) {
            val eventFunctions = eventSymbols
                .filter { it is KSFunctionDeclaration && it.validate() }
                .map { it as KSFunctionDeclaration }
                .toList()

            if (eventFunctions.isNotEmpty()) {
                generateEventHandlersFile(eventFunctions)
            }
        }

        return unprocessed
    }

    private fun generateEventHandlersFile(functions: List<KSFunctionDeclaration>) {
        // Determine package name based on the package of the first function
        val packageName = determinePackageName(functions)

        // Extract the base package (e.g., essential.core, essential.achievements)
        val basePackage = packageName.substringBeforeLast(".generated")

        val fileSpec = FileSpec.builder(packageName, "EventHandlersGenerated")
            .addImport("arc.Events", "")
            .addImport("arc.func", "Cons")
            .addImport(basePackage, "eventListeners")
            .addFunction(generateRegisterEventHandlersFunction(functions))
            .build()

        fileSpec.writeTo(codeGenerator, Dependencies(true, *functions.mapNotNull { it.containingFile }.toTypedArray()))
    }

    private fun generateRegisterEventHandlersFunction(functions: List<KSFunctionDeclaration>): FunSpec {
        // Extract event types from function parameters
        val eventTypes = functions.map { function ->
            // Get the first parameter of the function, which should be the event type
            val parameter = function.parameters.firstOrNull()
            val parameterType = parameter?.type?.resolve()
            val eventTypeName = parameterType?.declaration?.qualifiedName?.asString() ?: "Unknown"

            // Get the simple name of the function
            val functionName = function.simpleName.asString()

            Triple(eventTypeName, function.packageName.asString(), functionName)
        }

        return FunSpec.builder("registerGeneratedEventHandlers")
            .addCode(
                """
                ${eventTypes.joinToString("\n\n") { (eventType, packageName, functionName) ->
                    """
                    Events.on($eventType::class.java, Cons<$eventType> {
                        $packageName.$functionName(it)
                    }.also { listener -> eventListeners[$eventType::class.java] = listener })
                    """.trimIndent()
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
