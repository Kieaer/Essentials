plugins {
    alias(libs.plugins.kotlin)
    `java-library`
}

dependencies {
    if (System.getenv("LOCAL_REPO_IP") != null) {
        implementation(libs.bundles.local.game)
    } else {
        implementation(libs.bundles.game)
    }
    compileOnly(libs.ksp.api)
    compileOnly(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
    implementation("com.squareup:javapoet:1.13.0")
}

// This is needed to make the annotation available to other modules
java {
    withSourcesJar()
}
