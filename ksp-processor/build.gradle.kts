plugins {
    alias(libs.plugins.kotlin)
    `java-library`
}

dependencies {
    compileOnly(libs.ksp.api)
    compileOnly(libs.kotlinpoet)
    implementation(libs.bundles.game)
    implementation(libs.kotlinpoet.ksp)
    implementation("com.squareup:javapoet:1.13.0")
}

// This is needed to make the annotation available to other modules
java {
    withSourcesJar()
}
