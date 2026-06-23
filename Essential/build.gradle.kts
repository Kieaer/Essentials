import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin.Companion.shadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
    `java-library`
}

dependencies {
    compileOnly(rootProject.libs.bundles.game)
    ksp(project(":ksp-processor"))
    implementation(project(":ksp-processor"))
    implementation(libs.bundles.kotlinxEcosystem)
    implementation(libs.bundles.exposed)
    implementation(libs.bundles.r2dbc.drivers)
    implementation(libs.bundles.ktor)
    implementation(libs.jfiglet)
    implementation(libs.maven.check)
    implementation(libs.kaml)
    implementation(libs.jbcrypt)

    implementation(libs.proguard)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.game.test)
    testImplementation(libs.bundles.kotlinxEcosystem)
    testImplementation(libs.bundles.exposed)
    testImplementation(libs.bundles.r2dbc.drivers)
    testImplementation(libs.bundles.testcontainers)
    testImplementation(libs.jbcrypt)
}


tasks.register<JavaExec>("proguardJar") {
    val shadowJarFile = tasks.shadowJar.get().archiveFile.get().asFile
    val outputFile = File(buildDir, "libs/${project.name}-proguard.jar")

    // Make sure the output directory exists
    doFirst {
        outputFile.parentFile.mkdirs()
    }

    // Configure the JavaExec task
    mainClass.set("proguard.ProGuard")
    classpath = configurations.runtimeClasspath.get()

    // ProGuard arguments
    val argsList = mutableListOf(
        "-injars", shadowJarFile.absolutePath,
        "-outjars", outputFile.absolutePath,
        "-printmapping", "${buildDir}/proguard/mapping.txt",
        "-printseeds", "${buildDir}/proguard/seeds.txt",
        "-printusage", "${buildDir}/proguard/usage.txt",
        "@${rootProject.projectDir}/proguard-rules.pro"
    )

    // Provide ALL compile+runtime classpath entries as library jars for analysis only
    // (exclude the fat shadow jar which is already passed as -injars)
    val runtimeFiles = configurations.runtimeClasspath.get().files
    val compileFiles = configurations.compileClasspath.get().files
    val allLibs = (runtimeFiles + compileFiles)
        .asSequence()
        .filter { it.exists() && it != shadowJarFile }
        .distinctBy { it.absolutePath }
        .toList()
    allLibs.forEach { file ->
        argsList.addAll(listOf("-libraryjars", file.absolutePath))
    }

    args = argsList
    doFirst {
        File("${buildDir}/proguard").mkdirs()
    }
}

tasks.test {
    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.SHORT
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
            css = css.lines().map { it.trim() }.joinToString("")
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