plugins {
    alias(libs.plugins.kotlin)
    `java-library`
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation(libs.bundles.game)
}

// This is needed to make the annotation available to other modules
java {
    withSourcesJar()
}
