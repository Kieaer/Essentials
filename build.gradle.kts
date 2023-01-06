plugins {
    kotlin("jvm") version "1.8.0-Beta"
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
}

repositories {
    mavenCentral()
    maven(url = "https://www.jitpack.io")
}

dependencies {
    val exposedVersion = "0.41.1"
    val mindustryVersion = "v140.4"
    val arcVersion = "v140.4"

    compileOnly("com.github.anuken.arc:arc-core:$arcVersion")
    compileOnly("com.github.anuken.mindustryjitpack:core:$mindustryVersion")

    implementation("com.github.PersonTheCat:hjson-java:3.0.0-C11")
    implementation("de.svenkubiak:jBCrypt:0.4.3")
    implementation("com.mewna:catnip:3.1.0")
    //implementation("com.github.gimlet2:kottpd:0.2.1")
    implementation("org.apache.maven:maven-artifact:3.8.5")

    implementation("com.h2database:h2:2.1.214")
    implementation("org.xerial:sqlite-jdbc:3.40.0.0")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    implementation("com.neovisionaries:nv-i18n:1.29")
    implementation(files("libs/ip2location.jar"))
    implementation(files("libs/langdetect.jar"))
    implementation(files("libs/jsonic-1.2.0.jar"))

    testImplementation("com.github.anuken.arc:arc-core:$arcVersion")
    testImplementation("com.github.anuken.mindustryjitpack:core:$mindustryVersion")
    testImplementation("com.github.anuken.mindustryjitpack:server:$mindustryVersion")
    testImplementation("com.github.anuken.arc:backend-headless:$arcVersion")
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