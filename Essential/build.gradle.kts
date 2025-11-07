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