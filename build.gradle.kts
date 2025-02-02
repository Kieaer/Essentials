plugins {
    kotlin("jvm") version "2.1.10"
    kotlin("plugin.serialization") version "2.1.10"
    kotlin("kapt") version "2.1.10"
}

repositories {
    mavenCentral()
}

val exposedVersion = "0.58.0"
val sqliteVersion = "3.48.0.0"
val serializationVersion = "1.8.0"
val hikariVersion = "6.2.1"

dependencies {
    compileOnly(fileTree("lib"))
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-json:$exposedVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:$serializationVersion")
    implementation("org.xerial:sqlite-jdbc:$sqliteVersion")

    implementation("com.zaxxer:HikariCP:$hikariVersion")

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
    /*doLast {
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
    }*/
}