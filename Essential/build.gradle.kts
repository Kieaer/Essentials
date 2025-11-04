import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin.Companion.shadowJar
import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.ksp)
    `java-library`
}

dependencies {
    compileOnly(rootProject.libs.bundles.game)
    ksp(project(":ksp-processor"))
    implementation(project(":ksp-processor"))
    implementation(libs.bundles.kotlinxEcosystem)
    implementation(libs.bundles.exposed)
    implementation(libs.bundles.r2dbc.drivers)
    implementation(libs.bundles.ktor)
    implementation(libs.jfiglet)
    implementation(libs.maven.check)
    implementation(libs.kaml)
    implementation(libs.jbcrypt)

    testImplementation(kotlin("test"))
    testImplementation(libs.bundles.game.test)
    testImplementation(libs.bundles.kotlinxEcosystem)
    testImplementation(libs.kotlinxCoroutinesTest)
    testImplementation(libs.bundles.exposed)
    testImplementation(libs.bundles.r2dbc.drivers)
    testImplementation(libs.jbcrypt)
}
