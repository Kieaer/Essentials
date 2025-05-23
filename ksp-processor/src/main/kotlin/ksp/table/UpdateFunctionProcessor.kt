package ksp.table

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo

class UpdateFunctionProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(GenerateUpdate::class.qualifiedName!!)
        val unprocessed = symbols.filter { !it.validate() }.toList()

        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(UpdateFunctionVisitor(), Unit) }

        return unprocessed
    }

    inner class UpdateFunctionVisitor : KSVisitorVoid() {
        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val properties = classDeclaration.getAllProperties()
                .filter { it.isDelegated() }
                .toList()

            if (properties.isEmpty()) {
                logger.warn("No delegated properties found in ${classDeclaration.simpleName.asString()}")
                return
            }

            val className = classDeclaration.toClassName()
            val packageName = className.packageName

            val fileSpec = FileSpec.builder(packageName, "${className.simpleName}UpdateExt")
                .addImport("essential.database.table", "PlayerTable")
                .addImport("org.jetbrains.exposed.sql.transactions.experimental", "newSuspendedTransaction")
                .addImport("org.jetbrains.exposed.sql", "SqlExpressionBuilder.eq")
                .addFunction(generateUpdateFunction(className, properties))
                .build()

            fileSpec.writeTo(codeGenerator, Dependencies(true, classDeclaration.containingFile!!))
        }

        private fun generateUpdateFunction(className: ClassName, properties: List<KSPropertyDeclaration>): FunSpec {
            val tableName = "PlayerTable"

            val primaryKeyProperty = properties.find { it.simpleName.asString() == "uuid" }
                ?: properties.firstOrNull()

            if (primaryKeyProperty == null) {
                logger.error("No primary key property found")
                throw IllegalStateException("No primary key property found")
            }

            val primaryKeyName = primaryKeyProperty.simpleName.asString()

            return FunSpec.builder("update")
                .receiver(className)
                .addModifiers(KModifier.SUSPEND)
                .addCode(
                    """
                    newSuspendedTransaction {
                        PlayerDataEntity.findSingleByAndUpdate($tableName.$primaryKeyName eq this@update.$primaryKeyName) {
                            ${properties.joinToString("\n                            ") { 
                                "it.${it.simpleName.asString()} = ${it.simpleName.asString()}" 
                            }}
                        }
                    }
                    """.trimIndent()
                )
                .build()
        }
    }
}

class UpdateFunctionProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return UpdateFunctionProcessor(environment.codeGenerator, environment.logger)
    }
}
