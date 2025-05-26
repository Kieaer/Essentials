plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(project(":ksp-processor"))
    implementation(project(":ksp-processor"))

    implementation(project(":common"))
    implementation(libs.jfiglet)
    implementation(libs.maven.check)

    compileOnly(libs.bundles.game)
    compileOnly(libs.bundles.kotlinxEcosystem)
    compileOnly(libs.bundles.jackson)
    compileOnly(libs.bundles.exposed)
    compileOnly(libs.kaml)
    compileOnly(libs.jbcrypt)

    testImplementation(project(":common"))
    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.game.test)
    testImplementation(libs.bundles.kotlinxEcosystem)
    testImplementation(libs.bundles.exposed)
    testImplementation(libs.sqlite)
    testImplementation(libs.jbcrypt)
}
