import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin.Companion.shadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.shadowJar) apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "com.gradleup.shadow")

    tasks.shadowJar {
        from(sourceSets.main.get().output) {
            exclude("**/mindustry/**")
        }

        from(project.configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
            exclude("META-INF/*.SF")
            exclude("META-INF/*.DSA")
            exclude("META-INF/*.RSA")
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(rootDir) {
            include("plugin.json")
        }

        minimize {
            exclude(dependency("org.jetbrains.exposed:.*:.*"))
        }
    }

    dependencies {
//        compileOnly(libs.bundle.game)

        /*if (project.name == "Essential") {
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
        }*/
    }
}