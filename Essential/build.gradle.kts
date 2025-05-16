plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(project(":ksp-processor"))
    implementation(project(":ksp-processor"))

    implementation(project(":common"))
    implementation(libs.jbcrypt)
    implementation(libs.jfiglet)
    implementation(libs.maven.check)

    compileOnly(libs.bundles.game)
    compileOnly(libs.bundles.kotlinxEcosystem)
    compileOnly(libs.bundles.jackson)
    compileOnly(libs.bundles.exposed)
    compileOnly(libs.kaml)
}
