rootProject.name = "Essentials"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://jitpack.io")

        ivy {
            url = uri("https://github.com/")
            patternLayout {
                artifact("/[organisation]/[module]/releases/download/[revision]/[artifact].[ext]")
                artifact("/[organisation]/[module]/releases/download/[revision]/[classifier].[ext]")
                artifact("/[organisation]/[module]/releases/download/[revision]/server-[classifier].[ext]")
                artifact("/[organisation]/[module]/releases/download/[revision]/dependencies.jar")
            }
            metadataSources {
                artifact()
            }
            content {
                includeModule("Anuken", "Mindustry")
            }
        }
    }
}

include("Essential")
include("ksp-processor")
