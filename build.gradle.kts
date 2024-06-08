import com.github.gradle.node.npm.task.NpmTask

plugins {
    kotlin("jvm") version "1.9.20-RC2"
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.github.node-gradle.node") version "5.0.0"
    id("maven-publish")
    id("jacoco")
    id("org.sonarqube") version "4.4.1.3373"
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
    }
}

repositories {
    mavenCentral()
    maven(url = "https://raw.githubusercontent.com/Zelaux/MindustryRepo/master/repository")
    maven(url = "https://www.jitpack.io")
}

val mindustryVersion = "v146"
val arcVersion = "v146"

dependencies {
    val hjsonVersion = "3.0.0-C11"
    val jBCryptVersion = "0.4.3"
    val mavenVersion = "4.0.0-alpha-3"
    val h2Version = "2.2.220"
    val exposedVersion = "0.46.0"
    val slf4jVersion = "2.0.6"
    val jfigletVersion = "0.0.9"
    val postgresVersion = "42.7.2"
    val mysqlConnectVersion = "8.3.0"
    val mariadbConnectVersion = "3.3.3"

    compileOnly("com.github.anuken.arc:arc-core:$arcVersion")
    compileOnly("com.github.anuken.mindustryjitpack:core:$mindustryVersion")

    implementation("com.github.PersonTheCat:hjson-java:$hjsonVersion")
    implementation("de.svenkubiak:jBCrypt:$jBCryptVersion")

    //implementation("com.github.gimlet2:kottpd:0.2.1")
    implementation("org.apache.maven:maven-artifact:$mavenVersion")

    implementation("com.h2database:h2:$h2Version")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    implementation(files("libs/lingua.jar"))
    implementation("org.slf4j:slf4j-api:2.0.13")
    implementation("org.slf4j:slf4j-nop:$slf4jVersion")
    implementation("com.github.lalyos:jfiglet:$jfigletVersion")

    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.mysql:mysql-connector-j:$mysqlConnectVersion")
    implementation("org.mariadb.jdbc:mariadb-java-client:$mariadbConnectVersion")
}

// 테스트용
dependencies {
    val rulesVersion = "1.19.0"
    val dataFakerVersion = "1.8.1"
    val mockitoVersion = "5.4.0"
    val jtsVersion = "1.19.0"

    testImplementation("com.github.anuken.arc:arc-core:$arcVersion")
    testImplementation("com.github.anuken.mindustryjitpack:core:$mindustryVersion")
    testImplementation("com.github.anuken.mindustryjitpack:server:$mindustryVersion")
    testImplementation("com.github.anuken.arc:backend-headless:$arcVersion")
    testImplementation("com.github.stefanbirkner:system-rules:$rulesVersion")
    testImplementation("net.datafaker:datafaker:$dataFakerVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.locationtech.jts:jts-core:$jtsVersion")

}

// 웹 서버용
dependencies {
    val ktor = "2.3.10"
    implementation("io.ktor:ktor-server-core-jvm:$ktor")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor")
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
    description = "jar 압축"
    group = JavaBasePlugin.BUILD_TASK_NAME
    dependsOn(tasks.jar)
    dependsOn(tasks.shadowJar)
}

node {
    download.set(true)
    nodeProjectDir.set(file("./src/www"))
}

val nodeBuild = tasks.register<NpmTask>("nodeBuild") {
    description = "node.js 빌드"
    group = JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME
    args.set(listOf("run", "build"))
}

val nodeInstall = tasks.register<NpmTask>("nodeInstall") {
    description = "node.js 복사"
    group = JavaBasePlugin.BUILD_DEPENDENTS_TASK_NAME
    args.set(listOf("install"))
}

tasks.register("web") {
    description = "웹 파일 빌드"
    group = JavaBasePlugin.BUILD_TASK_NAME
    val build = tasks["nodeBuild"]
    val install = tasks["nodeInstall"]
    if (!file("./src/www/node_modules").exists()) {
        dependsOn(install)
        dependsOn(build)
        build.mustRunAfter(install)
    } else {
        dependsOn(build)
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

tasks.jacocoTestReport {
    reports {
        xml.required.set(true)
        xml.outputLocation.set(file("build/reports/jacoco/test/jacoco.xml"))
    }
}

sonar {
    properties {
        property("sonar.projectKey", "Kieaer_Essentials")
        property("sonar.organization", "kieaer")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/reports/jacoco/test/jacoco.xml")
    }
}