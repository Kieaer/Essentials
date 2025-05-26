package ksp.table

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy

class TableProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(GenerateTable::class.qualifiedName!!)
        val unprocessed = symbols.filter { !it.validate() }.toList()

        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { 
                val classDeclaration = it as KSClassDeclaration
                val annotation = classDeclaration.annotations.find { it ->
                    it.shortName.asString() == "GenerateTable"
                }

                if (annotation != null) {
                    it.accept(DataClassVisitor(), Unit)
                    it.accept(InterfaceVisitor(), Unit)
                    it.accept(UpdateFunctionVisitor(), Unit)
                    it.accept(AdapterVisitor(), Unit)
                }
            }

        return unprocessed
    }

    inner class DataClassVisitor : KSVisitorVoid() {
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
            val dataClassName = className.simpleName.replace("Entity", "")

            val fileSpec = FileSpec.builder(packageName, dataClassName)
                .addImport(className.packageName.replace("entity", ""), dataClassName)
                .addFunction(generateToDataFunction(className, dataClassName))
                .addFunction(generateFromDataFunction(className, dataClassName, properties))
                .build()

            fileSpec.writeTo(codeGenerator, Dependencies(true, classDeclaration.containingFile!!))
        }

        private fun generateToDataFunction(className: ClassName, dataClassName: String): FunSpec {
            return FunSpec.builder("toData")
                .receiver(className)
                .returns(ClassName(className.packageName, dataClassName))
                .addCode(
                    """
                    return $dataClassName(
                        entity = this
                    )
                    """.trimIndent()
                )
                .build()
        }

        private fun generateFromDataFunction(className: ClassName, dataClassName: String, properties: List<KSPropertyDeclaration>): FunSpec {
            return FunSpec.builder("toEntity")
                .receiver(ClassName(className.packageName, dataClassName))
                .returns(className)
                .addParameter("entity", className)
                .addCode(
                    """
                    ${properties.joinToString("\n        ") { 
                        "entity.${it.simpleName.asString()} = ${it.simpleName.asString()}" 
                    }}
                    return entity
                    """.trimIndent()
                )
                .build()
        }
    }

    inner class InterfaceVisitor : KSVisitorVoid() {
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
            val interfaceName = "I" + className.simpleName.replace("Entity", "")

            val fileSpec = FileSpec.builder(packageName, interfaceName)
                .addImport("org.jetbrains.exposed.dao.id", "EntityID")
                .addType(generateInterface(interfaceName, properties))
                .build()

            fileSpec.writeTo(codeGenerator, Dependencies(true, classDeclaration.containingFile!!))
        }

        private fun generateInterface(interfaceName: String, properties: List<KSPropertyDeclaration>): TypeSpec {
            val builder = TypeSpec.interfaceBuilder(interfaceName)

            // Add id property to the interface
            builder.addProperty(
                PropertySpec.builder("id", 
                    ClassName("org.jetbrains.exposed.dao.id", "EntityID").parameterizedBy(
                        ClassName("kotlin", "UInt")
                    )
                )
                    .mutable(true)
                    .addModifiers(KModifier.ABSTRACT)
                    .build()
            )

            properties.forEach { property ->
                val propertyName = property.simpleName.asString()
                val propertyType = property.type.resolve()
                val typeName = propertyType.toTypeName()

                builder.addProperty(
                    PropertySpec.builder(propertyName, typeName)
                        .mutable(true) // Make all properties mutable (var)
                        .addModifiers(KModifier.ABSTRACT)
                        .build()
                )
            }

            return builder.build()
        }
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

    inner class AdapterVisitor : KSVisitorVoid() {
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
            val dataClassName = className.simpleName.replace("Entity", "")
            val interfaceName = "I$dataClassName"
            val adapterClassName = "${dataClassName}Adapter"

            // Generate the adapter in the same file as the data class
            val fileSpec = FileSpec.builder(packageName, "${dataClassName}Adapter")
                .addType(generateAdapterClass(adapterClassName, interfaceName, className, properties))
                .build()

            fileSpec.writeTo(codeGenerator, Dependencies(true, classDeclaration.containingFile!!))
        }

        private fun generateAdapterClass(adapterClassName: String, interfaceName: String, entityClassName: ClassName, properties: List<KSPropertyDeclaration>): TypeSpec {
            val builder = TypeSpec.classBuilder(adapterClassName)
                .addModifiers(KModifier.INTERNAL) // Change from PRIVATE to INTERNAL for visibility across files
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("entity", entityClassName)
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("entity", entityClassName)
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("entity")
                        .build()
                )
                .addSuperinterface(ClassName(entityClassName.packageName, interfaceName))

            // Add id property to the adapter
            builder.addProperty(
                PropertySpec.builder("id", 
                    ClassName("org.jetbrains.exposed.dao.id", "EntityID").parameterizedBy(
                        ClassName("kotlin", "UInt")
                    )
                )
                    .mutable(true) // Make property mutable to allow setter
                    .addModifiers(KModifier.OVERRIDE)
                    .getter(
                        FunSpec.getterBuilder()
                            .addCode("return entity.id")
                            .build()
                    )
                    .setter(
                        FunSpec.setterBuilder()
                            .addParameter("value", 
                                ClassName("org.jetbrains.exposed.dao.id", "EntityID").parameterizedBy(
                                    ClassName("kotlin", "UInt")
                                )
                            )
                            .addCode("// Note: id is typically immutable, this setter is provided for interface compatibility")
                            .build()
                    )
                    .build()
            )

            properties.forEach { property ->
                val propertyName = property.simpleName.asString()
                val propertyType = property.type.resolve()
                val typeName = propertyType.toTypeName()

                builder.addProperty(
                    PropertySpec.builder(propertyName, typeName)
                        .mutable(true) // Make property mutable to allow setter
                        .addModifiers(KModifier.OVERRIDE)
                        .getter(
                            FunSpec.getterBuilder()
                                .addCode("return entity.$propertyName")
                                .build()
                        )
                        .setter(
                            FunSpec.setterBuilder()
                                .addParameter("value", typeName)
                                .addCode("entity.$propertyName = value")
                                .build()
                        )
                        .build()
                )
            }

            return builder.build()
        }
    }
}

class TableProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return TableProcessor(environment.codeGenerator, environment.logger)
    }
}
