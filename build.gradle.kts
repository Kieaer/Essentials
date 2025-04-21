plugins {
    kotlin("jvm") version "2.1.10"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.10"
    id("com.gradleup.shadow") version "9.0.0-beta9"
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "com.gradleup.shadow")

    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://raw.githubusercontent.com/Zelaux/MindustryRepo/master/repository")
        maven(url = "https://www.jitpack.io")
    }

    val game : String by rootProject
    val kaml : String by rootProject
    val exposed : String by rootProject
    val slf4j : String by rootProject
    val hjson : String by rootProject
    val jbcrypt : String by rootProject
    val mavenArtifact : String by rootProject
    val jackson : String by rootProject
    val classgraph : String by rootProject
    val jfiglet : String by rootProject
    val logback : String by rootProject

    val rules : String by rootProject
    val dataFaker : String by rootProject
    val mockito : String by rootProject
    val jts : String by rootProject
    val h2 : String by rootProject

    configurations.all {
        resolutionStrategy.eachDependency {
            if (this.requested.group == "com.github.Anuken.Arc") {
                this.useVersion(game)
            }
        }
    }

    tasks.jar {
        from(sourceSets.main.get().output) {
            exclude("**/mindustry/**")
        }

        from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
            exclude("META-INF/*.SF")
            exclude("META-INF/*.DSA")
            exclude("META-INF/*.RSA")
        }

        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(rootDir) {
            include("plugin.json")
        }
    }

    tasks.shadowJar {
        dependsOn("jar")

        minimize {
            exclude(dependency("org.jetbrains.exposed:.*:.*"))
            exclude(dependency("org.slf4j:slf4j-api:.*"))
            exclude(dependency("org.slf4j:slf4j-nop:.*"))
        }
    }

    dependencies {
        compileOnly("com.github.Anuken.Arc:arc-core:$game")
        compileOnly("com.github.Anuken.mindustry:core:$game")
        compileOnly("com.github.Anuken.mindustry:server:$game")

        runtimeOnly("org.slf4j:slf4j-nop:$slf4j")

        if (project.name == "Essential") {
            // Main module
            implementation("com.charleskorn.kaml:kaml-jvm:$kaml")
            implementation("org.jetbrains.exposed:exposed-core:$exposed")
            implementation("org.jetbrains.exposed:exposed-jdbc:$exposed")
            implementation("org.jetbrains.exposed:exposed-java-time:$exposed")

            implementation("com.github.PersonTheCat:hjson-java:$hjson")
            implementation("de.svenkubiak:jBCrypt:$jbcrypt")
            implementation("org.apache.maven:maven-artifact:$mavenArtifact")
            implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jackson")
            implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jackson")
            implementation("io.github.classgraph:classgraph:$classgraph")
            implementation("com.github.lalyos:jfiglet:$jfiglet")
            implementation("ch.qos.logback:logback-classic:$logback")

            testImplementation("org.jetbrains.kotlin:kotlin-test")
            testImplementation("com.github.Anuken.Arc:arc-core:$game")
            testImplementation("com.github.Anuken.mindustry:server:$game")
            testImplementation("com.github.Anuken.mindustry:core:$game")
            testImplementation("com.github.Anuken.Arc:backend-headless:$game")
            testImplementation("com.github.stefanbirkner:system-rules:$rules")
            testImplementation("net.datafaker:datafaker:$dataFaker")
            testImplementation("org.mockito:mockito-core:$mockito")
            testImplementation("org.locationtech.jts:jts-core:$jts")
            testImplementation("com.h2database:h2:$h2")
        } else {
            // Sub modules
            compileOnly(project(":Essential"))
        }
    }
}