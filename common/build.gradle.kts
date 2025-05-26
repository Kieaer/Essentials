plugins {
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(project(":ksp-processor"))
    compileOnly(libs.bundles.game)

    implementation(project(":ksp-processor"))
    implementation(libs.bundles.kotlinxEcosystem)
    implementation(libs.bundles.jackson)
    implementation(libs.bundles.exposed)
    implementation(libs.hikariCP)
    implementation(libs.kaml)
    implementation(libs.jbcrypt)
}
