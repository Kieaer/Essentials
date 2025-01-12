import java.io.BufferedWriter
import java.io.OutputStreamWriter
import java.lang.Thread.sleep
import java.net.Socket

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "2.1.0"
    kotlin("kapt") version "2.1.0"
}

repositories {
    mavenCentral()
}

val exposedVersion = "0.57.0"
val sqliteVersion = "3.47.1.0"
val serializationVersion = "1.7.3"

dependencies {
    compileOnly(fileTree("lib"))
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")
    implementation("org.reflections:reflections:0.10.2")


    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    doLast {
        Socket("localhost", 6859).use { socket ->
            BufferedWriter(OutputStreamWriter(socket.getOutputStream())).use {
                it.write("exit")
                it.newLine()
            }
        }.apply {
            sleep(1500)
            File("E:\\Github\\Remake\\untitled\\build\\libs\\untitled.jar").copyTo(
                File("E:\\민더\\config\\mods\\Essentials.jar"),
                true
            )
        }
    }
}