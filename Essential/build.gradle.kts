import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin.Companion.shadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import java.util.Collections
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
    `java-library`
    jacoco
}

val mindustryAssets: Configuration by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val proguard by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

val proguardLibraries by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
}

configurations.configureEach {
    exclude(group = "com.github.waffle", module = "waffle-jna")
}

dependencies {
    mindustryAssets("Anuken:Mindustry:${rootProject.libs.versions.mindustry.get()}:assets@jar")
    compileOnly(rootProject.libs.bundles.game)
    ksp(project(":ksp-processor"))
    implementation(project(":ksp-processor"))
    implementation(kotlin("reflect"))
    implementation(libs.bundles.kotlinxEcosystem)
    implementation(libs.bundles.exposed)
    implementation(libs.bundles.r2dbc.drivers)
    implementation(libs.bundles.flyway)
    implementation(libs.bundles.ktor)
    implementation(libs.reactor.netty.core)
    implementation(libs.jfiglet)
    implementation(libs.maven.check)
    implementation(libs.kaml)
    implementation(libs.jbcrypt)

    proguard(libs.proguard)
    proguardLibraries(rootProject.libs.bundles.game)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.game.test)
    testImplementation(libs.bundles.kotlinxEcosystem)
    testImplementation(libs.bundles.exposed)
    testImplementation(libs.bundles.r2dbc.drivers)
    testImplementation(libs.bundles.flyway)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.jbcrypt)
}

abstract class ExtractMindustryBundlesTask @javax.inject.Inject constructor(
    private val archiveOperations: ArchiveOperations,
    private val fileSystemOperations: FileSystemOperations
) : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val assetsJar: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun extract() {
        val jarFiles = assetsJar.files
        if (jarFiles.isEmpty()) return

        fileSystemOperations.sync {
            jarFiles.forEach { jar ->
                from(archiveOperations.zipTree(jar)) {
                    include("**/bundle*.properties")
                    eachFile {
                        path = "bundles/mindustry/" + name
                    }
                    includeEmptyDirs = false
                }
            }
            into(outputDir)
        }
    }
}

val extractMindustryBundles by tasks.registering(ExtractMindustryBundlesTask::class) {
    assetsJar.from(mindustryAssets)
    outputDir.set(layout.buildDirectory.dir("generated/resources/mindustry-bundles"))
}

sourceSets {
    main {
        resources {
            srcDir(extractMindustryBundles)
        }
    }
}


tasks.register<JavaExec>("proguardJar") {
    group = "build"
    description = "Shrinks the standalone Essentials jar and verifies reflective runtime resources."
    notCompatibleWithConfigurationCache("Runs ProGuard and inspects the generated archive in task actions.")
    dependsOn(tasks.shadowJar)

    val inputJar = tasks.shadowJar.flatMap { it.archiveFile }
    val outputJar = layout.buildDirectory.file("libs/${project.name}-proguard.jar")
    val rulesFile = rootProject.layout.projectDirectory.file("proguard-rules.pro")

    inputs.file(inputJar)
    inputs.file(rulesFile)
    inputs.files(proguardLibraries)
    outputs.file(outputJar)

    mainClass.set("proguard.ProGuard")
    classpath = proguard

    doFirst {
        val outputFile = outputJar.get().asFile
        outputFile.parentFile.mkdirs()
        layout.buildDirectory.dir("proguard").get().asFile.mkdirs()

        val proguardArgs = mutableListOf(
            "-injars", inputJar.get().asFile.absolutePath,
            "-outjars", outputFile.absolutePath,
            "-printmapping", layout.buildDirectory.file("proguard/mapping.txt").get().asFile.absolutePath,
            "-printseeds", layout.buildDirectory.file("proguard/seeds.txt").get().asFile.absolutePath,
            "-printusage", layout.buildDirectory.file("proguard/usage.txt").get().asFile.absolutePath,
            "@${rulesFile.asFile.absolutePath}"
        )

        proguardLibraries.files
            .filter { it.exists() }
            .distinctBy { it.absolutePath }
            .forEach { proguardArgs.addAll(listOf("-libraryjars", it.absolutePath)) }

        args = proguardArgs
    }

    doLast {
        val outputFile = outputJar.get().asFile
        val requiredEntries = setOf(
            "plugin.json",
            "essential/core/Main.class",
            "db/migration/V6__update_apm_achievements.sql",
            "META-INF/services/org.flywaydb.core.extensibility.Plugin",
            "META-INF/services/java.sql.Driver",
            "org/flywaydb/core/Flyway.class",
            "org/flywaydb/core/internal/database/h2/H2DatabaseType.class",
            "org/flywaydb/database/postgresql/PostgreSQLDatabaseType.class",
            "org/flywaydb/database/mysql/MySQLDatabaseType.class",
            "org/flywaydb/database/mysql/mariadb/MariaDBDatabaseType.class",
            "org/h2/Driver.class",
            "org/postgresql/Driver.class",
            "org/mariadb/jdbc/Driver.class",
            "essential/core/service/web/auth/UserSession\$\$serializer.class"
        )
        val forbiddenEntries = setOf(
            "arc64.dll",
            "libarc64.so",
            "libarcarm64.so",
            "libarc64.dylib",
            "libarcarm64.dylib"
        )
        val forbiddenPrefixes = listOf(
            "arc/",
            "mindustry/",
            "server/",
            "proguard/",
            "com/guardsquare/proguard/",
            "com/google/devtools/ksp/",
            "com/squareup/kotlinpoet/",
            "com/squareup/javapoet/",
            "META-INF/proguard/",
            "META-INF/com.android.tools/",
            "META-INF/maven/",
            "META-INF/native-image/",
            "META-INF/rewrite/"
        )

        ZipFile(outputFile).use { zip ->
            val entries = Collections.list(zip.entries())
            val duplicateEntries = entries.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
            check(duplicateEntries.isEmpty()) {
                "Duplicate entries remain in the ProGuard jar: ${duplicateEntries.sorted().joinToString()}"
            }

            val entryNames = entries.mapTo(mutableSetOf()) { it.name }
            val missingRequired = requiredEntries - entryNames
            check(missingRequired.isEmpty()) {
                "ProGuard removed required runtime entries: ${missingRequired.sorted().joinToString()}"
            }

            val leakedEntries = entryNames.filter { name ->
                name in forbiddenEntries || forbiddenPrefixes.any(name::startsWith)
            }
            check(leakedEntries.isEmpty()) {
                "Build-time or host-provided entries leaked into the ProGuard jar: ${leakedEntries.sorted().joinToString()}"
            }

            val missingProviders = mutableListOf<String>()
            entries.asSequence()
                .filter { !it.isDirectory && it.name.startsWith("META-INF/services/") }
                .forEach { serviceEntry ->
                    zip.getInputStream(serviceEntry).bufferedReader().useLines { lines ->
                        lines.map { it.substringBefore('#').trim() }
                            .filter { it.isNotEmpty() }
                            .forEach { provider ->
                                val providerClass = provider.substringBefore(';').replace('.', '/') + ".class"
                                if (providerClass !in entryNames) {
                                    missingProviders += "${serviceEntry.name}: $provider"
                                }
                            }
                    }
                }
            check(missingProviders.isEmpty()) {
                "Service descriptors reference removed providers:\n${missingProviders.sorted().joinToString("\n")}"
            }
        }
    }
}

tasks.test {
    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = true
        showCauses = true
    }
    finalizedBy("jacocoTestReport")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
    }
}

tasks.processResources {
    doLast {
        val destDir = destinationDir

        // Last non-whitespace char already emitted (used to disambiguate regex vs division)
        fun lastNonWs(sb: StringBuilder): Char? {
            for (idx in sb.length - 1 downTo 0) {
                if (!sb[idx].isWhitespace()) return sb[idx]
            }
            return null
        }

        // A '/' starts a regex literal (not division) when the preceding significant token
        // is an operator, opener, separator, or the start of input.
        val regexAllowedChars = "([{,;:=+-*%&|^~!?<>"
        fun regexAllowed(prev: Char?): Boolean = prev == null || prev in regexAllowedChars

        fun minifyCss(content: String): String {
            var css = content.replace(Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL), "")
            css = css.lines().joinToString("") { it.trim() }
            css = css.replace(Regex("\\s*([{};:,])\\s*"), "$1")
            css = css.replace(Regex("\\s+"), " ")
            return css.trim()
        }

        fun minifyJsWhitespace(content: String): String {
            val sb = StringBuilder()
            var i = 0
            val len = content.length
            var state = "NORMAL"
            var inClass = false

            fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_' || c == '$'
            fun isSafeToJoin(c: Char): Boolean = c in ";{},[]()+-*/=><&|?:"
            
            while (i < len) {
                val c = content[i]
                val next = if (i + 1 < len) content[i + 1] else null
                
                when (state) {
                    "NORMAL" -> {
                        if (c == '\'') {
                            state = "SINGLE_QUOTE"
                            sb.append(c)
                        } else if (c == '"') {
                            state = "DOUBLE_QUOTE"
                            sb.append(c)
                        } else if (c == '`') {
                            state = "TEMPLATE_LITERAL"
                            sb.append(c)
                        } else if (c == '/') {
                            val prev = lastNonWs(sb)
                            sb.append(c)
                            if (regexAllowed(prev)) {
                                state = "REGEX"
                                inClass = false
                            }
                        } else if (c.isWhitespace()) {
                            if (c == '\n' || c == '\r') {
                                var lastNonWs: Char? = null
                                for (idx in sb.length - 1 downTo 0) {
                                    if (!sb[idx].isWhitespace()) {
                                        lastNonWs = sb[idx]
                                        break
                                    }
                                }
                                var nextNonWs: Char? = null
                                for (idx in i + 1 until len) {
                                    if (!content[idx].isWhitespace()) {
                                        nextNonWs = content[idx]
                                        break
                                    }
                                }
                                val lastSafe = lastNonWs != null && isSafeToJoin(lastNonWs)
                                val nextSafe = nextNonWs != null && isSafeToJoin(nextNonWs)
                                if (!lastSafe && !nextSafe) {
                                    sb.append('\n')
                                }
                            } else {
                                var lastNonWs: Char? = null
                                for (idx in sb.length - 1 downTo 0) {
                                    if (!sb[idx].isWhitespace()) {
                                        lastNonWs = sb[idx]
                                        break
                                    }
                                }
                                if (lastNonWs != null && isWordChar(lastNonWs) && next != null && isWordChar(next)) {
                                    sb.append(' ')
                                }
                            }
                        } else {
                            sb.append(c)
                        }
                    }
                    "SINGLE_QUOTE" -> {
                        sb.append(c)
                        if (c == '\\') {
                            if (next != null) {
                                sb.append(next)
                                i++
                            }
                        } else if (c == '\'') {
                            state = "NORMAL"
                        }
                    }
                    "DOUBLE_QUOTE" -> {
                        sb.append(c)
                        if (c == '\\') {
                            if (next != null) {
                                sb.append(next)
                                i++
                            }
                        } else if (c == '"') {
                            state = "NORMAL"
                        }
                    }
                    "TEMPLATE_LITERAL" -> {
                        sb.append(c)
                        if (c == '\\') {
                            if (next != null) {
                                sb.append(next)
                                i++
                            }
                        } else if (c == '`') {
                            state = "NORMAL"
                        }
                    }
                    "REGEX" -> {
                        sb.append(c)
                        if (c == '\\') {
                            if (next != null) {
                                sb.append(next)
                                i++
                            }
                        } else if (c == '[') {
                            inClass = true
                        } else if (c == ']') {
                            inClass = false
                        } else if (c == '/' && !inClass) {
                            state = "NORMAL"
                        }
                    }
                }
                i++
            }
            return sb.toString().trim()
        }

        fun minifyJs(content: String): String {
            val sb = StringBuilder()
            var i = 0
            val len = content.length
            var state = "NORMAL"
            var inClass = false

            while (i < len) {
                val c = content[i]
                val next = if (i + 1 < len) content[i + 1] else null
                
                when (state) {
                    "NORMAL" -> {
                        if (c == '/' && next == '/') {
                            state = "LINE_COMMENT"
                            i++
                        } else if (c == '/' && next == '*') {
                            state = "BLOCK_COMMENT"
                            i++
                        } else if (c == '/') {
                            val prev = lastNonWs(sb)
                            sb.append(c)
                            if (regexAllowed(prev)) {
                                state = "REGEX"
                                inClass = false
                            }
                        } else if (c == '\'') {
                            state = "SINGLE_QUOTE"
                            sb.append(c)
                        } else if (c == '"') {
                            state = "DOUBLE_QUOTE"
                            sb.append(c)
                        } else if (c == '`') {
                            state = "TEMPLATE_LITERAL"
                            sb.append(c)
                        } else {
                            sb.append(c)
                        }
                    }
                    "SINGLE_QUOTE" -> {
                        sb.append(c)
                        if (c == '\\') {
                            if (next != null) {
                                sb.append(next)
                                i++
                            }
                        } else if (c == '\'') {
                            state = "NORMAL"
                        }
                    }
                    "DOUBLE_QUOTE" -> {
                        sb.append(c)
                        if (c == '\\') {
                            if (next != null) {
                                sb.append(next)
                                i++
                            }
                        } else if (c == '"') {
                            state = "NORMAL"
                        }
                    }
                    "TEMPLATE_LITERAL" -> {
                        sb.append(c)
                        if (c == '\\') {
                            if (next != null) {
                                sb.append(next)
                                i++
                            }
                        } else if (c == '`') {
                            state = "NORMAL"
                        }
                    }
                    "REGEX" -> {
                        sb.append(c)
                        if (c == '\\') {
                            if (next != null) {
                                sb.append(next)
                                i++
                            }
                        } else if (c == '[') {
                            inClass = true
                        } else if (c == ']') {
                            inClass = false
                        } else if (c == '/' && !inClass) {
                            state = "NORMAL"
                        }
                    }
                    "LINE_COMMENT" -> {
                        if (c == '\n' || c == '\r') {
                            sb.append(c)
                            state = "NORMAL"
                        }
                    }
                    "BLOCK_COMMENT" -> {
                        if (c == '*' && next == '/') {
                            state = "NORMAL"
                            i++
                        }
                    }
                }
                i++
            }
            
            val codeWithoutComments = sb.toString()
            return minifyJsWhitespace(codeWithoutComments)
        }

        val webDir = File(destDir, "web")
        if (webDir.exists()) {
            val cssDir = File(webDir, "css")
            if (cssDir.exists()) {
                cssDir.listFiles()?.forEach { file ->
                    if (file.extension == "css" && !file.name.endsWith(".min.css")) {
                        val originalContent = file.readText()
                        val minifiedContent = minifyCss(originalContent)
                        file.writeText(minifiedContent)
                        logger.lifecycle("Minified CSS: ${file.name} (${originalContent.length} -> ${minifiedContent.length} bytes)")
                    }
                }
            }
            val jsDir = File(webDir, "js")
            if (jsDir.exists()) {
                jsDir.listFiles()?.forEach { file ->
                    if (file.extension == "js" && !file.name.endsWith(".min.js")) {
                        val originalContent = file.readText()
                        val minifiedContent = minifyJs(originalContent)
                        file.writeText(minifiedContent)
                        logger.lifecycle("Minified JS: ${file.name} (${originalContent.length} -> ${minifiedContent.length} bytes)")
                    }
                }
            }
        }
    }
}
