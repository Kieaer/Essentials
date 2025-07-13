plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(project(":ksp-processor"))
    compileOnly(project(":Essential"))

    runtimeOnly(project(":ksp-processor"))
    runtimeOnly(libs.bundles.kotlinxEcosystem)
    runtimeOnly(libs.bundles.jackson)
    runtimeOnly(libs.bundles.exposed)
    runtimeOnly(libs.kaml)
    runtimeOnly(libs.hikariCP)
    runtimeOnly(libs.jbcrypt)
    runtimeOnly(libs.jfiglet)
    runtimeOnly(libs.sqlite)
}
