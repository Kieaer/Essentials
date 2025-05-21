plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(project(":ksp-processor"))
    implementation(project(":ksp-processor"))
    implementation(libs.lingua)

    compileOnly(project(":common"))
    compileOnly(project(":Essential"))
    compileOnly(libs.bundles.game)
    compileOnly(libs.bundles.kotlinxEcosystem)
    compileOnly(libs.bundles.jackson)
    compileOnly(libs.bundles.exposed)
    compileOnly(libs.kaml)
}