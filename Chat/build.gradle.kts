plugins {
    kotlin("jvm")
}


configurations.all {
    resolutionStrategy.eachDependency { ->
        if (this.requested.group == "com.github.Anuken.Arc") {
            this.useVersion(mindustryVersion)
        }
    }
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
}

dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

dependencies {
    val rulesVersion = "1.19.0"
    val dataFakerVersion = "1.8.1"
    val mockitoVersion = "5.4.0"
    val jtsVersion = "1.19.0"

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