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
    compileOnly("com.github.Anuken.Arc:arc-core:v128")
    compileOnly("com.github.Anuken.Mindustry:core:v128")

    implementation("com.github.PersonTheCat:hjson-java:master")
    implementation("de.svenkubiak:jBCrypt:+")
    implementation("com.mewna:catnip:3.1.0")
    implementation("org.apache.maven:maven-artifact:+")
    implementation("org.jsoup:jsoup:+")
    implementation("com.github.gimlet2:kottpd:+")
    implementation("org.slf4j:slf4j-nop:+")
    implementation("com.h2database:h2:1.4.200")
    implementation("com.neovisionaries:nv-i18n:+")
    implementation(files("libs/ip2location.jar"))

    testImplementation("com.github.Anuken.arc:arc-core:v128")
    testImplementation("com.github.Anuken.Mindustry:core:v128")
    testImplementation("com.github.Anuken.Mindustry:server:v128")
    testImplementation("com.github.Anuken.arc:backend-headless:v128")
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

tasks.jacocoTestReport {
    reports {
        xml.isEnabled = true
    }
    dependsOn(tasks.test) // tests are required to run before generating the report
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport) // report is always generated after tests run
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