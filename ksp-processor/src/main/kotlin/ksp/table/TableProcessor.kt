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

                processDataClass(classDeclaration)
            }

        return unprocessed
    }

    private fun processDataClass(classDeclaration: KSClassDeclaration) {
        if (!classDeclaration.modifiers.contains(Modifier.DATA)) {
            logger.warn("@GenerateCode can only be applied to data classes: ${classDeclaration.simpleName.asString()}")
            return
        }

        val className = classDeclaration.simpleName.asString()
        val packageName = classDeclaration.packageName.asString()
        val tableClassName = className.replace("Data", "Table")

        val primaryConstructor = classDeclaration.primaryConstructor
        if (primaryConstructor == null) {
            logger.error("Data class ${classDeclaration.simpleName.asString()} does not have a primary constructor")
            return
        }

        val constructorParams = primaryConstructor.parameters
        val constructorParamNames = constructorParams.map { it.name?.asString() ?: "" }.toSet()

        val properties = classDeclaration.getAllProperties()
            .filter { property -> constructorParamNames.contains(property.simpleName.asString()) }
            .toList()

        val db = classDeclaration.annotations.find { it.shortName.asString() == "GenerateCode" }
            ?.arguments
            ?.find { it.name?.asString() == "db" }
            ?.value
            ?.toString()
            ?.removeSurrounding("\"", "'")
            ?: ""

        val fileSpec = createFileSpec(packageName, className, tableClassName, properties, db)

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
        properties: List<KSPropertyDeclaration>,
        db: String
    ): String {
        val sb = StringBuilder()

        sb.append("package $packageName\n\n")
        sb.append("import org.jetbrains.exposed.v1.core.ResultRow\n")
        sb.append("import org.jetbrains.exposed.v1.r2dbc.Query\n")
        sb.append("import org.jetbrains.exposed.v1.r2dbc.selectAll\n")
        sb.append("import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction\n")
        sb.append("import org.jetbrains.exposed.v1.r2dbc.update\n")
        sb.append("import essential.common.database.table.$tableClassName\n")
        sb.append("import $packageName.$className\n")
        if (db.isNotEmpty()) {
            sb.append("import essential.common.database.$db\n")
        }
        sb.append("import kotlinx.serialization.json.Json\n")
        sb.append("import kotlinx.serialization.encodeToString\n")
        sb.append("import kotlinx.serialization.decodeFromString\n")
        sb.append("import kotlinx.coroutines.flow.*\n")
        sb.append("import org.jetbrains.exposed.v1.core.eq\n")
        sb.append("import kotlin.time.ExperimentalTime\n\n")
        sb.append("private val generatedJson = Json { ignoreUnknownKeys = true; isLenient = true }\n\n")

        fun isSimpleType(typeDecl: KSClassDeclaration?): Boolean {
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

        fun isSerializableType(typeDecl: KSClassDeclaration?): Boolean {
            if (typeDecl == null) return false
            return typeDecl.annotations.any {
                val name = it.shortName.asString()
                name == "Serializable" || it.annotationType.resolve().declaration.qualifiedName?.asString() == "kotlinx.serialization.Serializable"
            }
        }

        // toData extension for Table
        sb.append("/**\n")
        sb.append(" * Converts a ResultRow to a $className instance.\n")
        sb.append(" * This function is generated automatically by the @GenerateCode annotation.\n")
        sb.append(" */\n")
          sb.append("@OptIn(ExperimentalTime::class)\n")
        sb.append("fun $tableClassName.toData(row: ResultRow): $className {\n")

        properties.forEach { property ->
            val propertyName = property.simpleName.asString()
            val typeDecl = property.type.resolve().declaration as? KSClassDeclaration
            val needsJson = !isSimpleType(typeDecl) && isSerializableType(typeDecl)
            if (needsJson) {
                val typeName = typeDecl!!.simpleName.asString()
                sb.append("    val $propertyName = generatedJson.decodeFromString<$typeName>(row[$tableClassName.$propertyName])\n")
            } else {
                sb.append("    val $propertyName = row[$tableClassName.$propertyName]\n")
            }
        }

        sb.append("\n    return $className(\n")

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

        // ResultRow.toClassName()
        sb.append("/**\n")
        sb.append(" * Converts a ResultRow to a $className instance.\n")
        sb.append(" * This is a convenience method that uses the table's toData method.\n")
        sb.append(" * This function is generated automatically by the @GenerateCode annotation.\n")
        sb.append(" */\n")
        sb.append("fun ResultRow.to$className(): $className {\n")
        sb.append("    return $tableClassName.toData(this)\n")
        sb.append("}\n\n")

        // mapToClassNameList
        sb.append("/**\n")
        sb.append(" * Maps query results to a list of $className instances.\n")
        sb.append(" * This is a convenience method for the table class.\n")
        sb.append(" * This function is generated automatically by the @GenerateCode annotation.\n")
        sb.append(" */\n")
        sb.append("suspend fun Query.mapTo${className}List(): List<$className> {\n")
        sb.append("    return this.map { $tableClassName.toData(it) }.toList()\n")
        sb.append("}\n\n")

        // fromInsertReturning
        sb.append("/**\n")
        sb.append(" * Creates a $className instance from the ID returned by insertReturning.\n")
        sb.append(" * This function is generated automatically by the @GenerateCode annotation.\n")
        sb.append(" */\n")
        sb.append("suspend fun <T> $tableClassName.fromInsertReturning(id: T): $className {\n")
        if (db.isNotEmpty()) {
            sb.append("    return suspendTransaction(db = $db) {\n")
        } else {
            sb.append("    return suspendTransaction {\n")
        }
        sb.append("        val query = $tableClassName.selectAll()\n")
        sb.append("        when (id) {\n")
        sb.append("            is Int -> query.where { $tableClassName.id eq id.toUInt() }\n")
        sb.append("            is UInt -> query.where { $tableClassName.id eq id }\n")
        sb.append("            else -> throw IllegalArgumentException(\"Unsupported ID type: \${id!!::class.java}\")\n")
        sb.append("        }\n")
        sb.append("        query.map { row -> row.to$className() }.first()\n")
        sb.append("    }\n")
        sb.append("}\n\n")

        // update
        sb.append("/**\n")
        sb.append(" * Updates a database record using a $className instance.\n")
        sb.append(" * This function is generated automatically by the @GenerateCode annotation.\n")
        sb.append(" */\n")
        sb.append("@OptIn(ExperimentalTime::class)")
        sb.append("suspend fun $className.update(): Boolean {\n")
        sb.append("    val data = this\n")
        if (db.isNotEmpty()) {
            sb.append("    return suspendTransaction(db = $db) {\n")
        } else {
            sb.append("    return suspendTransaction {\n")
        }
        sb.append("        $tableClassName.update({ $tableClassName.id eq data.id }) {\n")

        properties.filter { it.simpleName.asString() != "id" }.forEach { property ->
            val propertyName = property.simpleName.asString()
            val typeDecl = property.type.resolve().declaration as? KSClassDeclaration
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
