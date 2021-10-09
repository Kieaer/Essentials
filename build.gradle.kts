plugins {
    id("org.jetbrains.kotlin.jvm") version "1.5.0"
    id("org.sonarqube") version "3.3"
    jacoco
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
}

repositories {
    mavenCentral()
    jcenter()
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
    implementation("org.apache.maven:maven-artifact:3.8.1")
    implementation("org.jsoup:jsoup:1.14.2")
    implementation("com.github.gimlet2:kottpd:0.2.1")
    implementation("org.slf4j:slf4j-nop:1.7.32")
    implementation("com.h2database:h2:1.4.200")

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

    finalizedBy(tasks.getByName("copy"))
}

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
    }
    dependsOn(tasks.test) // tests are required to run before generating the report
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
}

tasks.register<Copy>("copy") {
    from("$buildDir/libs/Essentials.jar")
    into("C:/Users/cloud/OneDrive/바탕 화면/server/config/mods")
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

jacoco {
    toolVersion = "0.8.7"
}