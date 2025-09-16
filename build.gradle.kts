import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin.Companion.shadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.shadowJar) apply false
    `maven-publish`
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "maven-publish")

    if (project.name != "ksp-processor") {
        dependencies {
            if (System.getenv("LOCAL_REPO_IP") != null) {
                compileOnly(rootProject.libs.bundles.local.game)
            } else {
                compileOnly(rootProject.libs.bundles.game)
            }
        }
    }

    // Add ProGuard as a dependency
    dependencies {
        implementation("com.guardsquare:proguard-base:7.7.0")
    }

    tasks.shadowJar {
        exclude("arc/**")
        exclude("mindustry/**")
        exclude("server/**")

        if (project.name != "EssentialWeb") {
            exclude("kotlin/reflect/**")
        }

        from(project.configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
            exclude("META-INF/*.SF")
            exclude("META-INF/*.DSA")
            exclude("META-INF/*.RSA")


            include("org/sqlite/native/Linux/x86_64/**")
            include("org/sqlite/native/Linux/x86/**")
            include("org/sqlite/native/Windows/x86/**")
            include("org/sqlite/native/Windows/x86_64/**")

            exclude("org/sqlite/native/Mac/**")
            exclude("org/sqlite/native/Linux/aarch64/**")
            exclude("org/sqlite/native/Windows/arm64/**")
            
            // Ensure Ktor classes are included
            include("io/ktor/**")
            include("kotlin/jvm/functions/**")
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(rootDir) {
            include("plugin.json")
        }
    }

    if (project.name != "ksp-processor") {
        tasks.register<JavaExec>("proguardJar") {
            dependsOn("shadowJar")

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

            // Add Java runtime libraries based on Java version
            val javaHome = System.getProperty("java.home")

            val jmods = File("$javaHome/jmods")
            if (jmods.exists()) {
                // Add java.base module (contains core classes)
                argsList.add("-libraryjars")
                argsList.add("$javaHome/jmods/java.base.jmod(!**.jar;!module-info.class)")

                // Add java.sql module (contains SQL-related classes)
                if (File("$javaHome/jmods/java.sql.jmod").exists()) {
                    argsList.add("-libraryjars")
                    argsList.add("$javaHome/jmods/java.sql.jmod(!**.jar;!module-info.class)")
                }

                // Add java.desktop module (contains AWT/Swing classes that might be referenced)
                if (File("$javaHome/jmods/java.desktop.jmod").exists()) {
                    argsList.add("-libraryjars")
                    argsList.add("$javaHome/jmods/java.desktop.jmod(!**.jar;!module-info.class)")
                }
            }

            args = argsList

            // Create output directories
            doFirst {
                File("${buildDir}/proguard").mkdirs()
            }
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "essential"
                artifactId = project.name.lowercase()
                version = "1.0.0"

                from(components["java"])
            }
        }
    }
}
