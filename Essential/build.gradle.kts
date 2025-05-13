plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
}

dependencies {
    implementation(project(":common"))

    // KSP processor dependency
    ksp(project(":ksp-processor"))
    implementation(project(":ksp-processor"))

    compileOnly(libs.bundles.game)
    compileOnly(libs.bundles.kotlinxEcosystem)
    compileOnly(libs.bundles.jackson)
    compileOnly(libs.bundles.exposed)
    compileOnly(libs.kaml)

    implementation(libs.jbcrypt)
    implementation(libs.jfiglet)
}
