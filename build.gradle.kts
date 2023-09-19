import com.github.gradle.node.npm.task.NpmTask

plugins {
    kotlin("jvm") version "1.8.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.node-gradle.node") version "5.0.0"
    id("maven-publish")
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

val exposedVersion = "0.41.1"
val mindustryVersion = "v145"
val arcVersion = "v145"

dependencies {
    compileOnly("com.github.anuken.arc:arc-core:$arcVersion")
    compileOnly("com.github.anuken.mindustryjitpack:core:$mindustryVersion")

    implementation("com.github.PersonTheCat:hjson-java:3.0.0-C11")
    implementation("de.svenkubiak:jBCrypt:0.4.3")

    //implementation("com.github.gimlet2:kottpd:0.2.1")
    implementation("org.apache.maven:maven-artifact:4.0.0-alpha-3")

    implementation("com.h2database:h2:2.2.220")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    implementation(files("libs/lingua.jar"))
    implementation("org.slf4j:slf4j-nop:2.0.6")
    implementation("com.github.lalyos:jfiglet:0.0.9")

    testImplementation("com.github.anuken.arc:arc-core:$arcVersion")
    testImplementation("com.github.anuken.mindustryjitpack:core:$mindustryVersion")
    testImplementation("com.github.anuken.mindustryjitpack:server:$mindustryVersion")
    testImplementation("com.github.anuken.arc:backend-headless:$arcVersion")
    testImplementation("com.github.stefanbirkner:system-rules:1.19.0")
    testImplementation("net.datafaker:datafaker:1.8.1")
    testImplementation("org.mockito:mockito-core:5.4.0")
    testImplementation("org.locationtech.jts:jts-core:1.19.0")

    val ktor = "2.3.1"
    implementation("io.ktor:ktor-server-core:$ktor")
    implementation("io.ktor:ktor-server-netty:$ktor")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor")
    implementation("io.ktor:ktor-serialization-jackson:$ktor")
    implementation("io.ktor:ktor-server-rate-limit:$ktor")
}

tasks.jar {
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
}

tasks.register("compression") {
    dependsOn(tasks.jar)
    dependsOn(tasks.shadowJar)
}

node {
    download.set(true)
    version.set("18.16.0")
    npmVersion.set("9.5.1")
    nodeProjectDir.set(file("./src/www"))
}

val nodeBuild = tasks.register<NpmTask>("nodeBuild") {
    args.set(listOf("run", "build"))
}

val nodeInstall = tasks.register<NpmTask>("nodeInstall") {
    args.set(listOf("install"))
}

tasks.register("web") {
    val build = tasks["nodeBuild"]
    val install = tasks["nodeInstall"]
    if (!file("./src/www/node_modules").exists()) {
        dependsOn(install)
        build.mustRunAfter(install)
    } else {
        dependsOn(install)
    }

    project.delete(
        files("./src/main/resources/www")
    )
    copy {
        from("src/www/dist")
        into("src/main/resources/www")
    }
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

configurations.all {
    resolutionStrategy.eachDependency { ->
        if (this.requested.group == "com.github.Anuken.Arc") {
            this.useVersion(mindustryVersion)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.kieaer"
            artifactId = "Essentials"
            version = "v18"
            from(components["java"])
        }
    }
}