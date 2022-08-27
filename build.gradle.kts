plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    id("org.sonarqube") version "3.4.0.2513"
    id("de.undercouch.download") version "4.0.0"
}

java {
    sourceCompatibility = JavaVersion.VERSION_18
}

repositories {
    mavenCentral()
    maven(url = "https://www.jitpack.io")
}

dependencies {
    val exposedVersion = "0.39.2"
    val mindustryVersion = "main-SNAPSHOT"
    val arcVersion = "v137"

    compileOnly("com.github.Anuken.Arc:arc-core:$arcVersion")
    compileOnly("com.github.Anuken.mindustryjitpack:core:$mindustryVersion")

    implementation("com.github.PersonTheCat:hjson-java:3.0.0-C11")
    implementation("de.svenkubiak:jBCrypt:0.4.3")
    implementation("com.mewna:catnip:3.1.0")
    //implementation("com.github.gimlet2:kottpd:0.2.1")
    implementation("org.apache.maven:maven-artifact:3.8.5")

    implementation("org.xerial:sqlite-jdbc:3.39.2.0")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    implementation("com.neovisionaries:nv-i18n:1.29")
    implementation(files("libs/ip2location.jar"))

    testImplementation("com.github.Anuken.arc:arc-core:$arcVersion")
    testImplementation("com.github.Anuken.mindustryjitpack:core:$mindustryVersion")
    testImplementation("com.github.Anuken.mindustryjitpack:server:$mindustryVersion")
    testImplementation("com.github.Anuken.arc:backend-headless:$arcVersion")
    testImplementation("com.github.stefanbirkner:system-rules:1.19.0")
    testImplementation("com.github.javafaker:javafaker:1.0.2")
}

tasks.jar {
    archiveFileName.set("Essentials.jar")
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }){
        exclude("**/META-INF/*.SF")
        exclude("**/META-INF/*.DSA")
        exclude("**/META-INF/*.RSA")
    }
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.compileKotlin {
    kotlinOptions.allWarningsAsErrors = false
}

sourceSets{
    test{
        resources{
            srcDir("src/main/resources")
        }
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", "Kieaer_Essentials")
        property("sonar.organization", "kieaer")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sourceEncoding","utf-8")
    }
}