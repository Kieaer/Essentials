plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(project(":ksp-processor"))
    implementation(libs.discord) {
        exclude(module = "opus-java")
        exclude(module = "tink")
    }
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
