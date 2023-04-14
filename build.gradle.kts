import org.gradle.internal.os.OperatingSystem

plugins {
    kotlin("jvm") version "1.8.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

repositories {
    mavenCentral()
    maven(url = "https://www.jitpack.io")
}

dependencies {
    val exposedVersion = "0.41.1"
    val mindustryVersion = "v143"
    val arcVersion = "v143"

    compileOnly("com.github.anuken.arc:arc-core:$arcVersion")
    compileOnly("com.github.anuken.mindustryjitpack:core:$mindustryVersion")

    implementation("com.github.PersonTheCat:hjson-java:3.0.0-C11")
    implementation("de.svenkubiak:jBCrypt:0.4.3")
    implementation("com.mewna:catnip:3.3.5")
    //implementation("com.github.gimlet2:kottpd:0.2.1")
    implementation("org.apache.maven:maven-artifact:4.0.0-alpha-3")

    implementation("com.h2database:h2:2.1.214")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    implementation(files("libs/lingua.jar"))
    implementation("org.slf4j:slf4j-nop:2.0.6")

    testImplementation("com.github.anuken.arc:arc-core:$arcVersion")
    testImplementation("com.github.anuken.mindustryjitpack:core:$mindustryVersion")
    testImplementation("com.github.anuken.mindustryjitpack:server:$mindustryVersion")
    testImplementation("com.github.anuken.arc:backend-headless:$arcVersion")
    testImplementation("com.github.stefanbirkner:system-rules:1.19.0")
    testImplementation("net.datafaker:datafaker:1.8.1")

    val ktor_version = "2.2.3"
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-jackson:$ktor_version")
    implementation("io.ktor:ktor-server-rate-limit:$ktor_version")
}

tasks.jar {
    enabled = false



    if (!file("./src/main/resources/www").exists()) {
        dependsOn("web")
    }

    archiveFileName.set("Essentials.jar")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }){
        exclude("**/META-INF/*.SF")
        exclude("**/META-INF/*.DSA")
        exclude("**/META-INF/*.RSA")
    }

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    dependsOn(tasks.shadowJar)
}

tasks.register("web") {
    if (OperatingSystem.current() == OperatingSystem.forName("windows")) {
        exec {
            workingDir("./src/www")
            commandLine("npm.cmd", "i")
        }
        exec {
            workingDir("./src/www")
            commandLine("npm.cmd", "run", "build")
        }
    } else { /* if os is unix-like */
        exec {
            workingDir("./src/www")
            commandLine("npm", "i")
        }
        exec {
            workingDir("./src/www")
            commandLine("npm", "run", "build")
        }
    }
    project.delete(
        files("./src/main/resources/www")
    )
    copy {
        from("src/www/dist")
        into("src/main/resources/www")
    }
}

tasks.compileKotlin {
    kotlinOptions.allWarningsAsErrors = false
}

tasks.shadowJar {
    archiveFileName.set("Essentials.jar")

   minimize {
       exclude(dependency("org.jetbrains.exposed:.*:.*"))
       exclude(dependency("org.slf4j:slf4j-nop:.*"))
       exclude(dependency(files("libs/lingua.jar")))
   }
}

sourceSets{
    test{
        resources{
            srcDir("src/main/resources")
        }
    }
}