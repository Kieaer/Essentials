plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
}

dependencies {
    ksp(project(":ksp-processor"))
    implementation(libs.discord) {
        exclude(module = "opus-java")
        exclude(module="tink")
    }

    implementation(project(":common"))
}
