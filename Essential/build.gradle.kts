plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
    `java-library`
}

dependencies {
    ksp(project(":ksp-processor"))
    implementation(project(":ksp-processor"))
    implementation(libs.bundles.kotlinxEcosystem)
    implementation(libs.bundles.exposed)
    implementation(libs.jfiglet)
    implementation(libs.maven.check)
    implementation(libs.r2dbc.h2)
    implementation(libs.kaml)
    implementation(libs.jbcrypt)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.game.test)
    testImplementation(libs.bundles.kotlinxEcosystem)
    testImplementation(libs.bundles.exposed)
    testImplementation(libs.r2dbc.h2)
    testImplementation(libs.jbcrypt)
}
