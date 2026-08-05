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

        ivy {
            url = uri("https://github.com/")
            patternLayout {
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
