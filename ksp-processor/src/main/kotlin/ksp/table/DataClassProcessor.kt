package ksp.table

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo

class DataClassProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(GenerateDataClass::class.qualifiedName!!)
        val unprocessed = symbols.filter { !it.validate() }.toList()

        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(DataClassVisitor(), Unit) }

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
                .addType(generateDataClass(dataClassName, properties))
                .addFunction(generateToDataFunction(className, dataClassName, properties))
                .addFunction(generateFromDataFunction(className, dataClassName, properties))
                .build()

            fileSpec.writeTo(codeGenerator, Dependencies(true, classDeclaration.containingFile!!))
        }

        private fun generateDataClass(dataClassName: String, properties: List<KSPropertyDeclaration>): TypeSpec {
            val builder = TypeSpec.classBuilder(dataClassName)
                .addModifiers(KModifier.DATA)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .apply {
                            properties.forEachIndexed { index, property ->
                                val propertyName = property.simpleName.asString()
                                val propertyType = property.type.resolve()
                                val typeName = propertyType.toTypeName()

                                addParameter(propertyName, typeName)
                            }
                        }
                        .build()
                )

            properties.forEach { property ->
                val propertyName = property.simpleName.asString()
                val propertyType = property.type.resolve()
                val typeName = propertyType.toTypeName()

                builder.addProperty(
                    PropertySpec.builder(propertyName, typeName)
                        .initializer("%N", propertyName)
                        .build()
                )
            }

            return builder.build()
        }

        private fun generateToDataFunction(className: ClassName, dataClassName: String, properties: List<KSPropertyDeclaration>): FunSpec {
            return FunSpec.builder("toData")
                .receiver(className)
                .returns(ClassName(className.packageName, dataClassName))
                .addCode(
                    """
                    return $dataClassName(
                        ${properties.joinToString(",\n                        ") { 
                            "${it.simpleName.asString()} = ${it.simpleName.asString()}" 
                        }}
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
}

class DataClassProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return DataClassProcessor(environment.codeGenerator, environment.logger)
    }
}
