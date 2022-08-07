plugins {
    id("org.jetbrains.kotlin.jvm") version "1.6.10"
    id("org.sonarqube") version "3.4.0.2513"
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
    maven (url = "https://www.jitpack.io")
}

dependencies {
    val exposedVersion: String by project
    val mindustryVersion: String by project

    compileOnly("com.github.Anuken.Arc:arc-core:$mindustryVersion")
    compileOnly("com.github.Anuken.Mindustry:core:$mindustryVersion")

    implementation("com.github.PersonTheCat:hjson-java:master")
    implementation("de.svenkubiak:jBCrypt:0.4.3")
    implementation("com.mewna:catnip:3.1.0")
    implementation("org.apache.maven:maven-artifact:3.8.5")
    implementation("org.jsoup:jsoup:1.15.2")
    implementation("com.github.gimlet2:kottpd:0.2.1")
    implementation("org.slf4j:slf4j-nop:2.0.0-alpha7")
    implementation("com.h2database:h2:2.1.214")

    implementation("org.xerial:sqlite-jdbc:3.36.0.3")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    implementation("com.neovisionaries:nv-i18n:1.29")
    implementation(files("libs/ip2location.jar"))

    testImplementation("com.github.Anuken.arc:arc-core:$mindustryVersion")
    testImplementation("com.github.Anuken.Mindustry:core:$mindustryVersion")
    testImplementation("com.github.Anuken.Mindustry:server:$mindustryVersion")
    testImplementation("com.github.Anuken.arc:backend-headless:$mindustryVersion")
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