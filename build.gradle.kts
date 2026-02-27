import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin.Companion.shadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.shadowJar) apply false
    `maven-publish`
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "com.gradleup.shadow")
    apply(plugin = "maven-publish")

    tasks.shadowJar {
        mergeServiceFiles()

        exclude("arc/**")
        exclude("mindustry/**")
        exclude("server/**")

        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        from(rootDir) {
            include("plugin.json")
        }
    }

    publishing {
        publications {
            create<MavenPublication>("maven") {
                groupId = "essentials"
                artifactId = project.name.lowercase()
                version = "1.0.0"

                from(components["java"])
            }
        }
    }
}
