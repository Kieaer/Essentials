import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin.Companion.shadowJar
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
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

/**
 * Comma-separated optional modules to omit from an artifact.
 *
 * Examples:
 *   -PexcludeModules=web
 *   -PexcludeModules=discord,achievements
 *   -PexcludeModules=services
 */
val optionalModules = setOf(
    "achievements",
    "bridge",
    "chat",
    "contribution",
    "discord",
    "effect",
    "migration",
    "protect",
    "vote",
    "web",
)

val moduleAliases = mapOf(
    "achievement" to "achievements",
    "achievements" to "achievements",
    "core/service" to "services",
    "core/services" to "services",
    "db-migrate" to "migration",
    "db-migration" to "migration",
    "flyway" to "migration",
    "migrate" to "migration",
    "service" to "services",
    "services" to "services",
)

val excludedModules = providers.gradleProperty("excludeModules")
    .orNull
    .orEmpty()
    .split(',')
    .map(String::trim)
    .filter(String::isNotEmpty)
    .flatMap { requestedModule ->
        val normalized = requestedModule.lowercase()
        when (val module = moduleAliases[normalized] ?: normalized) {
            "services" -> optionalModules
            in optionalModules -> setOf(module)
            else -> throw GradleException(
                "Unknown module '$requestedModule'. Use one of: " +
                    "${(optionalModules + "services").sorted().joinToString(", ")}."
            )
        }
    }
    .toSortedSet()

val excludedSourcePatterns = buildList {
    excludedModules.forEach { module ->
        add("essential/core/service/$module/**")
    }
    if ("achievements" in excludedModules) {
        // The web achievement endpoint depends on the achievements service.
        add("essential/core/service/web/achievement/**")
    }
}

val excludedResourcePatterns = buildList {
    if ("web" in excludedModules) {
        add("web/**")
        add("bundles/web/**")
    }
    if ("achievements" in excludedModules) {
        add("bundles/achievements/**")
    }
    if ("migration" in excludedModules) {
        add("db/migration/**")
    }
}

val excludedTestSourcePatterns = buildList {
    excludedModules.forEach { module ->
        add("essential/core/service/$module/**")
    }
}

val excludedCodePrefixValues = excludedSourcePatterns
    .filter { it.endsWith("/**") }
    .map { it.removeSuffix("/**").replace('.', '/') }
val excludedResourcePrefixValues = excludedResourcePatterns
    .filter { it.endsWith("/**") }
    .map { it.removeSuffix("/**") }
val webModuleExcluded = "web" in excludedModules
val migrationModuleExcluded = "migration" in excludedModules
val excludedDependencyPrefixValues = buildList {
    if (migrationModuleExcluded) {
        add("org/flywaydb/")
        add("org/postgresql/")
        add("org/mariadb/jdbc/")
    }
}

if (excludedModules.isNotEmpty()) {
    logger.lifecycle("Building Essentials without optional modules: ${excludedModules.joinToString(", ")}")
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

abstract class VerifyModuleExclusionsTask : DefaultTask() {
    @get:InputFile
    abstract val archiveFile: RegularFileProperty

    @get:Input
    abstract val excludedCodePrefixes: ListProperty<String>

    @get:Input
    abstract val excludedResourcePrefixes: ListProperty<String>

    @get:Input
    abstract val excludedDependencyPrefixes: ListProperty<String>

    @get:Input
    abstract val excludesWeb: Property<Boolean>

    @TaskAction
    fun verify() {
        ZipFile(archiveFile.get().asFile).use { zip ->
            val entryNames = Collections.list(zip.entries()).mapTo(mutableSetOf()) { it.name }
            val excludedEntries = entryNames.filter { entry ->
                !entry.endsWith('/') && (excludedCodePrefixes.get().any(entry::startsWith) ||
                    excludedResourcePrefixes.get().any(entry::startsWith))
            }
            check(excludedEntries.isEmpty()) {
                "Excluded module code or resources are present in the shadow jar: ${excludedEntries.sorted().joinToString()}"
            }

            val excludedDependencies = entryNames.filter { entry ->
                !entry.endsWith('/') && excludedDependencyPrefixes.get().any(entry::startsWith)
            }
            check(excludedDependencies.isEmpty()) {
                "Excluded module libraries are present in the shadow jar: ${excludedDependencies.sorted().joinToString()}"
            }

            if (excludesWeb.get()) {
                val ktorEntries = entryNames.filter { it.startsWith("io/ktor/") }
                check(ktorEntries.isEmpty()) {
                    "Ktor web libraries are present although the web module is excluded: ${ktorEntries.sorted().joinToString()}"
                }
            }
        }
    }
}

abstract class VerifyProguardJarTask : DefaultTask() {
    @get:InputFile
    abstract val archiveFile: RegularFileProperty

    @get:Input
    abstract val requiredEntries: ListProperty<String>

    @get:Input
    abstract val excludedCodePrefixes: ListProperty<String>

    @get:Input
    abstract val excludedResourcePrefixes: ListProperty<String>

    @get:Input
    abstract val excludedDependencyPrefixes: ListProperty<String>

    @TaskAction
    fun verify() {
        val forbiddenEntries = setOf(
            "arc64.dll",
            "libarc64.so",
            "libarcarm64.so",
            "libarc64.dylib",
            "libarcarm64.dylib",
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
            "META-INF/rewrite/",
        )

        ZipFile(archiveFile.get().asFile).use { zip ->
            val entries = Collections.list(zip.entries())
            val duplicateEntries = entries.groupingBy { it.name }.eachCount().filterValues { it > 1 }.keys
            check(duplicateEntries.isEmpty()) {
                "Duplicate entries remain in the ProGuard jar: ${duplicateEntries.sorted().joinToString()}"
            }

            val entryNames = entries.mapTo(mutableSetOf()) { it.name }
            val missingRequired = requiredEntries.get().toSet() - entryNames
            check(missingRequired.isEmpty()) {
                "ProGuard removed required runtime entries: ${missingRequired.sorted().joinToString()}"
            }

            val leakedEntries = entryNames.filter { name ->
                name in forbiddenEntries || forbiddenPrefixes.any(name::startsWith)
            }
            check(leakedEntries.isEmpty()) {
                "Build-time or host-provided entries leaked into the ProGuard jar: ${leakedEntries.sorted().joinToString()}"
            }

            val excludedEntries = entryNames.filter { entry ->
                !entry.endsWith('/') && (excludedCodePrefixes.get().any(entry::startsWith) ||
                    excludedResourcePrefixes.get().any(entry::startsWith))
            }
            check(excludedEntries.isEmpty()) {
                "Excluded module code or resources are present in the ProGuard jar: ${excludedEntries.sorted().joinToString()}"
            }

            val excludedDependencies = entryNames.filter { entry ->
                !entry.endsWith('/') && excludedDependencyPrefixes.get().any(entry::startsWith)
            }
            check(excludedDependencies.isEmpty()) {
                "Excluded module libraries are present in the ProGuard jar: ${excludedDependencies.sorted().joinToString()}"
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
    if (!migrationModuleExcluded) {
        implementation(libs.bundles.flyway)
    }
    if ("web" !in excludedModules) {
        implementation(libs.bundles.ktor)
        implementation(libs.reactor.netty.core)
    }
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
            exclude(*excludedResourcePatterns.toTypedArray())
        }
    }
}

kotlin {
    sourceSets.named("main") {
        kotlin.exclude(*excludedSourcePatterns.toTypedArray())
    }
    sourceSets.named("test") {
        kotlin.exclude(*excludedTestSourcePatterns.toTypedArray())
    }
}


val proguardInputJar = tasks.shadowJar.flatMap { it.archiveFile }
val proguardOutputJar = layout.buildDirectory.file("libs/${project.name}-proguard.jar")
val proguardRulesFile = rootProject.layout.projectDirectory.file("proguard-rules.pro")
val proguardOutputDirectory = proguardOutputJar.get().asFile.parentFile
val proguardLibraryFiles = proguardLibraries.files
    .filter { it.exists() }
    .distinctBy { it.absolutePath }
val proguardArguments = buildList {
    addAll(listOf(
        "-injars", proguardInputJar.get().asFile.absolutePath,
        "-outjars", proguardOutputJar.get().asFile.absolutePath,
        "-printmapping", File(proguardOutputDirectory, "${project.name}-proguard-mapping.txt").absolutePath,
        "-printseeds", File(proguardOutputDirectory, "${project.name}-proguard-seeds.txt").absolutePath,
        "-printusage", File(proguardOutputDirectory, "${project.name}-proguard-usage.txt").absolutePath,
        "@${proguardRulesFile.asFile.absolutePath}",
    ))
    proguardLibraryFiles.forEach { library -> addAll(listOf("-libraryjars", library.absolutePath)) }
}
val requiredProguardEntries = buildList {
    addAll(listOf(
        "plugin.json",
        "essential/core/Main.class",
        "META-INF/services/java.sql.Driver",
        "org/h2/Driver.class",
    ))
    if (!migrationModuleExcluded) {
        addAll(listOf(
            "essential/core/service/migration/FlywayMigration.class",
            "META-INF/services/org.flywaydb.core.extensibility.Plugin",
            "org/flywaydb/core/Flyway.class",
            "org/flywaydb/core/internal/database/h2/H2DatabaseType.class",
            "org/flywaydb/database/postgresql/PostgreSQLDatabaseType.class",
            "org/flywaydb/database/mysql/MySQLDatabaseType.class",
            "org/flywaydb/database/mysql/mariadb/MariaDBDatabaseType.class",
            "org/postgresql/Driver.class",
            "org/mariadb/jdbc/Driver.class",
        ))
    }
    if (!webModuleExcluded) add("essential/core/service/web/auth/UserSession\$\$serializer.class")
}

val verifyProguardJar = tasks.register<VerifyProguardJarTask>("verifyProguardJar") {
    group = "verification"
    description = "Verifies the ProGuard jar's runtime entries and modular exclusions."
    archiveFile.set(proguardOutputJar)
    requiredEntries.set(requiredProguardEntries)
    excludedCodePrefixes.set(excludedCodePrefixValues)
    excludedResourcePrefixes.set(excludedResourcePrefixValues)
    excludedDependencyPrefixes.set(excludedDependencyPrefixValues)
}

tasks.register<JavaExec>("proguardJar") {
    group = "build"
    description = "Shrinks the standalone Essentials jar and verifies reflective runtime resources."
    dependsOn(tasks.shadowJar)
    finalizedBy(verifyProguardJar)

    inputs.file(proguardInputJar)
    inputs.file(proguardRulesFile)
    inputs.files(proguardLibraries)
    outputs.file(proguardOutputJar)

    mainClass.set("proguard.ProGuard")
    classpath = proguard
    args = proguardArguments
}

val shadowArchive = tasks.shadowJar.flatMap { it.archiveFile }

tasks.register<VerifyModuleExclusionsTask>("verifyModuleExclusions") {
    group = "verification"
    description = "Verifies that a modular shadow jar contains no excluded code or resources."
    dependsOn(tasks.shadowJar)
    archiveFile.set(shadowArchive)
    excludedCodePrefixes.set(excludedCodePrefixValues)
    excludedResourcePrefixes.set(excludedResourcePrefixValues)
    excludedDependencyPrefixes.set(excludedDependencyPrefixValues)
    excludesWeb.set(webModuleExcluded)
}

tasks.test {
    if (excludedModules.isNotEmpty()) {
        include("**/ModularPluginSmokeTest.class")
    }
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

        fun lastNonWs(sb: StringBuilder): Char? {
            for (idx in sb.length - 1 downTo 0) {
                if (!sb[idx].isWhitespace()) return sb[idx]
            }
            return null
        }
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
