import com.github.jengelman.gradle.plugins.shadow.ShadowJavaPlugin.Companion.shadowJar

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.shadowJar) apply false
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
    apply(plugin = "com.gradleup.shadow")

    if (project.name != "ksp-processor") {
        dependencies {
            if (System.getenv("LOCAL_REPO_IP") != null) {
                compileOnly(rootProject.libs.bundles.local.game)
            } else {
                compileOnly(rootProject.libs.bundles.game)
            }
        }
    }

    tasks.shadowJar {
        exclude("arc/**")
        exclude("mindustry/**")
        exclude("server/**")

        from(project.configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) }) {
            exclude("META-INF/*.SF")
            exclude("META-INF/*.DSA")
            exclude("META-INF/*.RSA")
        }

        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(rootDir) {
            include("plugin.json")
        }
    }
}