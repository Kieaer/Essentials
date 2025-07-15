plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
}

dependencies {
    compileOnly(project(":Essential"))

    ksp(project(":ksp-processor"))

    implementation(project(":common"))
    implementation(project(":ksp-processor"))
    implementation(libs.bundles.kotlinxEcosystem)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.exposed)
    implementation(libs.bundles.ktor)
    implementation(libs.kaml)
    implementation(libs.hikariCP)
    implementation(libs.jbcrypt)
}
