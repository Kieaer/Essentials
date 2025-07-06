plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
    java
}

dependencies {
    ksp(project(":ksp-processor"))
    implementation(project(":common"))
}