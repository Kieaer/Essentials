package ksp.table

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.google.devtools.ksp.validate

class TableProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(GenerateCode::class.qualifiedName!!)
        val unprocessed = symbols.filter { !it.validate() }.toList()

        symbols
            .filter { it is KSClassDeclaration && it.validate() }
            .forEach {
                val classDeclaration = it as KSClassDeclaration
                val annotation = classDeclaration.annotations.find { it ->
                    it.shortName.asString() == "GenerateTable"
                }

                // Process data classes annotated with @GenerateCode
                processDataClass(classDeclaration)
            }

        return unprocessed
    }

    private fun processDataClass(classDeclaration: KSClassDeclaration) {
        // Check if it's a data class
        if (!classDeclaration.modifiers.contains(Modifier.DATA)) {
            logger.warn("@GenerateCode can only be applied to data classes: ${classDeclaration.simpleName.asString()}")
            return
        }

        val className = classDeclaration.simpleName.asString()
        val packageName = classDeclaration.packageName.asString()

        // Generate extension function for converting ResultRow to data class
        val tableClassName = className.replace("Data", "Table")

        // Get only the primary constructor parameters of the data class
        val primaryConstructor = classDeclaration.primaryConstructor
        if (primaryConstructor == null) {
            logger.error("Data class ${classDeclaration.simpleName.asString()} does not have a primary constructor")
            return
        }

        val constructorParams = primaryConstructor.parameters
        val constructorParamNames = constructorParams.map { it.name?.asString() ?: "" }.toSet()

        // Filter properties to only include those that are in the primary constructor
        val properties = classDeclaration.getAllProperties()
            .filter { property -> constructorParamNames.contains(property.simpleName.asString()) }
            .toList()

        // Generate the code
        val fileSpec = createFileSpec(packageName, className, tableClassName, properties)

        // Write the generated code to a file
        val fileName = "${className}Extensions"
        val dependencies = Dependencies(false, classDeclaration.containingFile!!)

        codeGenerator.createNewFile(
            dependencies,
            packageName,
            fileName
        ).use { outputStream ->
            outputStream.write(fileSpec.toByteArray())
        }
    }

    private fun createFileSpec(
        packageName: String,
        className: String,
        tableClassName: String,
        properties: List<KSPropertyDeclaration>
    ): String {
        val sb = StringBuilder()

        // Package declaration
        sb.append("package $packageName\n\n")

        // Imports
        sb.append("import org.jetbrains.exposed.v1.core.ResultRow\n")
        sb.append("import org.jetbrains.exposed.v1.core.eq\n")
        sb.append("import org.jetbrains.exposed.v1.r2dbc.Query\n")
        sb.append("import org.jetbrains.exposed.v1.r2dbc.selectAll\n")
        sb.append("import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction\n")
        sb.append("import org.jetbrains.exposed.v1.r2dbc.update\n")
        sb.append("import essential.common.database.table.$tableClassName\n")
        sb.append("import $packageName.$className\n")
        sb.append("import kotlinx.serialization.json.Json\n")
        sb.append("import kotlinx.serialization.encodeToString\n")
        sb.append("import kotlinx.serialization.decodeFromString\n")
        sb.append("import kotlinx.coroutines.flow.first\n")
        sb.append("import kotlinx.coroutines.flow.map\n")
        sb.append("import kotlinx.coroutines.flow.toList\n\n")

        // Helper: set of primitive/simple types that don't need JSON conversion
        fun isSimpleType(typeDecl: com.google.devtools.ksp.symbol.KSClassDeclaration?): Boolean {
            val qn = typeDecl?.qualifiedName?.asString() ?: return true
            return qn in setOf(
                "kotlin.Int", "kotlin.UInt", "kotlin.Short", "kotlin.UShort",
                "kotlin.Byte", "kotlin.UByte", "kotlin.Long", "kotlin.ULong",
                "kotlin.Float", "kotlin.Double", "kotlin.Boolean", "kotlin.String",
                "kotlin.Char", "kotlin.UInt?", "kotlin.UByte?",
                "kotlin.collections.List", "kotlin.collections.Set", "kotlin.collections.Map",
                "kotlin.Array",
                "kotlinx.datetime.LocalDateTime"
            )
        }

        fun isSerializableType(typeDecl: com.google.devtools.ksp.symbol.KSClassDeclaration?): Boolean {
            if (typeDecl == null) return false
            return typeDecl.annotations.any {
                val name = it.shortName.asString()
                name == "Serializable" || it.annotationType.resolve().declaration.qualifiedName?.asString() == "kotlinx.serialization.Serializable"
            }
        }

        // Extension function for Table
        sb.append("/**\n")
        sb.append(" * Converts a ResultRow to a $className instance.\n")
        sb.append(" * This function is generated automatically by the @GenerateCode annotation.\n")
        sb.append(" */\n")
        sb.append("fun $tableClassName.toData(row: ResultRow): $className {\n")

        // Create variables for each property
        properties.forEach { property ->
            val propertyName = property.simpleName.asString()
            val typeDecl = property.type.resolve().declaration as? com.google.devtools.ksp.symbol.KSClassDeclaration
            val needsJson = !isSimpleType(typeDecl) && isSerializableType(typeDecl)
            if (needsJson) {
                val typeName = typeDecl!!.simpleName.asString()
                sb.append("    val $propertyName = Json.decodeFromString<$typeName>(row[$tableClassName.$propertyName])\n")
            } else {
                sb.append("    val $propertyName = row[$tableClassName.$propertyName]\n")
            }
        }

        sb.append("\n    return $className(\n")

        // Add properties
        properties.forEachIndexed { index, property ->
            val propertyName = property.simpleName.asString()
            sb.append("        $propertyName = $propertyName")
            if (index < properties.size - 1) {
                sb.append(",")
            }
            sb.append("\n")
        }

        sb.append("    )\n")
        sb.append("}\n\n")

        // Extension function for ResultRow
        sb.append("/**\n")
        sb.append(" * Converts a ResultRow to a $className instance.\n")
        sb.append(" * This is a convenience method that uses the table's toData method.\n")
        sb.append(" * This function is generated automatically by the @GenerateCode annotation.\n")
        sb.append(" */\n")
        sb.append("fun ResultRow.to$className(): $className {\n")
        sb.append("    return $tableClassName.toData(this)\n")
        sb.append("}\n\n")

        // Extension function for mapping query results
        sb.append("/**\n")
        sb.append(" * Maps query results to a list of $className instances.\n")
        sb.append(" * This is a convenience method for the table class.\n")
        sb.append(" * This function is generated automatically by the @GenerateCode annotation.\n")
        sb.append(" */\n")
        sb.append("suspend fun Query.mapTo${className}List(): List<$className> {\n")
        sb.append("    return this.map { $tableClassName.toData(it) }.toList()\n")
        sb.append("}\n\n")

        // Function for mapping insertReturning result
        sb.append("/**\n")
        sb.append(" * Creates a $className instance from the ID returned by insertReturning.\n")
        sb.append(" * This function is generated automatically by the @GenerateCode annotation.\n")
        sb.append(" */\n")
        sb.append("suspend fun <T> $tableClassName.fromInsertReturning(id: T): $className {\n")
        sb.append("    return suspendTransaction {\n")
        sb.append("        val query = $tableClassName.selectAll()\n")
        sb.append("        when (id) {\n")
        sb.append("            is Int -> query.where { $tableClassName.id eq id.toUInt() }\n")
        sb.append("            is UInt -> query.where { $tableClassName.id eq id }\n")
        sb.append("            else -> throw IllegalArgumentException(\"Unsupported ID type: \${id!!::class.java}\")\n")
        sb.append("        }\n")
        sb.append("        query.map { row -> row.to$className() }.first()\n")
        sb.append("    }\n")
        sb.append("}\n\n")

        // Function for updating a record using a data class instance
        sb.append("/**\n")
        sb.append(" * Updates a database record using a $className instance.\n")
        sb.append(" * This function is generated automatically by the @GenerateCode annotation.\n")
        sb.append(" */\n")
        sb.append("suspend fun $className.update(): Boolean {\n")
        sb.append("    val data = this\n")
        sb.append("    return suspendTransaction {\n")
        sb.append("        $tableClassName.update({ $tableClassName.id eq data.id }) {\n")

        // Skip the id field as it's the primary key and shouldn't be updated
        properties.filter { it.simpleName.asString() != "id" }.forEach { property ->
            val propertyName = property.simpleName.asString()
            val typeDecl = property.type.resolve().declaration as? com.google.devtools.ksp.symbol.KSClassDeclaration
            val needsJson = !isSimpleType(typeDecl) && isSerializableType(typeDecl)
            if (needsJson) {
                sb.append("            it[$tableClassName.$propertyName] = Json.encodeToString(data.$propertyName)\n")
            } else {
                sb.append("            it[$tableClassName.$propertyName] = data.$propertyName\n")
            }
        }

        sb.append("        } > 0\n")
        sb.append("    }\n")
        sb.append("}\n")

        return sb.toString()
    }
}

class TableProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return TableProcessor(environment.codeGenerator, environment.logger)
    }
}
