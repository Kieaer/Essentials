plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
    `java-library`
}

dependencies {
    compileOnlyApi(libs.bundles.game)

    ksp(project(":ksp-processor"))
    api(project(":ksp-processor"))
    api(libs.bundles.kotlinxEcosystem)
    api(libs.bundles.jackson)
    api(libs.bundles.exposed)
    api(libs.jfiglet)
    api(libs.maven.check)
    api(libs.sqlite)
    api(libs.hikariCP)
    api(libs.kaml)
    api(libs.jbcrypt)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.game.test)
    testImplementation(libs.bundles.kotlinxEcosystem)
    testImplementation(libs.bundles.exposed)
    testImplementation(libs.sqlite)
    testImplementation(libs.jbcrypt)
}
