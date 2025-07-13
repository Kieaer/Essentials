plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
    `java-library`
}

dependencies {
    ksp(project(":ksp-processor"))
    api(project(":common"))
    api(libs.jfiglet)
    api(libs.maven.check)
    api(libs.sqlite)

    testImplementation(kotlin("test"))
    if (System.getenv("LOCAL_REPO_IP") != null) {
        testImplementation(libs.bundles.local.game.test)
    } else {
        testImplementation(libs.bundles.game.test)
    }
    testImplementation(libs.bundles.kotlinxEcosystem)
    testImplementation(libs.bundles.exposed)
    testImplementation(libs.sqlite)
    testImplementation(libs.jbcrypt)
}
