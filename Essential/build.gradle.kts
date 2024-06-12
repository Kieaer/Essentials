plugins {
    kotlin("jvm")
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven(url = "https://raw.githubusercontent.com/Zelaux/MindustryRepo/master/repository")
    maven(url = "https://www.jitpack.io")
}

val mindustryVersion = "v146"

dependencies {
    compileOnly("com.github.anuken.arc:arc-core:$mindustryVersion")
    compileOnly("com.github.anuken.mindustryjitpack:core:$mindustryVersion")

    implementation("com.charleskorn.kaml:kaml-jvm:0.59.0")
    implementation("org.jetbrains.exposed:exposed-core:0.51.0")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.51.0")

    runtimeOnly("org.slf4j:slf4j-nop:2.0.13")

    implementation("com.github.PersonTheCat:hjson-java:3.0.0-C11")
    implementation("de.svenkubiak:jBCrypt:0.4.3")
    implementation("org.apache.maven:maven-artifact:4.0.0-alpha-3")
    implementation("com.github.MindustryInside:CommandsLib:f6a8d78")
    implementation("io.github.classgraph:classgraph:4.8.173")
    implementation("com.github.lalyos:jfiglet:0.0.9")


    val rulesVersion = "1.19.0"
    val dataFakerVersion = "1.8.1"
    val mockitoVersion = "5.4.0"
    val jtsVersion = "1.19.0"

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("com.github.anuken.arc:arc-core:$mindustryVersion")
    testImplementation("com.github.anuken.mindustryjitpack:core:$mindustryVersion")
    testImplementation("com.github.anuken.mindustryjitpack:server:$mindustryVersion")
    testImplementation("com.github.anuken.arc:backend-headless:$mindustryVersion")
    testImplementation("com.github.stefanbirkner:system-rules:$rulesVersion")
    testImplementation("net.datafaker:datafaker:$dataFakerVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.locationtech.jts:jts-core:$jtsVersion")
}

tasks.test {
    useJUnitPlatform()
}

configurations.all {
    resolutionStrategy.eachDependency { ->
        if (this.requested.group == "com.github.Anuken.Arc") {
            this.useVersion(mindustryVersion)
        }
    }
}

sourceSets{
    test{
        resources{
            srcDir("src/main/resources")
        }
    }
}

tasks.jar {
    from(sourceSets.main.get().output)

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
    }

    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    dependsOn("shadowJar")

    manifest {
        attributes["Main-Class"] = "essentials.core.Main"
    }
}

tasks.shadowJar {
    minimize {
        exclude(dependency("org.jetbrains.exposed:.*:.*"))
        exclude(dependency("org.slf4j:slf4j-api:.*"))
        exclude(dependency("org.slf4j:slf4j-nop:.*"))
    }
}