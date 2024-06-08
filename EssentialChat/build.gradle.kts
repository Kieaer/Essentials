plugins {
    kotlin("jvm")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

repositories {
    mavenCentral()
    maven(url = "https://raw.githubusercontent.com/Zelaux/MindustryRepo/master/repository")
    maven(url = "https://www.jitpack.io")
}

configurations.all {
    resolutionStrategy.eachDependency { ->
        if (this.requested.group == "com.github.Anuken.Arc") {
            this.useVersion(mindustryVersion)
        }
    }
}

val mindustryVersion = "v146"

dependencies {
    compileOnly("com.github.anuken.arc:arc-core:$mindustryVersion")
    compileOnly("com.github.anuken.mindustryjitpack:core:$mindustryVersion")
    compileOnly(project(":Essential"))
}

tasks.jar {
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }){
        exclude("**/META-INF/*.SF")
        exclude("**/META-INF/*.DSA")
        exclude("**/META-INF/*.RSA")
    }

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn("shadowJar")
}

tasks.shadowJar {
    minimize()
}